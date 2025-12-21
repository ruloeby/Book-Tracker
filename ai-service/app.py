from flask import Flask, request, jsonify
import requests
import os
from collections import Counter
import json
import time

app = Flask(__name__)

GOOGLE_BOOKS_URL = "https://www.googleapis.com/books/v1/volumes"
MYMEMORY_URL = "https://api.mymemory.translated.net/get"

# Groq API Configuration
GROQ_API_KEY = os.environ.get("GROQ_API_KEY", "your_api_key_here")
GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"


# ============================================
# AI-POWERED BOOK SUMMARY (GROQ)
# ============================================

def generate_ai_summary_groq(title, author, description):
    """Generate AI summary using Groq's FREE API"""
    try:
        if not GROQ_API_KEY:
            return create_simple_summary(description)

        desc_truncated = description[:1500] if len(description) > 1500 else description

        prompt = f"""Summarize this book in 2-3 sentences. Be concise and engaging.

Book: "{title}" by {author}

Description: {desc_truncated}

Write a brief, compelling summary that captures what the book is about without spoilers."""

        response = requests.post(
            GROQ_API_URL,
            headers={
                "Authorization": f"Bearer {GROQ_API_KEY}",
                "Content-Type": "application/json"
            },
            json={
                "model": "llama-3.3-70b-versatile",
                "messages": [
                    {
                        "role": "system",
                        "content": "You are a helpful book reviewer. Write concise, engaging summaries in 2-3 sentences. No spoilers."
                    },
                    {
                        "role": "user",
                        "content": prompt
                    }
                ],
                "temperature": 0.5,
                "max_tokens": 200
            },
            timeout=15
        )

        if response.status_code == 200:
            data = response.json()
            choices = data.get("choices", [])
            if choices and len(choices) > 0:
                summary = choices[0].get("message", {}).get("content", "")
                if summary:
                    print(f"✓ [GROQ] Summary generated successfully")
                    return summary.strip()

        print(f"✗ [GROQ] Summary API returned {response.status_code}")
        return create_simple_summary(description)

    except requests.Timeout:
        print("✗ [GROQ] Summary request timed out")
        return create_simple_summary(description)
    except Exception as e:
        print(f"✗ [GROQ] Summary error: {e}")
        return create_simple_summary(description)


def create_simple_summary(description):
    """Fallback: Create a simple summary from description"""
    if not description:
        return "Summary not available for this book."

    sentences = description.replace('! ', '!|').replace('? ', '?|').replace('. ', '.|').split('|')
    summary = ""
    for sentence in sentences[:3]:
        sentence = sentence.strip()
        if sentence and len(summary) + len(sentence) + 2 <= 300:
            summary += sentence + ". "
        else:
            break

    return summary.strip() if summary else description[:250] + "..."


@app.route("/bookSummary", methods=["POST"])
def book_summary():
    """Get AI-generated book summary using Groq"""
    data = request.get_json()
    title = data.get("title")
    author = data.get("author")

    if not title or not author:
        return jsonify({"error": "Title and author are required"}), 400

    print(f"\n📚 [SUMMARY] Request for: {title} by {author}")

    try:
        # Step 1: Get book description from Google Books
        query = f'intitle:"{title}" inauthor:"{author}"'
        params = {"q": query, "maxResults": 1}

        print(f"🔍 [GOOGLE] Fetching book info...")
        gb_response = requests.get(GOOGLE_BOOKS_URL, params=params, timeout=10)

        if gb_response.status_code == 200:
            gb_data = gb_response.json()
            items = gb_data.get("items", [])

            if items:
                volume_info = items[0].get("volumeInfo", {})
                description = volume_info.get("description", "")

                if description and len(description) > 50:
                    # Step 2: Generate AI summary with Groq
                    ai_summary = generate_ai_summary_groq(title, author, description)
                    return jsonify({
                        "title": title,
                        "author": author,
                        "summary": ai_summary
                    })
                else:
                    print("✗ [GOOGLE] No description found")
            else:
                print("✗ [GOOGLE] No book found")
        else:
            print(f"✗ [GOOGLE] API error: {gb_response.status_code}")

    except requests.Timeout:
        print("✗ [GOOGLE] Request timed out")
    except Exception as e:
        print(f"✗ [SUMMARY] Error: {e}")

    # Fallback response
    return jsonify({
        "title": title,
        "author": author,
        "summary": f"'{title}' by {author} is a book worth exploring. Check it out to discover what makes it special!"
    })


