package com.booktracker.vitrine.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class OpenLibraryService {

    private final String BASE_URL = "https://openlibrary.org/search.json";
    private final RestTemplate restTemplate;

    private final ConcurrentHashMap<String, CachedResult> cache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = TimeUnit.HOURS.toMillis(2);

    public OpenLibraryService() {
        this.restTemplate = new RestTemplate();
    }

    private static class CachedResult {
        List<Map<String, Object>> books;
        long timestamp;

        CachedResult(List<Map<String, Object>> books) {
            this.books = books;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS;
        }
    }

    private static final List<String> EXCLUDED_TERMS = List.of(
            "romance", "erotic", "explicit", "erotica", "sensual",
            "passionate", "steamy", "harlequin"
    );

    public List<Map<String, Object>> searchBooks(String query, int page, int pageSize) {
        String cacheKey = query.toLowerCase().trim() + "_p" + page + "_s" + pageSize;
        CachedResult cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.books;
        }

        try {
            // For "All Genres", we need different offset calculation
            // Multiply by page to get truly different results each page
            int offset = (page - 1) * pageSize * 2; // Larger offset jumps for variety

            // Fetch 3x to have good buffer after filtering
            int fetchLimit = pageSize * 3;

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                    .queryParam("q", query)
                    .queryParam("offset", offset)
                    .queryParam("limit", fetchLimit)
                    .queryParam("fields", "key,title,author_name,cover_i,first_publish_year,number_of_pages_median,subject");

            ResponseEntity<Map> response = restTemplate.getForEntity(builder.toUriString(), Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("docs")) {
                List<Map<String, Object>> docs = (List<Map<String, Object>>) responseBody.get("docs");

                List<Map<String, Object>> filteredDocs = new ArrayList<>();
                for (Map<String, Object> doc : docs) {
                    if (doc.containsKey("title") && !containsExcludedContent(doc)) {
                        filteredDocs.add(doc);
                    }
                    if (filteredDocs.size() >= pageSize) break;
                }

                cache.put(cacheKey, new CachedResult(filteredDocs));
                return filteredDocs;
            }
            return List.of();
        } catch (Exception e) {
            System.err.println("Error fetching books from OpenLibrary: " + e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> searchBooks(String query) {
        return searchBooks(query, 1, 24);
    }

    private boolean containsExcludedContent(Map<String, Object> book) {
        String title = book.getOrDefault("title", "").toString().toLowerCase();

        for (String excluded : EXCLUDED_TERMS) {
            if (title.contains(excluded)) return true;
        }

        if (book.containsKey("subject")) {
            Object subjectsObj = book.get("subject");
            if (subjectsObj instanceof List) {
                List<String> subjects = (List<String>) subjectsObj;
                int checkLimit = Math.min(5, subjects.size());
                for (int i = 0; i < checkLimit; i++) {
                    String subjectLower = subjects.get(i).toLowerCase();
                    for (String excluded : EXCLUDED_TERMS) {
                        if (subjectLower.contains(excluded)) return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Get default books with better variety for pagination
     * Uses multiple popular search terms to ensure many pages of results
     */
    public List<Map<String, Object>> getSafeBooks(int page, int pageSize) {
        // Rotate through different queries based on page to get variety
        String[] defaultQueries = {
                "popular fiction bestseller",
                "classic literature novel",
                "fantasy adventure epic",
                "science fiction space",
                "mystery detective thriller",
                "historical fiction war",
                "young adult teen",
                "children books story",
                "biography memoir history",
                "science nature discovery"
        };

        // Use modulo to cycle through queries, but also include page offset
        int queryIndex = (page - 1) % defaultQueries.length;
        String query = defaultQueries[queryIndex];

        // Calculate sub-page within each query category
        int subPage = ((page - 1) / defaultQueries.length) + 1;

        return searchBooks(query, subPage, pageSize);
    }

    public List<Map<String, Object>> getSafeBooks() {
        return getSafeBooks(1, 24);
    }

    public Map<String, Object> getBookDetails(String key) {
        try {
            String url = "https://openlibrary.org" + key + ".json";
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            System.err.println("Error fetching book details: " + e.getMessage());
            return null;
        }
    }
}