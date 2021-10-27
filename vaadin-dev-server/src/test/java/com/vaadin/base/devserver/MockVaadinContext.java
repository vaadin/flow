package com.vaadin.base.devserver;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.vaadin.flow.server.VaadinContext;

public class MockVaadinContext implements VaadinContext {

    Map<String, Object> attributes = new HashMap<>();

    @Override
    public <T> T getAttribute(Class<T> type, Supplier<T> defaultValueSupplier) {
        Object result = attributes.get(type.getName());
        if (result == null && defaultValueSupplier != null) {
            result = defaultValueSupplier.get();
            attributes.put(type.getName(), result);
        }
        return type.cast(result);
    }

    @Override
    public <T> void setAttribute(Class<T> clazz, T value) {
        attributes.put(clazz.getName(), value);
    }

    @Override
    public void removeAttribute(Class<?> clazz) {
        attributes.remove(clazz.getName());
    }

    @Override
    public Enumeration<String> getContextParameterNames() {
        return Collections.enumeration(Collections.emptyList());
    }

    @Override
    public String getContextParameter(String name) {
        return null;
    }
};
