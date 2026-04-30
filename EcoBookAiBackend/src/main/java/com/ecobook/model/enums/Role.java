package com.ecobook.model.enums;

/**
 * User role for access control
 */
public enum Role {
    USER("Regular user"),
    ADMIN("Administrator");

    private final String description;

    Role(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
