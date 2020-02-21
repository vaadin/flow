package com.vaadin.flow.contexttest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PushIT extends ChromeBrowserTest {

    @Test
    public void testNoPush() throws InterruptedException {
        doTest("routed", null, false);
    }

    @Test
    public void testWebsocket() throws InterruptedException {
        doTest("", Transport.WEBSOCKET, true);
    }

    @Test
    public void testWebsocketXhr() throws InterruptedException {
        doTest("", Transport.WEBSOCKET_XHR, true);
    }

    @Test
    public void testLongPolling() throws InterruptedException {
        doTest("", Transport.LONG_POLLING, true);
    }

    @Test
    public void testSubContextWebsocket() throws InterruptedException {
        doTest("sub-context", Transport.WEBSOCKET, true);
    }

    @Test
    public void testSubContextWebsocketXhr() throws InterruptedException {
        doTest("sub-context", Transport.WEBSOCKET_XHR, true);
    }

    @Test
    public void testSubContextLongPolling() throws InterruptedException {
        doTest("sub-context", Transport.LONG_POLLING, true);
    }

    @Test
    public void testRoutedWebsocketXhr() throws InterruptedException {
        doTest("routed", Transport.WEBSOCKET_XHR, true);
    }

    @Test
    public void testRoutedLongPolling() throws InterruptedException {
        doTest("routed", Transport.LONG_POLLING, true);
    }

    @Test
    public void testRoutedSubContextWebsocket() throws InterruptedException {
        doTest("routed/sub-context", Transport.WEBSOCKET, true);
    }

    @Test
    public void testRoutedSubContextWebsocketXhr() throws InterruptedException {
        doTest("routed/sub-context", Transport.WEBSOCKET_XHR, true);
    }

    @Test
    public void testRoutedSubContextLongPolling() throws InterruptedException {
        doTest("routed/sub-context", Transport.LONG_POLLING, true);
    }

    private void doTest(final String subContext, Transport transport, boolean pushMustWork) throws InterruptedException {
        String url = getRootURL() + "/custom-context-router/" + subContext;
        if (transport != null) {
            url += "?transport=" + transport.getIdentifier();
        }
        getDriver().get(url);
        waitForDevServer();

        findElement(By.id(DependencyLayout.RUN_PUSH_ID)).click();

        WebElement signal = findElement(By.id(DependencyLayout.PUSH_SIGNAL_ID));
        String sampleText = pushMustWork ? DependencyLayout.PUSH_WORKS_TEXT : DependencyLayout.NO_PUSH_YET_TEXT;
        try {
            waitUntil(driver -> signal.getText().equals(sampleText), 2);
        } catch (TimeoutException e) {
            Assert.fail("Push state check failed when waiting for '"
                    + sampleText + "' in element #"
                    + DependencyLayout.PUSH_SIGNAL_ID);
        }
    }
}
