package com.vaadin.flow.uitest.ui.push;

import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.testutil.TestTag;
import com.vaadin.flow.testutil.jupiter.ChromeBrowserTest;

@Tag(TestTag.PUSH_TESTS)
abstract class PushConfigurationTest extends ChromeBrowserTest {

    @Override
    protected Class<? extends Component> getViewClass() {
        return PushConfiguration.class;
    }

    @BeforeEach
    public void setup() throws Exception {
        open();
        disablePush();
    }

    protected String getStatusText() {
        WebElement statusLabel = findElement(By.id("status"));

        return statusLabel.getText();
    }

    protected void disablePush() throws InterruptedException {
        findElement(By.id("push-mode")).findElement(
                By.id(PushMode.DISABLED.name().toLowerCase(Locale.ENGLISH)));

        int counter = getServerCounter();
        Thread.sleep(2000);
        Assertions.assertEquals(counter, getServerCounter(),
                "Server count changed without push enabled");
    }

    protected int getServerCounter() {
        return Integer.parseInt(findElement(By.id("server-counter")).getText());
    }

    protected void waitForServerCounterToUpdate() {
        int counter = getServerCounter();
        final int waitCounter = counter + 2;
        waitUntil(input -> getServerCounter() >= waitCounter);
    }

    @SuppressWarnings("unchecked")
    protected Map<String, ?> getClientPushConfig() {
        return (Map<String, ?>) getCommandExecutor().executeScript(
                "return window.Vaadin.Flow.clients.view.debug().pushConfiguration;");
    }

    @SuppressWarnings("unchecked")
    protected Map<String, ?> getClientPushConfigParams() {
        Map<String, ?> map = (Map<String, ?>) getClientPushConfig()
                .get("parameters");
        return (Map<String, ?>) map.get("pushConfigurationParameters");
    }

    protected String getPushMode() {
        return getClientPushConfig().get("pushMode").toString();
    }

    protected String getTransport() {
        return getClientPushConfigParams().get("transport").toString();
    }

    protected String getFallBackTransport() {
        return getClientPushConfigParams().get("fallbackTransport").toString();
    }
}
