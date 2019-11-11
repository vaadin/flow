package com.vaadin.flow.connect.backend.security;

import com.vaadin.flow.connect.backend.entity.User;

@FunctionalInterface
public interface CurrentUser {

    User getUser();
}
