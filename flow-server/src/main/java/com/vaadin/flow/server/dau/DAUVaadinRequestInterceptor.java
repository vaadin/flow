package com.vaadin.flow.server.dau;

import jakarta.servlet.http.Cookie;
import java.util.Optional;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.ServiceDestroyEvent;
import com.vaadin.flow.server.ServiceDestroyListener;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinRequestInterceptor;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.pro.licensechecker.dau.DauIntegration;

/**
 * Request interceptor that collects daily active users and stores them in the
 * in-memory cache.
 *
 * For internal use only. May be renamed or removed in a future release.
 */
public class DAUVaadinRequestInterceptor implements VaadinRequestInterceptor,
        VaadinServiceInitListener, ServiceDestroyListener {

    private final String applicationName;
    private final UserIdentitySupplier userIdentitySupplier;
    private final DAUCustomizer dauCustomizer;

    public DAUVaadinRequestInterceptor(
            DeploymentConfiguration deploymentConfiguration,
            DAUCustomizer dauCustomizer) {
        this.applicationName = deploymentConfiguration.getApplicationName();
        this.userIdentitySupplier = dauCustomizer != null
                ? dauCustomizer.getUserIdentitySupplier()
                : null;
        this.dauCustomizer = dauCustomizer;
    }

    @Override
    public void requestStart(VaadinRequest request, VaadinResponse response) {
        if (!DAUUtils.isTrackableRequest(request)) {
            return;
        }

        // user is counted even if request handling throws an exception
        Optional<DAUUtils.DauCookie> maybePresentCookie = DAUUtils
                .getTrackingCookie(request).flatMap(DAUUtils::parserCookie);
        if (maybePresentCookie.isPresent()) {
            DAUUtils.DauCookie dauCookie = maybePresentCookie.get();
            if (dauCookie.isActive()) {

                VaadinSession vaadinSession = VaadinSession.getCurrent();
                String userIdentity = Optional.ofNullable(userIdentitySupplier)
                        .flatMap(supplier -> supplier
                                .apply(new UserIdentityContext(request,
                                        vaadinSession)))
                        .orElse(null);
                DAUUtils.trackUser(request, dauCookie.trackingHash(),
                        userIdentity);
            }

        } else if (response != null) {
            // response can be null, for example for PUSH websocket requests

            // DAU cookie is created if not present and re-created if invalid
            Cookie cookie = DAUUtils.generateNewCookie(request);
            response.addCookie(cookie);
        }
    }

    @Override
    public void handleException(VaadinRequest request, VaadinResponse response,
            VaadinSession vaadinSession, Exception t) {
        // no-op
    }

    @Override
    public void requestEnd(VaadinRequest request, VaadinResponse response,
            VaadinSession session) {
        request.removeAttribute(DAUUtils.ENFORCEMENT_EXCEPTION_KEY);
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addServiceDestroyListener(this);
        // TODO: error handling
        DauIntegration.startTracking(applicationName);
    }

    @Override
    public void serviceDestroy(ServiceDestroyEvent event) {
        // TODO: error handling
        DauIntegration.stopTracking();
    }
}
