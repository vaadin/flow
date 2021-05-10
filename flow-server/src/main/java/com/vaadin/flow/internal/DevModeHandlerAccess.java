package com.vaadin.flow.internal;

import java.util.Optional;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;

/**
 * Provides API to access to the {@link DevModeHandler} instance by a
 * {@link VaadinService}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
public interface DevModeHandlerAccess {

    /**
     * Returns a {@link DevModeHandler} instance for the given {@code service}.
     *
     * @param service
     *            a service
     * @return a <code>DevModeHandler</code> instance
     */
    DevModeHandler getDevModeHandler(VaadinService service);

    /**
     * Create a {@link DevModeHandler} is factory available.
     *
     * @param service
     *            a service
     * @return a <code>DevModeHandler</code> instance or null if disabled
     */
    static DevModeHandler getDevModeHandlerIfAvailable(VaadinService service) {
        VaadinContext context = service.getContext();
        return Optional.ofNullable(context)
                .map(ctx -> ctx.getAttribute(Lookup.class))
                .map(lu -> lu.lookup(DevModeHandlerAccess.class))
                .map(dmha -> dmha.getDevModeHandler(service)).orElse(null);
    }
}
