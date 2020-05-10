package com.example.googlemapapi.model;

public class User {
    private String email, fName;

    public User(String email, String fName) {
        this.email = email;
        this.fName = fName;
    }

    public User() {

    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getfName() {
        return fName;
    }

    public void setfName(String fName) {
        this.fName = fName;
    }
}
