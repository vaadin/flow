package com.vaadin.viteapp;

import java.util.Optional;

public final class UserService {

    private static final UserService INSTANCE = new UserService();

    private UserService() {
    }

    public static UserService getInstance() {
        return INSTANCE;
    }

    public String getName(Object authToken) {
        return "Joe";
    }

    public Optional<Object> authenticate(String user, String passwd) {
        if ("admin".equals(user) && "admin".equals(passwd)) {
            return Optional.of(new Object());
        } else {
            return Optional.empty();
        }
    }
}
