package com.vaadin.flow;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class BackNavIT extends ChromeBrowserTest {

    public static final String BACK_NAV_FIRST_VIEW = "/view/com.vaadin.flow.BackNavFirstView";
    public static final String BACK_NAV_SECOND_VIEW = "/view/com.vaadin.flow.BackNavSecondView?param";

    // Test for https://github.com/vaadin/flow/issues/19839
    @Test
    public void testBackButtonAfterHistoryStateChange() {
        getDriver().get(getTestURL(getRootURL(), BACK_NAV_FIRST_VIEW, null));

        try {
            waitUntil(arg -> driver.getCurrentUrl()
                    .endsWith(BACK_NAV_FIRST_VIEW));
        } catch (TimeoutException e) {
            Assert.fail("URL wasn't updated to expected one: "
                    + BACK_NAV_FIRST_VIEW);
        }

        $(NativeButtonElement.class).first().click();

        try {
            waitUntil(arg -> driver.getCurrentUrl()
                    .endsWith(BACK_NAV_SECOND_VIEW));
        } catch (TimeoutException e) {
            Assert.fail("URL wasn't updated to expected one: "
                    + BACK_NAV_SECOND_VIEW);
        }

        // Navigate back; ensure we get the first URL again
        getDriver().navigate().back();
        try {
            waitUntil(arg -> driver.getCurrentUrl()
                    .endsWith(BACK_NAV_FIRST_VIEW));
        } catch (TimeoutException e) {
            Assert.fail("URL wasn't updated to expected one: "
                    + BACK_NAV_FIRST_VIEW);
        }
    }

    @Test
    public void validateNoAfterNavigationForReplaceState() {
        getDriver().get(getTestURL(getRootURL(), BACK_NAV_FIRST_VIEW, null));

        try {
            waitUntil(arg -> driver.getCurrentUrl()
                    .endsWith(BACK_NAV_FIRST_VIEW));
        } catch (TimeoutException e) {
            Assert.fail("URL wasn't updated to expected one: "
                    + BACK_NAV_FIRST_VIEW);
        }

        $(NativeButtonElement.class).first().click();

        try {
            waitUntil(arg -> driver.getCurrentUrl()
                    .endsWith(BACK_NAV_SECOND_VIEW));
        } catch (TimeoutException e) {
            Assert.fail("URL wasn't updated to expected one: "
                    + BACK_NAV_SECOND_VIEW);
        }

        Assert.assertEquals("Second view: 1",
                $(SpanElement.class).id(BackNavSecondView.CALLS).getText());
    }

}