# ============================================
# AI-POWERED RECOMMENDATION SYSTEM (GROQ)
# ============================================

def analyze_books_with_llm(book_titles):
    """Use Groq API to analyze user's reading patterns"""
    try:
        print(f"\n🤖 [GROQ] Analyzing reading patterns for {len(book_titles)} books...")
        print(f"📖 [GROQ] Books to analyze: {book_titles}")

        if not GROQ_API_KEY or GROQ_API_KEY == "":
            print("✗ [GROQ] No API key found. Using fallback method.")
            return None

        books_list = "\n".join([f"- {title}" for title in book_titles[:5]])

        prompt = f"""Analyze this reading list briefly.

Books:
{books_list}

Return JSON only:
{{"genres": ["genre1", "genre2"], "reasoning": "brief explanation"}}"""

        print(f"📤 [GROQ] Sending request to Groq API...")

        response = requests.post(
            GROQ_API_URL,
            headers={
                "Authorization": f"Bearer {GROQ_API_KEY}",
                "Content-Type": "application/json"
            },
            json={
                "model": "llama-3.3-70b-versatile",
                "messages": [
                    {
                        "role": "system",
                        "content": "You are a book recommendation expert. Always respond with valid JSON only."
                    },
                    {
                        "role": "user",
                        "content": prompt
                    }
                ],
                "temperature": 0.7,
                "max_tokens": 200,
                "response_format": {"type": "json_object"}
            },
            timeout=15
        )

        if response.status_code == 200:
            data = response.json()
            choices = data.get("choices", [])

            if choices and len(choices) > 0:
                message = choices[0].get("message", {})
                text = message.get("content", "")
                print(f"✓ [GROQ] Raw response text: {text}")

                text = text.strip()
                if text.startswith("```json"):
                    text = text[7:]
                if text.startswith("```"):
                    text = text[3:]
                if text.endswith("```"):
                    text = text[:-3]
                text = text.strip()

                analysis = json.loads(text)
                print(f"✓ [GROQ] Parsed analysis: {analysis}")
                return analysis
            else:
                print(f"✗ [GROQ] No choices in response")
        else:
            print(f"✗ [GROQ] API call failed (status: {response.status_code})")
            print(f"✗ [GROQ] Response: {response.text}")

        return None

    except Exception as e:
        print(f"✗ [GROQ] Error: {e}")
        import traceback
        traceback.print_exc()
        return None


def get_llm_book_recommendations(genres, book_count=4):
    """Use Groq API to generate specific book recommendations"""
    try:
        print(f"\n📚 [GROQ] Generating {book_count} recommendations for genres: {genres}")

        if not GROQ_API_KEY or GROQ_API_KEY == "":
            print("✗ [GROQ] No API key found")
            return []

        genres_text = ", ".join(genres[:2])

        prompt = f"""Recommend {book_count} popular books for: {genres_text}
Return JSON: {{"books": [{{"title": "Book Title", "author": "Author Name", "reason": "5 words max"}}]}}"""

        print(f"📤 [GROQ] Sending book recommendation request...")

        response = requests.post(
            GROQ_API_URL,
            headers={
                "Authorization": f"Bearer {GROQ_API_KEY}",
                "Content-Type": "application/json"
            },
            json={
                "model": "llama-3.3-70b-versatile",
                "messages": [
                    {
                        "role": "system",
                        "content": "Return valid JSON only. Be concise."
                    },
                    {
                        "role": "user",
                        "content": prompt
                    }
                ],
                "temperature": 0.7,
                "max_tokens": 400,
                "response_format": {"type": "json_object"}
            },
            timeout=15
        )

        print(f"📥 [GROQ] Book recs response status: {response.status_code}")

        if response.status_code == 200:
            data = response.json()
            choices = data.get("choices", [])

            if choices and len(choices) > 0:
                message = choices[0].get("message", {})
                text = message.get("content", "")
                print(f"✓ [GROQ] Raw recommendations text: {text[:300]}...")

                text = text.strip()
                if text.startswith("```json"):
                    text = text[7:]
                if text.startswith("```"):
                    text = text[3:]
                if text.endswith("```"):
                    text = text[:-3]
                text = text.strip()

                parsed = json.loads(text)
                recommendations = []

                if isinstance(parsed, dict):
                    for key in ['books', 'recommendations', 'results', 'data']:
                        if key in parsed and isinstance(parsed[key], list):
                            recommendations = parsed[key]
                            break
                elif isinstance(parsed, list):
                    recommendations = parsed

                if recommendations:
                    print(f"✓ [GROQ] Generated {len(recommendations)} recommendations:")
                    for i, rec in enumerate(recommendations, 1):
                        print(f"   {i}. {rec.get('title', 'N/A')} by {rec.get('author', 'N/A')}")
                    return recommendations[:book_count]
                else:
                    print(f"✗ [GROQ] Could not extract recommendations from response")
        else:
            print(f"✗ [GROQ] Failed to generate recommendations (status: {response.status_code})")
            print(f"✗ [GROQ] Response: {response.text}")

        return []

    except Exception as e:
        print(f"✗ [GROQ] Error generating recommendations: {e}")
        import traceback
        traceback.print_exc()
        return []


