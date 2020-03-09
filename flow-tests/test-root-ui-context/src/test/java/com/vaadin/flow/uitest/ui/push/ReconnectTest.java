package com.vaadin.flow.uitest.ui.push;

import java.io.IOException;

import org.junit.Test;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;

public abstract class ReconnectTest extends ChromeBrowserTestWithProxy {

    @Override
    public void setup() throws Exception {
        super.setup();

        open();

        startTimer();
        waitUntilServerCounterChanges();

        testBench().disableWaitForVaadin();
    }

    @Test
    public void messageIsQueuedOnDisconnect() throws IOException {
        disconnectProxy();

        clickButtonAndWaitForTwoReconnectAttempts();

        connectAndVerifyConnectionEstablished();
        waitUntilClientCounterChanges(1);
    }

    @Test
    public void messageIsNotSentBeforeConnectionIsEstablished()
            throws IOException, InterruptedException {
        disconnectProxy();

        waitForNextReconnectionAttempt();
        clickButtonAndWaitForTwoReconnectAttempts();

        connectAndVerifyConnectionEstablished();
        waitUntilClientCounterChanges(1);
    }

    @Override
    protected DesiredCapabilities getDesiredCapabilities() {
        return super.getDesiredCapabilities();
    }

    private void clickButtonAndWaitForTwoReconnectAttempts() {
        clickClientButton();

        // Reconnection attempt is where pending messages can
        // falsely be sent to server.
        waitForNextReconnectionAttempt();

        // Waiting for the second reconnection attempt makes sure that the
        // first attempt has been completed or aborted.
        waitForNextReconnectionAttempt();
    }

    private void clickClientButton() {
        getIncrementClientCounterButton().click();
    }

    private void waitForNextReconnectionAttempt() {
        waitUntil(driver -> getBrowserLogs(true).stream()
                .filter(String.class::isInstance)
                .anyMatch("Reopening push connection"::equals));
    }

    private void connectAndVerifyConnectionEstablished() throws IOException {
        connectProxy();
        waitUntilServerCounterChanges();
    }

    private WebElement getIncrementClientCounterButton() {
        return BasicPushIT.getIncrementButton(this);
    }

    private void waitUntilServerCounterChanges() {
        final int counter = BasicPushIT.getServerCounter(this);
        waitUntil(input -> {
            try {
                return BasicPushIT
                        .getServerCounter(ReconnectTest.this) > counter;
            } catch (NoSuchElementException e) {
                return false;
            }
        }, 30);
    }

    private void waitUntilClientCounterChanges(final int expectedValue) {
        waitUntil(
                input -> BasicPushIT
                        .getClientCounter(ReconnectTest.this) == expectedValue,
                5);
    }

    private void startTimer() {
        BasicPushIT.getServerCounterStartButton(this).click();
    }

}