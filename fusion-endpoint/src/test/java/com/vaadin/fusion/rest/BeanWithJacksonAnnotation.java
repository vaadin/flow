/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BeanWithJacksonAnnotation {
    @JsonProperty("bookId")
    private String id;
    private String name;

    @JsonProperty("name")
    public void setFirstName(String name) {
        this.name = name;
    }

    @JsonProperty("name")
    public String getFirstName() {
        return name;
    }

    @JsonProperty
    public int getRating() {
        return 2;
    }
}