def enrich_with_google_books_fast(llm_recommendations, exclude_titles):
    """Fast enrichment with Google Books data"""
    print(f"\n🔍 [GOOGLE] Enriching {len(llm_recommendations)} recommendations...")

    enriched = []
    exclude_lower = set(t.lower().strip() for t in exclude_titles)

    for rec in llm_recommendations[:4]:
        title = rec.get("title", "")
        author = rec.get("author", "")
        reason = rec.get("reason", "Recommended")

        if title.lower().strip() in exclude_lower:
            continue

        try:
            query = f'intitle:"{title}" inauthor:"{author}"'
            response = requests.get(
                GOOGLE_BOOKS_URL,
                params={"q": query, "maxResults": 1},
                timeout=3
            )

            if response.status_code == 200:
                items = response.json().get("items", [])
                if items:
                    vol = items[0].get("volumeInfo", {})
                    thumbnail = vol.get("imageLinks", {}).get("thumbnail", "")

                    enriched.append({
                        "title": vol.get("title", title),
                        "author": vol.get("authors", [author])[0] if vol.get("authors") else author,
                        "coverId": thumbnail,
                        "reason": reason
                    })
                    continue

            enriched.append({
                "title": title,
                "author": author,
                "coverId": None,
                "reason": reason
            })

        except requests.Timeout:
            print(f"    ⏱ Timeout for {title}, skipping")
            continue
        except Exception:
            continue

    print(f"✓ [GOOGLE] Enrichment complete: {len(enriched)} books")
    return enriched


def quick_fallback_recommendations(library_titles, limit=4):
    """Ultra-fast fallback using pre-defined popular books"""
    popular_books = [
        {
            "title": "The Midnight Library",
            "author": "Matt Haig",
            "coverId": "https://books.google.com/books/content?id=Y54CEAAAQBAJ&printsec=frontcover&img=1&zoom=1",
            "reason": "Popular Fiction"
        },
        {
            "title": "Project Hail Mary",
            "author": "Andy Weir",
            "coverId": "https://books.google.com/books/content?id=gcCOEAAAQBAJ&printsec=frontcover&img=1&zoom=1",
            "reason": "Sci-Fi"
        },
        {
            "title": "Atomic Habits",
            "author": "James Clear",
            "coverId": "https://books.google.com/books/content?id=lFhbDwAAQBAJ&printsec=frontcover&img=1&zoom=1",
            "reason": "Self-Improvement"
        },
        {
            "title": "The Silent Patient",
            "author": "Alex Michaelides",
            "coverId": "https://books.google.com/books/content?id=IjpGDwAAQBAJ&printsec=frontcover&img=1&zoom=1",
            "reason": "Thriller"
        },
    ]

    exclude_lower = set(t.lower().strip() for t in library_titles)
    filtered = [b for b in popular_books if b["title"].lower().strip() not in exclude_lower]

    return filtered[:limit]


