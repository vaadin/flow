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

import java.time.Instant;
import java.util.function.Predicate;

import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.pro.licensechecker.dau.DauIntegration;
import com.vaadin.pro.licensechecker.dau.EnforcementException;

import static com.vaadin.flow.server.dau.DAUUtils.DAU_COOKIE_MAX_AGE_IN_SECONDS;
import static com.vaadin.flow.server.dau.DAUUtils.DAU_COOKIE_NAME;
import static com.vaadin.flow.server.dau.DAUUtils.ENFORCEMENT_EXCEPTION_KEY;

/**
 * A utility class for various daily active users collecting methods.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 24.5
 */
public final class FlowDauIntegration {

    private FlowDauIntegration() {
    }

    private record TrackingDetails(String trackingHash, String userIdentity) {
    }

    /**
     * Generates a new cookie for counting daily active users within 24 hour
     * time interval.
     * <p>
     * </p>
     * Cookie value is formatted as {@code  trackingHash$creationTime}, with
     * {@code creationTime} expressed as number of milliseconds from the epoch
     * of 1970-01-01T00:00:00Z. The cookie creation time is required on
     * subsequent requests to detect active users. By default, the cookie
     * expires after 24 hours.
     *
     * @param request
     *            http request from browser
     * @return http cookie to be used to count application's end-users daily
     */
    public static Cookie generateNewCookie(VaadinRequest request) {
        String cookieValue = DauIntegration.newTrackingHash() + '$'
                + Instant.now().toEpochMilli();
        Cookie cookie = new Cookie(DAU_COOKIE_NAME, cookieValue);
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setMaxAge(DAU_COOKIE_MAX_AGE_IN_SECONDS);
        cookie.setPath("/");
        return cookie;
    }

    /**
     * Tracks the current user with the given optional identity.
     * <p>
     * </p>
     * Tracking the user may raise an enforcement exception, that is stored and
     * applied later by calling
     * {@link #applyEnforcement(VaadinRequest, Predicate)} method.
     * <p>
     * </p>
     * Tracking for UIDL requests is postponed until the message is parsed, to
     * prevent UI poll events to be considered as user interaction.
     *
     * @param request
     *            the Vaadin request.
     * @param trackingHash
     *            user tracking hash, never {@literal null}.
     * @param userIdentity
     *            user identity, can be {@literal null}.
     */
    static void trackUser(VaadinRequest request, String trackingHash,
            String userIdentity) {
        if (HandlerHelper.isRequestType(request,
                HandlerHelper.RequestType.UIDL)) {
            // postpone tracking for UIDL requests to ServerRpcHandler to
            // prevent counting and blocking poll requests, that are not
            // consider active interaction with the application
            request.setAttribute(TrackingDetails.class.getName(),
                    new TrackingDetails(trackingHash, userIdentity));
        } else {
            try {
                DauIntegration.trackUser(trackingHash, userIdentity);
            } catch (EnforcementException ex) {
                // request will be blocked in ServerRpcHandler to prevent
                // blocking poll requests, that are not consider active
                // interaction with the application
                request.setAttribute(ENFORCEMENT_EXCEPTION_KEY, ex);
            }
        }
    }

    /**
     * Tells whether new user, i.e. who just opens the browser, not yet tracked,
     * not yet counted, should be blocked immediately.
     *
     * @return {@literal true} if the current request/user should be blocked,
     *         {@literal false} otherwise.
     */
    static boolean shouldEnforce() {
        return DauIntegration.shouldEnforce();
    }

    /**
     * Potentially applies enforcement to the current request if DAU limit is
     * exceeded.
     * <p>
     * </p>
     * If enforcement has to be applied an {@link EnforcementException} is
     * thrown.
     *
     * @param request
     *            the Vaadin request
     * @param enforceableRequest
     *            predicate to check if the request can be blocked or not.
     * @throws DauEnforcementException
     *             if request must be blocked because of DAU limit exceeded.
     */
    public static void applyEnforcement(VaadinRequest request,
            Predicate<VaadinRequest> enforceableRequest) {
        TrackingDetails trackingDetails = (TrackingDetails) request
                .getAttribute(TrackingDetails.class.getName());
        EnforcementException enforcementException = (EnforcementException) request
                .getAttribute(ENFORCEMENT_EXCEPTION_KEY);
        try {
            if ((enforcementException != null || trackingDetails != null)
                    && enforceableRequest.test(request)) {
                if (trackingDetails != null) {
                    try {
                        DauIntegration.trackUser(trackingDetails.trackingHash(),
                                trackingDetails.userIdentity());
                    } catch (EnforcementException ex) {
                        enforcementException = ex;
                    }
                }
                if (enforcementException != null) {
                    throw new DauEnforcementException(enforcementException);
                }
            }
        } finally {
            request.removeAttribute(ENFORCEMENT_EXCEPTION_KEY);
            request.removeAttribute(TrackingDetails.class.getName());
        }
    }
}
