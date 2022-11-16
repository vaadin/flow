package com.vaadin.flow.uitest.ui.push;

import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;

import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.testbench.BrowserTest;

public class PushConfigurationLongPollingIT extends PushConfigurationTest {

    @BrowserTest
    public void testLongPolling() throws InterruptedException {
        findElement(By.id("transport")).findElement(By
                .id(Transport.LONG_POLLING.name().toLowerCase(Locale.ENGLISH)))
                .click();

        Assertions.assertNotNull(getStatusText());
        Assertions.assertTrue(
                getStatusText().contains("fallbackTransport: long-polling"));
        Assertions.assertTrue(
                getStatusText().contains("transport: long-polling"));

        findElement(By.id("push-mode"))
                .findElement(By.id(
                        PushMode.AUTOMATIC.name().toLowerCase(Locale.ENGLISH)))
                .click();
        waitUntil(driver -> Transport.LONG_POLLING.getIdentifier()
                .equals(getTransport()), 10);
        waitForServerCounterToUpdate();
    }

}
