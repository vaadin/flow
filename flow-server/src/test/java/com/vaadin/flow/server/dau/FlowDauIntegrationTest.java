package com.vaadin.flow.server.dau;

import jakarta.servlet.http.Cookie;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.internal.hilla.EndpointRequestUtil;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.pro.licensechecker.LocalSubscriptionKey;
import com.vaadin.pro.licensechecker.SubscriptionKey;
import com.vaadin.pro.licensechecker.dau.DauIntegration;

import static org.junit.Assert.*;

public class FlowDauIntegrationTest {

    @Test
    public void generateNewCookie_setsUpExpectedParameters() {
        try (MockedStatic<DauIntegration> key = Mockito
                .mockStatic(DauIntegration.class)) {
            key.when(DauIntegration::newTrackingHash).thenReturn("hash");
            VaadinRequest request = Mockito.mock(VaadinRequest.class);
            Mockito.when(request.isSecure()).thenReturn(true);
            Cookie cookie = FlowDauIntegration.generateNewCookie(request);
            String[] hashAndTime = cookie.getValue().split("\\$");
            Assert.assertEquals("hash", hashAndTime[0]);
            Assert.assertFalse(hashAndTime[1].isBlank());
            Assert.assertEquals(DAUUtils.DAU_COOKIE_NAME, cookie.getName());
            Assert.assertTrue(cookie.isHttpOnly());
            Assert.assertTrue(cookie.getSecure());
            Assert.assertEquals(DAUUtils.DAU_COOKIE_MAX_AGE_IN_SECONDS,
                    cookie.getMaxAge());
            Assert.assertEquals("/", cookie.getPath());
        }
    }

    @Test
    public void generateNewCookie_notSecureRequest_cookieNotSecure() {
        try (MockedStatic<DauIntegration> key = Mockito
                .mockStatic(DauIntegration.class)) {
            key.when(DauIntegration::newTrackingHash).thenReturn("hash");
            VaadinRequest request = Mockito.mock(VaadinRequest.class);
            Mockito.when(request.isSecure()).thenReturn(false);
            Cookie cookie = FlowDauIntegration.generateNewCookie(request);
            Assert.assertFalse(cookie.getSecure());
        }
    }
}
