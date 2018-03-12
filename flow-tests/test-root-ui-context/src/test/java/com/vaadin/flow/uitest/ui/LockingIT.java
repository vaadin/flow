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

        clickButtonAndCheckMessage("check", LockingUI.ALL_OK);
        clickButtonAndCheckMessage("lock", LockingUI.LOCKING_ENDED);
        clickButtonAndCheckMessage("check", LockingUI.ALL_OK);
    }

    private void clickButtonAndCheckMessage(String buttonId, String text) {

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
