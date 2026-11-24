package com.vaadin.flow.uitest.ui.push;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.TimeoutException;

import com.vaadin.flow.testcategory.PushTests;

/**
 * Checks that push keeps working after the push connection has been idle for
 * longer than the server's idle timeouts, which close an idle websocket and
 * resume a suspended long polling request. The view is served by
 * {@link IdlePushChannelServlet}, which shortens those timeouts so that the
 * test does not have to wait for the five minute default.
 */
@Category(PushTests.class)
public abstract class IdlePushChannelIT extends AbstractBrowserConsoleTest {

    @Override
    protected String getTestPath() {
        return "/idle-push/" + URLEncoder.encode(getViewClass().getName(),
                StandardCharsets.UTF_8);
    }

    @Test
    public void longWaitBetweenActions() {
        open();
        BasicPushIT.getIncrementButton(this).click();
        testBench().disableWaitForVaadin();
        waitForClientCounter(1);
        getBrowserLogs(true);
        String reconnected = "Push connection re-established using "
                + getViewClass().getAnnotation(CustomPush.class).transport()
                        .getIdentifier();

        // The idle timeout ends the push connection and the client opens a
        // new one; without it, this test would not test an idle timeout.
        // Reading the logs does not touch the server, so the connection
        // stays idle meanwhile.
        try {
            waitUntil(
                    driver -> getBrowserLogs(false).stream()
                            .map(String::valueOf).anyMatch(reconnected::equals),
                    10 * IdlePushChannelServlet.IDLE_TIMEOUT_MS / 1000);
        } catch (TimeoutException e) {
            Assert.fail("Push connection was not re-established while idle. "
                    + "Browser logs: " + getBrowserLogs(false));
        }

        BasicPushIT.getIncrementButton(this).click();
        waitForClientCounter(2);

        // The server can still push to the client
        BasicPushIT.getServerCounterStartButton(this).click();
        try {
            waitUntil(driver -> BasicPushIT.getServerCounter(this) > 0);
        } catch (TimeoutException e) {
            Assert.fail("Server push did not reach the client after the idle "
                    + "period. Expected server counter > 0 but was "
                    + BasicPushIT.getServerCounter(this));
        }
    }

    private void waitForClientCounter(int expected) {
        try {
            waitUntil(driver -> BasicPushIT.getClientCounter(this) == expected);
        } catch (TimeoutException e) {
            Assert.fail("Expected client counter " + expected + " but was "
                    + BasicPushIT.getClientCounter(this));
        }
    }

}
