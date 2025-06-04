package com.vaadin.flow.uitest.ui.push;

import java.util.Locale;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;

import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.testcategory.IgnoreOSGi;

@Category(IgnoreOSGi.class)
public class PushConfigurationWebSocketIT extends PushConfigurationTest {

    @Test
    public void testWebsocket() throws InterruptedException {
        findElement(By.id("transport"))
                .findElement(By.id(
                        Transport.WEBSOCKET.name().toLowerCase(Locale.ENGLISH)))
                .click();

        findElement(By.id("push-mode"))
                .findElement(By.id(
                        PushMode.AUTOMATIC.name().toLowerCase(Locale.ENGLISH)))
                .click();

        MatcherAssert.assertThat(getStatusText(),
                CoreMatchers.containsString("fallbackTransport: long-polling"));
        MatcherAssert.assertThat(getStatusText(),
                CoreMatchers.containsString("transport: websocket"));

        waitForServerCounterToUpdate();

        Assert.assertEquals(Transport.WEBSOCKET.getIdentifier(),
                getTransport());
    }
}