@app.route("/api/v1/recommendations/users/<int:user_id>", methods=["POST"])
def get_user_recommendations(user_id):
    """Fast AI-powered recommendations"""
    print(f"\n⚡ [FAST] Recommendation request for user {user_id}")

    try:
        data = request.get_json()
        library_titles = data.get("library_titles", [])
        limit = request.args.get('limit', default=4, type=int)

        if not library_titles:
            return jsonify({
                "success": True,
                "recommendations": [],
                "count": 0,
                "reason": "empty_library"
            })

        print(f"📚 Library: {library_titles[:5]}...")

        # Step 1: Quick LLM analysis
        llm_analysis = analyze_books_with_llm(library_titles[:5])

        if llm_analysis and "genres" in llm_analysis:
            genres = llm_analysis.get("genres", [])[:2]

            # Step 2: Get recommendations
            llm_recs = get_llm_book_recommendations(genres, book_count=4)

            if llm_recs:
                # Step 3: Quick enrichment
                enriched = enrich_with_google_books_fast(llm_recs, library_titles)

                return jsonify({
                    "success": True,
                    "recommendations": enriched[:limit],
                    "count": len(enriched[:limit]),
                    "reason": "ai_powered"
                })

        # Fallback
        fallback_recs = quick_fallback_recommendations(library_titles, limit)

        return jsonify({
            "success": True,
            "recommendations": fallback_recs,
            "count": len(fallback_recs),
            "reason": "fallback"
        })

    except Exception as e:
        print(f"✗ Error: {e}")
        return jsonify({"success": True, "recommendations": [], "count": 0})


@app.route("/api/v1/recommendations/trending", methods=["GET"])
def get_trending_recommendations():
    """Get trending books using Google Books API"""
    try:
        limit = request.args.get('limit', default=8, type=int)

        params = {
            "q": "subject:bestseller",
            "maxResults": limit,
            "orderBy": "relevance"
        }

        response = requests.get(GOOGLE_BOOKS_URL, params=params, timeout=5)
        trending_books = []

        if response.status_code == 200:
            data = response.json()
            items = data.get("items", [])

            for item in items:
                volume_info = item.get("volumeInfo", {})
                title = volume_info.get("title", "")
                authors = volume_info.get("authors", [])
                author = authors[0] if authors else "Unknown"

                image_links = volume_info.get("imageLinks", {})
                thumbnail = image_links.get("thumbnail", "")

                trending_books.append({
                    "title": title,
                    "author": author,
                    "coverId": thumbnail,
                    "reason": "Trending"
                })

        return jsonify({
            "success": True,
            "trending": trending_books[:limit],
            "count": len(trending_books[:limit])
        })
    except Exception as e:
        return jsonify({"success": False, "trending": [], "error": str(e)}), 200


# ============================================
# TRANSLATION SERVICE (GROQ-POWERED)
# ============================================

# Language code mapping for better compatibility
LANG_MAP = {
    'ar': 'Arabic',
    'fr': 'French',
    'es': 'Spanish',
    'de': 'German',
    'it': 'Italian',
    'pt': 'Portuguese',
    'ru': 'Russian',
    'ja': 'Japanese',
    'ko': 'Korean',
    'zh': 'Chinese',
    'en': 'English'
}

def translate_with_groq(text, source_lang, target_lang):
    """Translate using Groq AI - much more reliable than MyMemory"""
    try:
        if not GROQ_API_KEY:
            print("✗ [GROQ] No API key available for translation")
            return None

        # Get full language names
        source_name = LANG_MAP.get(source_lang, source_lang)
        target_name = LANG_MAP.get(target_lang, target_lang)

        print(f"🌐 [GROQ] Translating from {source_name} to {target_name}")

        # Split into chunks if text is too long (Groq handles much larger texts than MyMemory)
        max_chunk_size = 2000
        if len(text) > max_chunk_size:
            chunks = [text[i:i+max_chunk_size] for i in range(0, len(text), max_chunk_size)]
            translated_chunks = []

            for i, chunk in enumerate(chunks):
                print(f"  📄 Translating chunk {i+1}/{len(chunks)}...")
                translated_chunk = translate_chunk_groq(chunk, source_name, target_name)
                if translated_chunk:
                    translated_chunks.append(translated_chunk)
                else:
                    return None

            return " ".join(translated_chunks)
        else:
            return translate_chunk_groq(text, source_name, target_name)

    except Exception as e:
        print(f"✗ [GROQ] Translation error: {e}")
        return None


