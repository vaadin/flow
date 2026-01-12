/*
 * Copyright 2000-2025 Vaadin Ltd.
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

    public DAUVaadinRequestInterceptor(
            DeploymentConfiguration deploymentConfiguration,
            DAUCustomizer dauCustomizer) {
        this.applicationName = deploymentConfiguration.getApplicationName();
        this.userIdentitySupplier = dauCustomizer != null
                ? dauCustomizer.getUserIdentitySupplier()
                : null;
    }

    @Override
    public void requestStart(VaadinRequest request, VaadinResponse response) {
        if (!DAUUtils.isTrackableRequest(request)) {
            return;
        }

        // user is counted even if request handling throws an exception
        Optional<DAUUtils.DauCookie> maybePresentCookie = DAUUtils
                .getTrackingCookie(request).flatMap(DAUUtils::parseCookie);
        if (maybePresentCookie.isPresent()) {
            DAUUtils.DauCookie dauCookie = maybePresentCookie.get();
            // ignore user's activity threshold if enforcement
            if (dauCookie.isActive() || FlowDauIntegration.shouldEnforce()) {
                trackUser(request, dauCookie.trackingHash());
            }
        } else if (response != null) {
            // response can be null, for example for PUSH websocket requests

            // DAU cookie is created if not present and re-created if invalid
            Cookie cookie = FlowDauIntegration.generateNewCookie(request);
            response.addCookie(cookie);

            // Enforce new users immediately, even if they are not yet active
            // and tracked
            if (FlowDauIntegration.shouldEnforce()) {
                trackUser(request, DAUUtils.parseCookie(cookie).orElseThrow()
                        .trackingHash());
            }
        }
    }

    private void trackUser(VaadinRequest request, String trackingHash) {
        VaadinSession vaadinSession = VaadinSession.getCurrent();
        String userIdentity = Optional.ofNullable(userIdentitySupplier)
                .flatMap(supplier -> supplier
                        .apply(new UserIdentityContext(request, vaadinSession)))
                .orElse(null);
        FlowDauIntegration.trackUser(request, trackingHash, userIdentity);
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
        DauIntegration.startTracking(applicationName);
    }

    @Override
    public void serviceDestroy(ServiceDestroyEvent event) {
        DauIntegration.stopTracking();
    }
}
