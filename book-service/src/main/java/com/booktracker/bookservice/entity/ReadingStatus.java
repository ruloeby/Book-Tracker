package com.booktracker.bookservice.entity;



public enum ReadingStatus {
    TO_READ("To Read"),
    READING("Currently Reading"),
    COMPLETED("Completed"),
    ON_HOLD("On Hold"),
    DROPPED("Dropped");

    private final String displayName;

    ReadingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}