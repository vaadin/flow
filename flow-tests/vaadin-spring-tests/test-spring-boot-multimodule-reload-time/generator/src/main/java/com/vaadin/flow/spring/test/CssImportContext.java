/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

public class CssImportContext {

    private String targetStyleName;

    private String value;

    public String getTargetStyleName() {
        return targetStyleName;
    }

    public void setTargetStyleName(String targetStyleName) {
        this.targetStyleName = targetStyleName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
