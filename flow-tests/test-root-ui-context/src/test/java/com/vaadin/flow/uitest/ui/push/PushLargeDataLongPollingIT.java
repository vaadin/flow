package com.vaadin.flow.uitest.ui.push;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;

import com.vaadin.flow.testcategory.PushTests;

@Category(PushTests.class)
public class PushLargeDataLongPollingIT extends AbstractLogTest {

    @Test
    public void longPollingLargeData() throws Exception {
        open();

        // Without this there is a large chance that we will wait for all pushes
        // to complete before moving on
        getCommandExecutor().disableWaitForVaadin();

        push();
        // Push complete. Browser will reconnect now as > 10MB has been sent
        // Push again to ensure push still works
        push();

    }

    private void push() throws InterruptedException {
        // Wait for startButton to be present
        waitForElementVisible(By.id("startButton"));

        findElement(By.id("startButton")).click();
        // Wait for push to start
        waitUntil(textToBePresentInElement(() -> getLastLog(), "Package "));

        // Wait for until push should be done
        Thread.sleep(PushLargeData.DEFAULT_DURATION_MS);

        // Wait until push is actually done
        waitUntil(
                textToBePresentInElement(() -> getLastLog(), "Push complete"));
    }

}