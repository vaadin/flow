/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import javax.servlet.ServletContext;

import java.util.Enumeration;
import java.util.function.Supplier;

import com.vaadin.flow.di.Lookup;

/**
 * {@link VaadinContext} that goes with {@link VaadinServletService}.
 *
 * @since 2.0.0
 */
public class VaadinServletContext implements VaadinContext {

    private transient ServletContext context;

    /**
     * Creates an instance of this context with given {@link ServletContext}.
     *
     * @param context
     *            the servlet context to use
     */
    public VaadinServletContext(ServletContext context) {
        this.context = context;
    }

    /**
     * Returns the underlying context.
     *
     * @return A non-null {@link ServletContext}.
     */
    public ServletContext getContext() {
        return context;
    }

    /**
     * Ensures there is a valid instance of {@link ServletContext}.
     */
    private void ensureServletContext() {
        if (getContext() == null
                && VaadinService.getCurrent() instanceof VaadinServletService) {
            context = ((VaadinServletService) VaadinService.getCurrent())
                    .getServlet().getServletContext();
        } else if (getContext() == null) {
            throw new IllegalStateException(
                    "The underlying ServletContext of VaadinServletContext is null and there is no VaadinServletService to obtain it from.");
        }
    }

    @Override
    public <T> T getAttribute(Class<T> type, Supplier<T> defaultValueSupplier) {
        ensureServletContext();
        synchronized (getContext()) {
            Object result = getContext().getAttribute(type.getName());
            if (result == null && defaultValueSupplier != null) {
                result = defaultValueSupplier.get();
                getContext().setAttribute(type.getName(), result);
            }
            return type.cast(result);
        }
    }

    @Override
    public <T> void setAttribute(Class<T> clazz, T value) {
        if (value == null) {
            removeAttribute(clazz);
        } else {
            synchronized (getContext()) {
                checkLookupDuplicate(clazz);
                getContext().setAttribute(clazz.getName(), value);
            }
        }
    }

    @Override
    public void removeAttribute(Class<?> clazz) {
        synchronized (getContext()) {
            checkLookupDuplicate(clazz);
            getContext().removeAttribute(clazz.getName());
        }
    }

    @Override
    public Enumeration<String> getContextParameterNames() {
        ensureServletContext();
        return getContext().getInitParameterNames();
    }

    @Override
    public String getContextParameter(String name) {
        ensureServletContext();
        return getContext().getInitParameter(name);
    }

    private Object doGetAttribute(Class<?> clazz) {
        ensureServletContext();
        return getContext().getAttribute(clazz.getName());
    }

    private void checkLookupDuplicate(Class<?> type) {
        Object attribute = doGetAttribute(type);
        if (attribute != null && Lookup.class.equals(type)) {
            throw new IllegalArgumentException("The attribute " + Lookup.class
                    + " has been already set once. It's not possible to everride its value");
        }
    }

}
