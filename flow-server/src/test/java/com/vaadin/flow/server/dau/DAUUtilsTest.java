package com.vaadin.flow.server.dau;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.SystemMessagesInfo;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceDauTest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.pro.licensechecker.dau.DauIntegration;
import com.vaadin.pro.licensechecker.dau.EnforcementException;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

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

            DAUUtils.trackUser(request, "trackingHash", null);
            dauIntegration.verifyNoInteractions();

            Assert.assertThrows(DauEnforcementException.class,
                    () -> DAUUtils.applyEnforcement(request, unused -> true));
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

            DAUUtils.trackUser(request, "trackingHash", null);
            dauIntegration.verify(
                    () -> DauIntegration.trackUser("trackingHash", null));

            Assert.assertThrows(DauEnforcementException.class,
                    () -> DAUUtils.applyEnforcement(request, unused -> true));

        } finally {
            VaadinSession.setCurrent(null);
        }
    }

    @Test
    public void jsonEnforcementResponse_noDauCustomizer_defaultMessages() {
        VaadinService service = VaadinServiceDauTest.vaadinServiceWithDau(null);
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        Mockito.when(request.getService()).thenReturn(service);
        String response = DAUUtils.jsonEnforcementResponse(request,
                new DauEnforcementException(new EnforcementException("STOP")));

        // remove JSON wrap
        response = response.replace("for(;;);[", "").replaceFirst("]$", "");
        JsonObject json = Json.parse(response).getObject("meta")
                .getObject("appError");

        EnforcementNotificationMessages expectedMessages = EnforcementNotificationMessages.DEFAULT;
        assertJsonErrorProperty("caption", expectedMessages.caption(), json);
        assertJsonErrorProperty("message", expectedMessages.message(), json);
        assertJsonErrorProperty("details", expectedMessages.details(), json);
        assertJsonErrorProperty("url", expectedMessages.url(), json);
    }

    @Test
    public void jsonEnforcementResponse_customMessages() {
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
                new DauEnforcementException(new EnforcementException("STOP")));
        response = response.replace("for(;;);[", "").replaceFirst("]$", "");
        JsonObject json = Json.parse(response).getObject("meta")
                .getObject("appError");

        assertJsonErrorProperty("caption", expectedMessages.caption(), json);
        assertJsonErrorProperty("message", expectedMessages.message(), json);
        assertJsonErrorProperty("details", expectedMessages.details(), json);
        assertJsonErrorProperty("url", expectedMessages.url(), json);

    }

    private void assertJsonErrorProperty(String expectedKey,
            String expectedValue, JsonObject json) {
        if (expectedValue != null) {
            Assert.assertEquals(expectedKey, expectedValue,
                    json.getString(expectedKey));
        } else {
            Assert.assertEquals("expected key " + expectedKey + " to be null",
                    JsonType.NULL, json.get(expectedKey).getType());
        }

    }

}