package com.vaadin.flow.uitest.ui.faulttolerance;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openqa.selenium.By;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTestWithProxy;

public class NetworkInterruptionIT extends ChromeBrowserTestWithProxy {

    private AtomicBoolean stopWatcher = new AtomicBoolean(false);

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    @Override
    public void setup() throws Exception {
        super.setup();
        Path proxyMonitorFile = tempDir.newFile("flow-test-proxy-monitor.txt")
                .toPath();
        WatchService watchService = FileSystems.getDefault().newWatchService();
        tempDir.getRoot().toPath().register(watchService,
                StandardWatchEventKinds.ENTRY_MODIFY);
        AtomicBoolean stopWatcher = new AtomicBoolean(false);
        new Thread(() -> {
            WatchKey key;
            try (WatchService ws = watchService) {
                while (!stopWatcher.get()) {
                    key = ws.poll(100, TimeUnit.MILLISECONDS);
                    if (key != null) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            if (event.context() instanceof Path p
                                    && proxyMonitorFile.equals(tempDir.getRoot()
                                            .toPath().resolve(p))) {
                                if (Files.readString(proxyMonitorFile)
                                        .contains("stop")) {
                                    disconnectProxy();
                                }
                            }
                        }
                        key.reset();
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }).start();
        this.stopWatcher = stopWatcher;
        open("proxyMonitorFile=" + proxyMonitorFile.toAbsolutePath());
        testBench().disableWaitForVaadin();
    }

    @After
    public void stopWatcher() {
        stopWatcher.set(true);
    }

    @Test
    public void networkInterruption_clickIncrementButton_messageQueuedAndResent()
            throws IOException {
        disconnectProxy();

        $(NativeButtonElement.class)
                .id(NetworkInterruptionView.INCREMENT_BUTTON_ID).click();
        waitForReconnectAttempts();
        connectProxy();

        waitForLogMessage("Re-established connection to server");

        waitUntil(d -> Integer.parseInt($(SpanElement.class)
                .id(NetworkInterruptionView.COUNTER_ID).getText()) == 1);
        ensureNoSystemErrorFromServer();
    }

    @Test
    public void networkInterruption_clickIncrementButtonMultipleTime_messagesQueuedAndResent()
            throws IOException {
        disconnectProxy();

        NativeButtonElement button = $(NativeButtonElement.class)
                .id(NetworkInterruptionView.INCREMENT_BUTTON_ID);

        button.click();
        button.click();
        button.click();
        button.click();
        waitForReconnectAttempts();
        connectProxy();

        waitForLogMessage("Re-established connection to server");

        waitUntil(d -> Integer.parseInt($(SpanElement.class)
                .id(NetworkInterruptionView.COUNTER_ID).getText()) == 4);
        ensureNoSystemErrorFromServer();
    }

    @Test
    public void networkInterruption_dropProxyBeforeResponse_serverMessageCachedAndResent()
            throws Exception {
        $(NativeButtonElement.class)
                .id(NetworkInterruptionView.INCREMENT_STOP_PROXY_BUTTON_ID)
                .click();
        waitForReconnectAttempts();
        connectProxy();
        waitForLogMessage("Re-established connection to server");

        waitUntil(d -> Integer.parseInt($(SpanElement.class)
                .id(NetworkInterruptionView.COUNTER_ID).getText()) == 1);
        ensureNoSystemErrorFromServer();
    }

    private void waitForReconnectAttempts() {
        waitForLogMessage("Reconnect attempt 4 for XHR");
    }

    private void ensureNoSystemErrorFromServer() {
        // Make sure there is no error caused by messages sync lost
        waitForElementNotPresent(By.cssSelector("div.v-system-error"));
    }

    private void waitForLogMessage(String expectedMessage) {
        waitUntil(driver -> getBrowserLogs(true).stream().anyMatch(
                message -> expectedMessage.equals(message.toString())));
    }

}
