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
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.ServiceDestroyEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.pro.licensechecker.dau.DauIntegration;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static com.vaadin.flow.server.dau.DAUUtils.DAU_MIN_ACTIVITY_IN_SECONDS;
import static com.vaadin.flow.server.dau.DAUVaadinRequestInterceptorTest.FakeAppShell.BASE_ICON_PATH;

public class DAUVaadinRequestInterceptorTest {

    public static final String APP_ID = "MY-APP";
    private MockDeploymentConfiguration configuration;
    private MockVaadinServletService vaadinService;
    private DAUVaadinRequestInterceptor interceptor;
    private String originalSubscriptionKey;

    @Before
    public void setUp() throws Exception {
        configuration = new MockDeploymentConfiguration();
        configuration.setApplicationOrSystemProperty(
                InitParameters.APPLICATION_IDENTIFIER, APP_ID);
        vaadinService = new MockVaadinServletService(configuration);
        interceptor = new DAUVaadinRequestInterceptor(configuration, null);

        originalSubscriptionKey = System.getProperty("vaadin.subscriptionKey");
        System.setProperty("vaadin.subscriptionKey", "sub-123");
    }

    @After
    public void tearDown() throws Exception {
        if (originalSubscriptionKey != null) {
            System.setProperty("vaadin.subscriptionKey",
                    originalSubscriptionKey);
        } else {
            System.clearProperty("vaadin.subscriptionKey");
        }
    }

