package com.vaadin.flow.uitest.ui.push;

import java.util.Locale;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;

public class PushConfigurationLongPollingIT extends PushConfigurationTest {

    @Test
    public void testLongPolling() throws InterruptedException {
        findElement(By.id("transport")).findElement(By
                .id(Transport.LONG_POLLING.name().toLowerCase(Locale.ENGLISH)))
                .click();

        Assert.assertThat(getStatusText(),
                CoreMatchers.containsString("fallbackTransport: long-polling"));
        Assert.assertThat(getStatusText(),
                CoreMatchers.containsString("transport: long-polling"));

        findElement(By.id("push-mode"))
                .findElement(By.id(
                        PushMode.AUTOMATIC.name().toLowerCase(Locale.ENGLISH)))
                .click();
        waitUntil(driver -> Transport.LONG_POLLING.getIdentifier()
                .equals(getTransport()), 10);
        waitForServerCounterToUpdate();
    }

}