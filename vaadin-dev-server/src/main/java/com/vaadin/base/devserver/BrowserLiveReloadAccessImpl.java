package com.vaadin.base.devserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccess;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;

public class BrowserLiveReloadAccessImpl implements BrowserLiveReloadAccess {

    @Override
    public BrowserLiveReload getLiveReload(VaadinContext context) {
        BrowserLiveReload liveReload = context
                .getAttribute(BrowserLiveReload.class);
        if (liveReload == null) {
            liveReload = new BrowserLiveReloadImpl();
            context.setAttribute(BrowserLiveReload.class, liveReload);
        }
        return liveReload;
    }

    /**
     * Returns a {@link BrowserLiveReload} instance for the given
     * {@code service}. The instance is stored in the Vaadin context by the
     * {@code vaadin-dev-server} dependency.
     * <p>
     * Returns {@code null} if production mode is enabled for the service.
     *
     * @param service
     *            a Vaadin service
     * @return a {@link BrowserLiveReload} instance or <code>null</code> in
     *         production mode
     */
    @Override
    public BrowserLiveReload getLiveReload(VaadinService service) {
        if (service.getDeploymentConfiguration().isProductionMode()) {
            getLogger().debug(
                    "BrowserLiveReloadAccessImpl::getLiveReload is called in production mode.");
            return null;
        }
        if (!service.getDeploymentConfiguration()
                .isDevModeLiveReloadEnabled()) {
            getLogger().debug(
                    "BrowserLiveReloadAccessImpl::getLiveReload is called when live reload is disabled.");
            return null;
        }
        return getLiveReload(service.getContext());
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(BrowserLiveReloadAccess.class);
    }
}