    @Test
    public void requestStart_initRequest_dauCookieAbsent_createCookie() {
        assertCookieCreated(request -> {
            Mockito.when(request.getPathInfo()).thenReturn("");
            Mockito.when(request.getMethod()).thenReturn("GET");
            Mockito.when(request
                    .getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                    .thenReturn(HandlerHelper.RequestType.INIT.getIdentifier());
        });
    }

    @Test
    public void requestStart_uidlRequest_dauCookieAbsent_createCookie() {
        assertCookieCreated(request -> {
            Mockito.when(request.getPathInfo()).thenReturn("");
            Mockito.when(request.getMethod()).thenReturn("POST");
            Mockito.when(request
                    .getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                    .thenReturn(HandlerHelper.RequestType.UIDL.getIdentifier());
        });
    }

    @Test
    public void requestStart_indexHtmlRequest_dauCookieAbsent_createCookie() {
        assertCookieCreated(request -> {
            Mockito.when(request.getPathInfo()).thenReturn("");
            Mockito.when(request.getMethod()).thenReturn("GET");
        });
        assertCookieCreated(request -> {
            Mockito.when(request.getPathInfo()).thenReturn(null);
            Mockito.when(request.getMethod()).thenReturn("GET");
        });
        assertCookieCreated(request -> {
            Mockito.when(request.getPathInfo()).thenReturn("/");
            Mockito.when(request.getMethod()).thenReturn("GET");
        });
    }

    @Test
    public void requestStart_trackableRequest_dauCookieAbsent_pushWebsocket_doNotCreateCookie() {
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        VaadinResponse response = Mockito.mock(VaadinResponse.class);
        Mockito.when(request
                .getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                .thenReturn(HandlerHelper.RequestType.UIDL.getIdentifier());

        Mockito.when(request.getCookies()).thenReturn(new Cookie[0]);

        VaadinSession.setCurrent(new MockVaadinSession(vaadinService));
        try {
            // PUSH handler calls with null response
            interceptor.requestStart(request, null);
        } finally {
            VaadinSession.setCurrent(null);
        }
        Mockito.verify(response, Mockito.never())
                .addCookie(ArgumentMatchers.any());
    }

    @Test
    public void requestStart_notTrackableRequest_dauCookieAbsent_doNotCreateCookie() {
        assertCookieNotCreated(request -> {
            Mockito.when(request
                    .getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                    .thenReturn(HandlerHelper.RequestType.HEARTBEAT
                            .getIdentifier());
        });

        assertCookieNotCreated(request -> {
            Mockito.when(request.getPathInfo()).thenReturn("");
            Mockito.when(request.getMethod()).thenReturn("POST");
        });
        assertCookieNotCreated(request -> {
            Mockito.when(request.getPathInfo()).thenReturn(null);
            Mockito.when(request.getMethod()).thenReturn("POST");
        });
        assertCookieNotCreated(request -> {
            Mockito.when(request.getPathInfo()).thenReturn("/");
            Mockito.when(request.getMethod()).thenReturn("POST");
        });

    }

    @Test
    public void requestStart_notTrackableInternalRequests_dauCookieAbsent_doNotCreateCookie() {
        assertCookieNotCreated(request -> {
            Mockito.when(request.getPathInfo()).thenReturn("/VAADIN/something");
            Mockito.when(request.getMethod()).thenReturn("GET");
        });

        assertCookieNotCreated(request -> {
            Mockito.when(request.getPathInfo()).thenReturn("/VAADIN/dynamic");
            Mockito.when(request.getMethod()).thenReturn("POST");
        });

        assertCookieNotCreated(request -> {
            Mockito.when(request.getPathInfo()).thenReturn("/HILLA/push");
            Mockito.when(request.getMethod()).thenReturn("POST");
        });
        assertCookieNotCreated(request -> {
            Mockito.when(request.getPathInfo()).thenReturn("/HILLA/push");
            Mockito.when(request.getMethod()).thenReturn("GET");
        });
    }

    @Test
    public void requestStart_notTrackableStaticResourceRequest_dauCookieAbsent_doNotCreateCookie() {
        for (String resourcePath : HandlerHelper.getPublicResources()) {
            assertCookieNotCreated(request -> {
                Mockito.when(request.getPathInfo()).thenReturn(resourcePath);
                Mockito.when(request.getMethod()).thenReturn("GET");
            });
        }

        for (String resourcePath : HandlerHelper.getPublicResourcesRoot()) {
            assertCookieNotCreated(request -> {
                Mockito.when(request.getPathInfo()).thenReturn(resourcePath);
                Mockito.when(request.getMethod()).thenReturn("GET");
            });
        }
    }

    @PWA(iconPath = BASE_ICON_PATH, name = "A", shortName = "B")
    public static class FakeAppShell implements AppShellConfigurator {
        static final String BASE_ICON_PATH = "/my/icons/icon.png";
    }

    @Test
    public void requestStart_notTrackablePWAIconsRequests_dauCookieAbsent_doNotCreateCookie() {
        Lookup lookup = Mockito.mock(Lookup.class);
        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public VaadinContext getContext() {
                VaadinContext context = super.getContext();
                if (context.getAttribute(Lookup.class) == null) {
                    context.setAttribute(Lookup.class, lookup);
                }
                return context;
            }
        };
        AppShellRegistry.getInstance(service.getContext())
                .setShell(FakeAppShell.class);
        VaadinService.setCurrent(service);
        try {
            for (String resourcePath : HandlerHelper
                    .getIconVariants(BASE_ICON_PATH)) {
                assertCookieNotCreated(request -> {
                    Mockito.when(request.getService()).thenReturn(service);
                    Mockito.when(request.getPathInfo())
                            .thenReturn(resourcePath);
                    Mockito.when(request.getMethod()).thenReturn("GET");
                });
            }
        } finally {
            VaadinService.setCurrent(null);
        }
    }

    @Test
    public void serviceInit_shouldStartDauTracking() {
        try (MockedStatic<DauIntegration> dauIntegration = Mockito
                .mockStatic(DauIntegration.class)) {
            interceptor.serviceInit(new ServiceInitEvent(vaadinService));
            dauIntegration.verify(() -> DauIntegration.startTracking(APP_ID));
        }
    }

    @Test
    public void serviceDestroy_shouldStopDauTracking() {
        try (MockedStatic<DauIntegration> dauIntegration = Mockito
                .mockStatic(DauIntegration.class)) {
            interceptor.serviceDestroy(new ServiceDestroyEvent(vaadinService));
            dauIntegration.verify(DauIntegration::stopTracking);
        }
    }

    @Test
    public void serviceInit_shouldInstallServiceDestroyListenerToStopDauTrackingOnShutdown() {
        try (MockedStatic<DauIntegration> dauIntegration = Mockito
                .mockStatic(DauIntegration.class)) {
            interceptor.serviceInit(new ServiceInitEvent(vaadinService));
            dauIntegration.verify(() -> DauIntegration.startTracking(APP_ID));
            vaadinService.destroy();
            dauIntegration.verify(DauIntegration::stopTracking);
        }
    }

    @Test
    public void requestStart_dauCookiePresent_activeUser_trackUser() {
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        VaadinResponse response = Mockito.mock(VaadinResponse.class);
        Mockito.when(request
                .getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                .thenReturn(HandlerHelper.RequestType.INIT.getIdentifier());

        String trackingHash = "trackingHash";
        Cookie cookie = createCookie(trackingHash, true);
        Mockito.when(request.getCookies()).thenReturn(new Cookie[] { cookie });

        VaadinSession.setCurrent(new MockVaadinSession(vaadinService));

        try (MockedStatic<DauIntegration> dauIntegration = Mockito
                .mockStatic(DauIntegration.class)) {
            interceptor.requestStart(request, response);
            dauIntegration
                    .verify(() -> DauIntegration.trackUser(trackingHash, null));
        } finally {
            VaadinSession.setCurrent(null);
        }
    }

    @Test
    public void requestStart_dauCookiePresent_activeUser_identitySupplier_trackUser() {
        String userIdentity = "user";
        UserIdentitySupplier identitySupplier = userIdentityContext -> Optional
                .of(userIdentity);

        DAUCustomizer dauCustomizer = new DAUCustomizer() {
            @Override
            public UserIdentitySupplier getUserIdentitySupplier() {
                return identitySupplier;
            }
        };
        interceptor = new DAUVaadinRequestInterceptor(configuration,
                dauCustomizer);
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        VaadinResponse response = Mockito.mock(VaadinResponse.class);
        Mockito.when(request
                .getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                .thenReturn(HandlerHelper.RequestType.INIT.getIdentifier());

        String trackingHash = "trackingHash";
        Cookie cookie = createCookie(trackingHash, true);
        Mockito.when(request.getCookies()).thenReturn(new Cookie[] { cookie });

        VaadinSession.setCurrent(new MockVaadinSession(vaadinService));

        try (MockedStatic<DauIntegration> dauIntegration = Mockito
                .mockStatic(DauIntegration.class)) {
            interceptor.requestStart(request, response);
            dauIntegration.verify(
                    () -> DauIntegration.trackUser(trackingHash, userIdentity));
        } finally {
            VaadinSession.setCurrent(null);
        }
    }

    @Test
    public void requestStart_dauCookiePresent_notActiveUser_trackUser() {
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        VaadinResponse response = Mockito.mock(VaadinResponse.class);
        Mockito.when(request
                .getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                .thenReturn(HandlerHelper.RequestType.INIT.getIdentifier());

        String trackingHash = "trackingHash";
        Cookie cookie = createCookie(trackingHash, false);
        Mockito.when(request.getCookies()).thenReturn(new Cookie[] { cookie });

        VaadinSession.setCurrent(new MockVaadinSession(vaadinService));

        try (MockedStatic<DauIntegration> dauIntegration = Mockito
                .mockStatic(DauIntegration.class)) {
            interceptor.requestStart(request, response);
            dauIntegration.verify(
                    () -> DauIntegration.trackUser(trackingHash, null),
                    Mockito.never());
        } finally {
            VaadinSession.setCurrent(null);
        }
    }

    @Test
    public void requestStart_invalidCookie_doNotTrack() {
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        VaadinResponse response = Mockito.mock(VaadinResponse.class);
        Mockito.when(request
                .getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                .thenReturn(HandlerHelper.RequestType.INIT.getIdentifier());

        Cookie cookie = new Cookie(DAUUtils.DAU_COOKIE_NAME,
                "invalid-cookie-value");
        Mockito.when(request.getCookies()).thenReturn(new Cookie[] { cookie });

        VaadinSession.setCurrent(new MockVaadinSession(vaadinService));

        try (MockedStatic<DauIntegration> dauIntegration = Mockito
                .mockStatic(DauIntegration.class)) {
            interceptor.requestStart(request, response);
            dauIntegration.verify(
                    () -> DauIntegration.trackUser(ArgumentMatchers.anyString(),
                            ArgumentMatchers.any()),
                    Mockito.never());
        } finally {
            VaadinSession.setCurrent(null);
        }
    }

    @Test
    public void requestStart_dauCookiePresent_notActiveUser_enforcement_tracksUser() {
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        VaadinResponse response = Mockito.mock(VaadinResponse.class);
        Mockito.when(request
                .getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                .thenReturn(HandlerHelper.RequestType.INIT.getIdentifier());

        String trackingHash = "trackingHash";
        Instant creationTime = Instant.now()
                .minusSeconds(DAU_MIN_ACTIVITY_IN_SECONDS / 2);
        Cookie cookie = new Cookie(DAUUtils.DAU_COOKIE_NAME,
                trackingHash + "$" + creationTime.toEpochMilli());

        Mockito.when(request.getCookies()).thenReturn(new Cookie[] { cookie });

        VaadinSession.setCurrent(new MockVaadinSession(vaadinService));

        try (MockedStatic<DauIntegration> dauIntegration = Mockito
                .mockStatic(DauIntegration.class)) {
            dauIntegration.when(DauIntegration::shouldEnforce).thenReturn(true);
            interceptor.requestStart(request, response);
            dauIntegration
                    .verify(() -> DauIntegration.trackUser(trackingHash, null));
        } finally {
            VaadinSession.setCurrent(null);
        }
    }

    @Test
    public void requestStart_noDauCookie_notActiveUser_enforcement_tracksUser() {
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        VaadinResponse response = Mockito.mock(VaadinResponse.class);
        Mockito.when(request
                .getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                .thenReturn(HandlerHelper.RequestType.INIT.getIdentifier());

        String trackingHash = "trackingHash";
        VaadinSession.setCurrent(new MockVaadinSession(vaadinService));

        try (MockedStatic<DauIntegration> dauIntegration = Mockito
                .mockStatic(DauIntegration.class)) {
            dauIntegration.when(DauIntegration::shouldEnforce).thenReturn(true);
            dauIntegration.when(DauIntegration::newTrackingHash)
                    .thenReturn(trackingHash);
            interceptor.requestStart(request, response);
            dauIntegration
                    .verify(() -> DauIntegration.trackUser(trackingHash, null));
        } finally {
            VaadinSession.setCurrent(null);
        }
    }

    private static Cookie createCookie(String trackingHash, boolean active) {
        Instant creationTime = Instant.now();
        if (active) {
            creationTime = creationTime
                    .minusSeconds(DAU_MIN_ACTIVITY_IN_SECONDS * 2);
        }
        return new Cookie(DAUUtils.DAU_COOKIE_NAME,
                trackingHash + "$" + creationTime.toEpochMilli());
    }

    private void assertCookieCreated(
            Consumer<VaadinRequest> requestCustomizer) {
        assertDauCookie(requestCustomizer, true);
    }

    private void assertCookieNotCreated(
            Consumer<VaadinRequest> requestCustomizer) {
        assertDauCookie(requestCustomizer, false);
    }

    private void assertDauCookie(Consumer<VaadinRequest> requestCustomizer,
            boolean expectCookieCreated) {
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        VaadinResponse response = Mockito.mock(VaadinResponse.class);
        Mockito.when(request.getCookies()).thenReturn(new Cookie[0]);
        requestCustomizer.accept(request);

        VaadinSession.setCurrent(new MockVaadinSession(vaadinService));
        try {
            interceptor.requestStart(request, response);
        } finally {
            VaadinSession.setCurrent(null);
        }
        if (expectCookieCreated) {
            Mockito.verify(response).addCookie(
                    ArgumentMatchers.argThat(cookie -> DAUUtils.DAU_COOKIE_NAME
                            .equals(cookie.getName())));
        } else {
            Mockito.verify(response, Mockito.never())
                    .addCookie(ArgumentMatchers.any());
        }
    }

}
