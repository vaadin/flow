package com.vaadin.flow.server;

import javax.servlet.ServletContext;
import java.util.function.Supplier;

/**
 * {@link VaadinContext} that goes with {@link VaadinServletService}.
 * @author miki
 * @since 14.0.0
 */
public class VaadinServletContext implements VaadinContext {

    private final ServletContext context;

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

    @Override
    public <T> T getAttribute(Class<T> type, Supplier<T> defaultValueSupplier) {
        synchronized (context) {
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
        context.setAttribute(value.getClass().getName(), value);
    }


}
