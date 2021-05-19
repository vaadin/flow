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
     *            a Vaadin service
     * @return a {@link DevModeHandler} instance
     */
    DevModeHandler getDevModeHandler(VaadinService service);

    /**
     * Create a {@link DevModeHandler} if factory available.
     *
     * @param service
     *            a Vaadin service
     * @return an {@link Optional} containing a {@link DevModeHandler} instance
     *         or <code>EMPTY</code> if disabled
     */
    static Optional<DevModeHandler> getDevModeHandlerFromService(
            VaadinService service) {
        VaadinContext context = service.getContext();
        return Optional.ofNullable(context)
                .map(ctx -> ctx.getAttribute(Lookup.class))
                .map(lu -> lu.lookup(DevModeHandlerAccess.class))
                .flatMap(dmha -> Optional
                        .ofNullable(dmha.getDevModeHandler(service)));
    }
}
