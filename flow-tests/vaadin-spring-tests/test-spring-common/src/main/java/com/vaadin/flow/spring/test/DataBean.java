/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.vaadin.flow.spring.annotation.VaadinSessionScope;

/**
 * @author Vaadin Ltd
 *
 */
@Component
@VaadinSessionScope
public class DataBean {

    private final String uid = UUID.randomUUID().toString();

    public String getMessage() {
        return "foo" + uid;
    }
}
