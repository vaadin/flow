package com.vaadin.flow.uitest.ui;

import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ResynchronizationIT extends ChromeBrowserTest {

    /*
     * If a component is only added in a lost message, it should be present after
     * resynchronization.
     */
    @Test
    public void resynchronize_componentAddedInLostMessage_appearAfterResync() {
        open();

        findElement(By.id(ResynchronizationView.ADD_BUTTON)).click();

        waitForElementPresent(By.className(ResynchronizationView.ADDED_CLASS));

        findElement(By.id(ResynchronizationView.ADD_BUTTON)).click();

        waitUntil(driver -> findElements(
                By.className(ResynchronizationView.ADDED_CLASS)).size() == 2);
    }

    /*
     * If a @ClientCallable is invoked in a lost message, the promises waiting
     * for the return value from the server should be rejected rather than
     * remain pending.
     */
    @Test
    public void resynchronize_clientCallableInvoked_promisesAreRejected() {
        open();

        findElement(By.id(ResynchronizationView.CALL_BUTTON)).click();

        waitForElementPresent(By.className(ResynchronizationView.REJECTED_CLASS));
    }

}