def translate_chunk_groq(text, source_name, target_name):
    """Translate a single chunk with Groq"""
    try:
        prompt = f"Translate the following text from {source_name} to {target_name}. Only provide the translation, no explanations:\n\n{text}"

        response = requests.post(
            GROQ_API_URL,
            headers={
                "Authorization": f"Bearer {GROQ_API_KEY}",
                "Content-Type": "application/json"
            },
            json={
                "model": "llama-3.3-70b-versatile",
                "messages": [
                    {
                        "role": "system",
                        "content": f"You are a professional translator. Translate text accurately from {source_name} to {target_name}. Provide only the translation without any explanations or notes."
                    },
                    {
                        "role": "user",
                        "content": prompt
                    }
                ],
                "temperature": 0.3,
                "max_tokens": 2000
            },
            timeout=20
        )

        if response.status_code == 200:
            data = response.json()
            choices = data.get("choices", [])
            if choices and len(choices) > 0:
                translated = choices[0].get("message", {}).get("content", "")
                if translated:
                    print(f"  ✓ Translation successful")
                    return translated.strip()

        print(f"  ✗ Groq API returned {response.status_code}")
        return None

    except Exception as e:
        print(f"  ✗ Chunk translation error: {e}")
        return None


def translate_with_mymemory_fallback(text, source_lang, target_lang):
    """Fallback to MyMemory for simple translations"""
    try:
        print(f"🔄 [MYMEMORY] Fallback translation...")

        # MyMemory has a 500 character limit
        if len(text) > 450:
            # Split into smaller chunks
            chunks = [text[i:i+450] for i in range(0, len(text), 450)]
            translated_chunks = []

            for chunk in chunks:
                params = {"q": chunk, "langpair": f"{source_lang}|{target_lang}"}
                response = requests.get(MYMEMORY_URL, params=params, timeout=10)

                if response.status_code == 200:
                    result = response.json()
                    translated = result.get("responseData", {}).get("translatedText", "")
                    if translated:
                        translated_chunks.append(translated)
                    else:
                        return None
                else:
                    return None

            return " ".join(translated_chunks)
        else:
            params = {"q": text, "langpair": f"{source_lang}|{target_lang}"}
            response = requests.get(MYMEMORY_URL, params=params, timeout=10)

            if response.status_code == 200:
                result = response.json()
                return result.get("responseData", {}).get("translatedText", "")

        return None

    except Exception as e:
        print(f"✗ [MYMEMORY] Fallback error: {e}")
        return None


@app.route("/translateText", methods=["POST"])
def translate_text():
    """Translate text using Groq AI (with MyMemory fallback)"""
    data = request.get_json()
    text = data.get("text", "")
    source = data.get("source", "en")
    target = data.get("target", "ar")

    if not text:
        return jsonify({"original": "", "translated": ""}), 200

    print(f"\n🌍 [TRANSLATE] Request: {len(text)} chars, {source} → {target}")

    try:
        # Try Groq first (more reliable, higher limits)
        translated = translate_with_groq(text, source, target)

        if translated:
            print(f"✓ [TRANSLATE] Success via Groq")
            return jsonify({
                "original": text,
                "translated": translated,
                "method": "groq"
            }), 200

        # Fallback to MyMemory
        print(f"⚠ [TRANSLATE] Groq failed, trying MyMemory...")
        translated = translate_with_mymemory_fallback(text, source, target)

        if translated:
            print(f"✓ [TRANSLATE] Success via MyMemory")
            return jsonify({
                "original": text,
                "translated": translated,
                "method": "mymemory"
            }), 200

        # If both fail, return error
        print(f"✗ [TRANSLATE] All methods failed")
        return jsonify({
            "original": text,
            "translated": f"Translation unavailable. Please try again.",
            "method": "none",
            "error": "Both translation services failed"
        }), 200

    except Exception as e:
        print(f"✗ [TRANSLATE] Error: {e}")
        return jsonify({
            "original": text,
            "translated": f"Translation error: {str(e)}",
            "method": "none"
        }), 500


# ============================================
# HOME & HEALTH ROUTES
# ============================================

@app.route("/")
def home():
    return jsonify({
        "status": "running",
        "service": "AI-Powered Book Service",
        "features": ["translation", "summaries", "recommendations"],
        "version": "2.0"
    })


@app.route("/health")
def health():
    return jsonify({"status": "healthy"}), 200


if __name__ == "__main__":
    print("\n" + "="*50)
    print(" Starting AI Book Service")
    print("="*50)
    print(f"✓ GROQ_API_KEY: {'Set' if GROQ_API_KEY else 'Not set'}")
    print(f"✓ Port: 5001")
    print(f"✓ Features: Translation, Summaries, Recommendations")
    print("="*50 + "\n")

    app.run(host='0.0.0.0', port=5001, debug=False)