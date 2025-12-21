package com.booktracker.vitrine.dto;



public class UserDto {
    private Long id;
    private String name;   // match entity field name: "name"
    private String email;  // match entity field name: "email"
    private String password;

    public UserDto() {}
    public UserDto(Long id, String name, String email, String password) {
        this.id = id; this.name = name; this.email = email; this.password = password;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
