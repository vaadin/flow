/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.typeconversion;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class FusionTestBean {
    public String name;
    public String address;
    public int age;
    public boolean isAdmin;
    public FusionTypeConversionEndpoints.TestEnum testEnum;
    public Collection<String> roles;
    private String customProperty;

    @JsonGetter("customProperty")
    public String getCustomProperty() {
        return customProperty;
    }

    @JsonSetter("customProperty")
    public void setCustomProperty(String customProperty) {
        this.customProperty = customProperty;
    }
}
