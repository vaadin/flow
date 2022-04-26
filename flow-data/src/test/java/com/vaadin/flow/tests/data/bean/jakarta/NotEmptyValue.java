package com.vaadin.flow.tests.data.bean.jakarta;

import jakarta.validation.constraints.NotEmpty;

public class NotEmptyValue {

    @NotEmpty
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
