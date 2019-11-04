/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.server;

import javax.servlet.ServletContext;
import java.util.Enumeration;
import java.util.function.Supplier;

/**
 * {@link VaadinContext} that goes with {@link VaadinServletService}.
 *
 * @since 2.0.0
 */
public class VaadinServletContext implements VaadinContext {

    private transient ServletContext context;

    /**
     * Creates an instance of this context with given {@link ServletContext}.
     * @param context Context.
     */
    public VaadinServletContext(ServletContext context) {
        this.context = context;
    }

    /**
     * Returns the underlying context.
     * @return A non-null {@link ServletContext}.
     */
    public ServletContext getContext() {
        return context;
    }

    /**
     * Ensures there is a valid instance of {@link ServletContext}.
     */
    private void ensureServletContext() {
        if(context == null && VaadinService.getCurrent() instanceof VaadinServletService) {
            context = ((VaadinServletService)VaadinService.getCurrent()).getServlet().getServletContext();
        } else if(context == null) {
            throw new IllegalStateException("The underlying ServletContext of VaadinServletContext is null and there is no VaadinServletService to obtain it from.");
        }
    }

    @Override
    public <T> T getAttribute(Class<T> type, Supplier<T> defaultValueSupplier) {
        ensureServletContext();
        synchronized (this) {
            Object result = context.getAttribute(type.getName());
            if (result == null && defaultValueSupplier != null) {
                result = defaultValueSupplier.get();
                context.setAttribute(type.getName(), result);
            }
            return type.cast(result);
        }
    }

    @Override
    public <T> void setAttribute(T value) {
        assert value != null;
        ensureServletContext();
        context.setAttribute(value.getClass().getName(), value);
    }

    @Override
    public void removeAttribute(Class<?> clazz) {
        ensureServletContext();
        context.removeAttribute(clazz.getName());
    }

    @Override
    public Enumeration<String> getContextParameterNames() {
        ensureServletContext();
        return context.getInitParameterNames();
    }

    @Override
    public String getContextParameter(String name) {
        ensureServletContext();
        return context.getInitParameter(name);
    }

}
