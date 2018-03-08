package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class LockingIT extends ChromeBrowserTest {

    @Test
    public void testLockingTheUIFor4HeartBeats() {
        open();

        clickButtonAndCheckNotification("check", LockingUI.ALL_OK);
        clickButtonAndCheckNotification("lock", LockingUI.LOCKING_ENDED);
        clickButtonAndCheckNotification("check", LockingUI.ALL_OK);
    }

    private void clickButtonAndCheckNotification(String buttonId, String text) {
        
        findElement(By.id(buttonId)).click();

        checkMessage(text);
    }

    private void checkMessage(String text) {

        waitForElementPresent(By.id("message"));

        WebElement message = findElement(By.id("message"));
        Assert.assertEquals("Unexpected text content in Message", text,
                message.getText());
    }
}
