package com.vaadin.flow.uitest.ui.push;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testcategory.PushTests;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import static org.junit.Assert.assertFalse;

@Category(PushTests.class)
public class TogglePushIT extends ChromeBrowserTest {

    @Test
    public void togglePushInInit() throws Exception {
        // Open with push disabled
        open("push=disabled");

        assertFalse(getPushToggle().isSelected());

        getDelayedCounterUpdateButton().click();
        Thread.sleep(2000);
        Assert.assertEquals("Counter has been updated 0 times",
                getCounterText());

        // Open with push enabled
        open("push=enabled");
        Assert.assertThat(getPushToggle().getText(),
                CoreMatchers.containsString("Push enabled"));

        getDelayedCounterUpdateButton().click();
        Thread.sleep(2000);
        Assert.assertEquals("Counter has been updated 1 times",
                getCounterText());

    }

    @Test
    public void togglePush() throws InterruptedException {
        open();
        getDelayedCounterUpdateButton().click();
        Thread.sleep(2000);

        // Push is enabled, so text gets updated
        Assert.assertEquals("Counter has been updated 1 times",
                getCounterText());

        // Disable push
        getPushToggle().click();
        getDelayedCounterUpdateButton().click();
        Thread.sleep(2000);
        // Push is disabled, so text is not updated
        Assert.assertEquals("Counter has been updated 1 times",
                getCounterText());

        getDirectCounterUpdateButton().click();
        // Direct update is visible, and includes previous update
        Assert.assertEquals("Counter has been updated 3 times",
                getCounterText());

        // Re-enable push
        getPushToggle().click();
        getDelayedCounterUpdateButton().click();
        Thread.sleep(2000);

        // Push is enabled again, so text gets updated
        Assert.assertEquals("Counter has been updated 4 times",
                getCounterText());
    }

    private WebElement getDirectCounterUpdateButton() {
        return findElement(By.id("update-counter"));
    }

    private WebElement getPushToggle() {
        return findElement(By.id("push-setting"));
    }

    private WebElement getDelayedCounterUpdateButton() {
        return findElement(By.id("update-counter-async"));
    }

    private String getCounterText() {
        return findElement(By.id("counter")).getText();
    }

}