package com.vaadin.flow.connect.backend.entity;

public class Role {
    public static final String USER = "user";
    // This role implicitly allows access to all views.
    public static final String ADMIN = "admin";

    private Role() {
        // Static methods and fields only
    }

    public static String[] getAllRoles() {
        return new String[] { USER, ADMIN };
    }

}
