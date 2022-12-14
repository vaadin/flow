package com.vaadin.flow.uitest.ui.push;

import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;

import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.testbench.BrowserTest;

public class PushConfigurationWebSocketIT extends PushConfigurationTest {

    @BrowserTest
    public void testWebsocket() throws InterruptedException {
        findElement(By.id("transport"))
                .findElement(By.id(
                        Transport.WEBSOCKET.name().toLowerCase(Locale.ENGLISH)))
                .click();

        findElement(By.id("push-mode"))
                .findElement(By.id(
                        PushMode.AUTOMATIC.name().toLowerCase(Locale.ENGLISH)))
                .click();

        Assertions.assertNotNull(getStatusText());
        Assertions.assertTrue(
                getStatusText().contains("fallbackTransport: long-polling"));
        Assertions.assertTrue(getStatusText().contains("transport: websocket"));

        waitForServerCounterToUpdate();

        Assertions.assertEquals(Transport.WEBSOCKET.getIdentifier(),
                getTransport());
    }
}
