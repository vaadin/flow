package com.vaadin.hummingbird.uitest.ui.push;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;

import com.vaadin.hummingbird.uitest.PhantomJSTest;
import com.vaadin.testbench.TestBenchTestCase;

public abstract class AbstractBasicPushIT extends PhantomJSTest {

    @Test
    public void testPush() throws InterruptedException {
        open();

        getIncrementButton().click();
        testBench().disableWaitForVaadin();

        waitUntilClientCounterChanges(1);

        getIncrementButton().click();
        getIncrementButton().click();
        getIncrementButton().click();
        waitUntilClientCounterChanges(4);

        // Test server initiated push
        getServerCounterStartButton().click();
        waitUntilServerCounterChanges();
    }

    public static int getClientCounter(TestBenchTestCase t) {
        WebElement clientCounterElem = t
                .findElement(By.id(BasicPushUI.CLIENT_COUNTER_ID));
        return Integer.parseInt(clientCounterElem.getText());
    }

    protected WebElement getIncrementButton() {
        return getIncrementButton(this);
    }

    protected WebElement getServerCounterStartButton() {
        return getServerCounterStartButton(this);
    }

    public static int getServerCounter(TestBenchTestCase t) {
        WebElement serverCounterElem = t
                .findElement(By.id(BasicPushUI.SERVER_COUNTER_ID));
        return Integer.parseInt(serverCounterElem.getText());
    }

    public static WebElement getServerCounterStartButton(TestBenchTestCase t) {
        return t.findElement(By.id(BasicPushUI.START_TIMER_ID));
    }

    public static WebElement getServerCounterStopButton(TestBenchTestCase t) {
        return t.findElement(By.id(BasicPushUI.STOP_TIMER_ID));
    }

    public static WebElement getIncrementButton(TestBenchTestCase t) {
        return t.findElement(By.id(BasicPushUI.INCREMENT_BUTTON_ID));
    }

    protected void waitUntilClientCounterChanges(final int expectedValue) {
        waitUntil(new ExpectedCondition<Boolean>() {

            @Override
            public Boolean apply(WebDriver input) {
                return AbstractBasicPushIT.getClientCounter(
                        AbstractBasicPushIT.this) == expectedValue;
            }
        }, 10);
    }

    protected void waitUntilServerCounterChanges() {
        final int counter = AbstractBasicPushIT.getServerCounter(this);
        waitUntil(new ExpectedCondition<Boolean>() {

            @Override
            public Boolean apply(WebDriver input) {
                return AbstractBasicPushIT
                        .getServerCounter(AbstractBasicPushIT.this) > counter;
            }
        }, 10);
    }

}
