package com.vaadin.base.devserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.internal.DevModeHandlerAccess;
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
 */
public class DevModeHandlerAccessImpl implements DevModeHandlerAccess {
    /**
     * Returns a {@link DevModeHandler} instance for the given {@code service}.
     * The instance is stored in the Vaadin context by the
     * {@code vaadin-dev-server} dependency.
     * <p>
     * Returns {@code null} if production mode is enabled for the service, or in
     * development mode but the {@code vaadin-dev-server} is not on the
     * classpath.
     *
     * @param service
     *            a service
     * @return a WebpackDevServer instance or null for production or disabled
     */
    @Override
    public DevModeHandler getDevModeHandler(VaadinService service) {
        if (service.getDeploymentConfiguration().isProductionMode()) {
            getLogger().debug(
                    "DevModeHandlerAccessImpl::getDevModeHandler is called in production mode.");
            return null;
        }
        if (!service.getDeploymentConfiguration().enableDevServer()) {
            getLogger().debug(
                    "DevModeHandlerAccessImpl::getDevModeHandler is called when dev server is disabled.");
            return null;
        }
        return DevModeHandlerImpl.getDevModeHandler();
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(DevModeHandler.class);
    }
}
