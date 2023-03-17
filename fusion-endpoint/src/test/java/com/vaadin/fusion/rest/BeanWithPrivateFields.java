/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.rest;

public class BeanWithPrivateFields {
    @SuppressWarnings("unused")
    private String codeNumber = "007";
    private String name = "Bond";
    private String firstName = "James";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    protected String getFirstName() {
        return firstName;
    }

    protected void setFirstName(String firstName) {
        this.firstName = firstName;
    }
}
