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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeType;
import tools.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.SystemMessagesInfo;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceDauTest;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.pro.licensechecker.dau.DauIntegration;
import com.vaadin.pro.licensechecker.dau.EnforcementException;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DAUUtilsTest {

    private String subscriptionKey;

    @Before
    public void setUp() throws Exception {
        subscriptionKey = System.getProperty("vaadin.subscriptionKey");
        System.setProperty("vaadin.subscriptionKey", "sub-1234");
    }

    @After
    public void tearDown() throws Exception {
        if (subscriptionKey != null) {
            System.setProperty("vaadin.subscriptionKey", subscriptionKey);
        } else {
            System.clearProperty("vaadin.subscriptionKey");
        }
    }

    @Test
    public void trackUser_uidlRequest_deferTracking() {
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        Mockito.when(request
                .getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                .thenReturn(HandlerHelper.RequestType.UIDL.getIdentifier());
        Map<String, Object> attributes = new HashMap<>();
        Mockito.doAnswer(
                i -> attributes.put(i.getArgument(0), i.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        Mockito.when(request.getAttribute(anyString()))
                .thenAnswer(i -> attributes.get(i.<String> getArgument(0)));

        try (MockedStatic<DauIntegration> dauIntegration = Mockito
                .mockStatic(DauIntegration.class)) {
            dauIntegration
                    .when(() -> DauIntegration.trackUser("trackingHash", null))
                    .thenThrow(new EnforcementException("STOP"));

            FlowDauIntegration.trackUser(request, "trackingHash", null);
            dauIntegration.verifyNoInteractions();

            Assert.assertThrows(DauEnforcementException.class,
                    () -> FlowDauIntegration.applyEnforcement(request,
                            unused -> true));
            dauIntegration.verify(
                    () -> DauIntegration.trackUser("trackingHash", null));
        } finally {
            VaadinSession.setCurrent(null);
        }
    }

    @Test
    public void trackUser_notUidlRequest_track() {
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        Mockito.when(request
                .getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                .thenReturn(HandlerHelper.RequestType.INIT.getIdentifier());
        Map<String, Object> attributes = new HashMap<>();
        Mockito.doAnswer(
                i -> attributes.put(i.getArgument(0), i.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        Mockito.when(request.getAttribute(anyString()))
                .thenAnswer(i -> attributes.get(i.<String> getArgument(0)));

        try (MockedStatic<DauIntegration> dauIntegration = Mockito
                .mockStatic(DauIntegration.class)) {
            dauIntegration
                    .when(() -> DauIntegration.trackUser("trackingHash", null))
                    .thenThrow(new EnforcementException("STOP"));

            FlowDauIntegration.trackUser(request, "trackingHash", null);
            dauIntegration.verify(
                    () -> DauIntegration.trackUser("trackingHash", null));

            Assert.assertThrows(DauEnforcementException.class,
                    () -> FlowDauIntegration.applyEnforcement(request,
                            unused -> true));

        } finally {
            VaadinSession.setCurrent(null);
        }
    }

    @Test
    public void jsonEnforcementResponse_noDauCustomizer_defaultMessages() {
        try (MockedStatic<DauIntegration> dauIntegrationMock = Mockito
                .mockStatic(DauIntegration.class)) {
            VaadinService service = VaadinServiceDauTest
                    .vaadinServiceWithDau(null);
            VaadinRequest request = Mockito.mock(VaadinRequest.class);
            Mockito.when(request.getService()).thenReturn(service);
            String response = DAUUtils.jsonEnforcementResponse(request,
                    new DauEnforcementException(
                            new EnforcementException("STOP")));

            // remove JSON wrap
            response = response.replace("for(;;);[", "").replaceFirst("]$", "");
            JsonNode json = JacksonUtils.readTree(response).get("meta")
                    .get("appError");

            EnforcementNotificationMessages expectedMessages = EnforcementNotificationMessages.DEFAULT;
            assertJsonErrorProperty("caption", expectedMessages.caption(),
                    json);
            assertJsonErrorProperty("message", expectedMessages.message(),
                    json);
            assertJsonErrorProperty("details", expectedMessages.details(),
                    json);
            assertJsonErrorProperty("url", expectedMessages.url(), json);
        }
    }

    @Test
    public void jsonEnforcementResponse_customMessages() {
        try (MockedStatic<DauIntegration> dauIntegrationMock = Mockito
                .mockStatic(DauIntegration.class)) {
            EnforcementNotificationMessages expectedMessages = new EnforcementNotificationMessages(
                    "caption", "message", "details", "url");
            DAUCustomizer customizer = new DAUCustomizer() {
                @Override
                public EnforcementNotificationMessages getEnforcementNotificationMessages(
                        SystemMessagesInfo systemMessagesInfo) {
                    return expectedMessages;
                }
            };
            VaadinService service = VaadinServiceDauTest
                    .vaadinServiceWithDau(customizer);
            VaadinRequest request = Mockito.mock(VaadinRequest.class);
            Mockito.when(request.getService()).thenReturn(service);

            String response = DAUUtils.jsonEnforcementResponse(request,
                    new DauEnforcementException(
                            new EnforcementException("STOP")));
            response = response.replace("for(;;);[", "").replaceFirst("]$", "");
            JsonNode json = JacksonUtils.readTree(response).get("meta")
                    .get("appError");

            assertJsonErrorProperty("caption", expectedMessages.caption(),
                    json);
            assertJsonErrorProperty("message", expectedMessages.message(),
                    json);
            assertJsonErrorProperty("details", expectedMessages.details(),
                    json);
            assertJsonErrorProperty("url", expectedMessages.url(), json);
        }
    }

    @Test
    public void trackDAU_trackingIntegratedWithRequest_noEnforcement() {
        MocksForTrackDAU mocks = new MocksForTrackDAU();
        VaadinService service = mocks.service;
        HttpServletRequest request = mocks.request;
        HttpServletResponse response = mocks.response;

        try (MockedStatic<FlowDauIntegration> flowDauIntegration = Mockito
                .mockStatic(FlowDauIntegration.class)) {

            DAUUtils.EnforcementResult result = DAUUtils.trackDAU(service,
                    request, response);

            mocks.assertNoEnforcement(result);
            flowDauIntegration.verify(() -> FlowDauIntegration
                    .applyEnforcement(any(VaadinRequest.class), any()));

            result.endRequestAction().run();

            verify(service).requestEnd(any(VaadinRequest.class),
                    Mockito.isNull(), Mockito.isNull());
        }
    }

    @Test
    public void trackDAU_trackingIntegratedWithRequest_enforcement() {
        MocksForTrackDAU mocks = new MocksForTrackDAU();
        VaadinService service = mocks.service;
        HttpServletRequest request = mocks.request;
        HttpServletResponse response = mocks.response;

        try (MockedStatic<FlowDauIntegration> flowDauIntegration = Mockito
                .mockStatic(FlowDauIntegration.class)) {
            flowDauIntegration
                    .when(() -> FlowDauIntegration
                            .applyEnforcement(any(VaadinRequest.class), any()))
                    .thenThrow(new DauEnforcementException(
                            new EnforcementException("STOP")));

            DAUUtils.EnforcementResult result = DAUUtils.trackDAU(service,
                    request, response);

            mocks.assertEnforcement(result);
            flowDauIntegration.verify(() -> FlowDauIntegration
                    .applyEnforcement(any(VaadinRequest.class), any()));

            result.endRequestAction().run();

            verify(service).requestEnd(any(VaadinRequest.class),
                    Mockito.isNull(), Mockito.isNull());
        }
    }

    private void assertJsonErrorProperty(String expectedKey,
            String expectedValue, JsonNode json) {
        if (expectedValue != null) {
            Assert.assertEquals(expectedKey, expectedValue,
                    json.get(expectedKey).asText());
        } else {
            Assert.assertEquals("expected key " + expectedKey + " to be null",
                    JsonNodeType.NULL, json.get(expectedKey).getNodeType());
        }

    }

    /**
     * Mocks for
     * {@link DAUUtils#trackDAU(VaadinService, HttpServletRequest, HttpServletResponse)}.
     */
    private static class MocksForTrackDAU {
        private VaadinService service;
        private HttpServletRequest request;
        private HttpServletResponse response;

        public MocksForTrackDAU() {
            this.service = Mockito.mock(VaadinServletService.class);
            this.request = Mockito.mock(HttpServletRequest.class);
            this.response = Mockito.mock(HttpServletResponse.class);

            VaadinContext vaadinContext = Mockito.mock(VaadinContext.class);
            when(vaadinContext.getAttribute(any())).thenReturn(null);
            MockDeploymentConfiguration config = new MockDeploymentConfiguration();
            config.setProductionMode(true);
            config.setApplicationOrSystemProperty(Constants.DAU_TOKEN, "true");
            when(service.getDeploymentConfiguration()).thenReturn(config);
            when(service.getContext()).thenReturn(vaadinContext);
        }

        private void assertNoEnforcement(DAUUtils.EnforcementResult result) {
            assertNotNull(result);
            assertNotNull(result.endRequestAction());
            assertNull(result.messages());
            assertNull(result.origin());
            verify(service).requestStart(any(VaadinRequest.class),
                    any(VaadinResponse.class));
            verify(service, Mockito.never()).requestEnd(
                    any(VaadinRequest.class), Mockito.isNull(),
                    Mockito.isNull());
        }

        private void assertEnforcement(DAUUtils.EnforcementResult result) {
            assertNotNull(result);
            assertNotNull(result.endRequestAction());
            assertNotNull(result.messages());
            assertNotNull(result.origin());
            verify(service).requestStart(any(VaadinRequest.class),
                    any(VaadinResponse.class));
            verify(service, Mockito.never()).requestEnd(
                    any(VaadinRequest.class), Mockito.isNull(),
                    Mockito.isNull());
        }
    }
}