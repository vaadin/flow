package com.vaadin.hummingbird.uitest.ui.scroll;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.testutil.PhantomJSTest;
import com.vaadin.testbench.By;

public class ScrollIT extends PhantomJSTest {

    // PhantomJS scroll position may differ a little locally and in Travis
    private static final int allowedScrollYVariance = 5;

    @Test
    public void testScrollRestoration_basicBackForward() {
        open();

        verifyView("ScrollView");
        verifyHistoryStatePosition("nan");
        verifyScrollY(0);

        scrollYBy(100);
        verifyScrollY(100);

        clickLink("ScrollView2");

        verifyView("ScrollView2");
        verifyHistoryStatePosition("1");
        verifyScrollY(0);

        scrollYBy(600);
        verifyScrollY(600);

        clickLink("ScrollView3");

        verifyView("ScrollView3");
        verifyHistoryStatePosition("2");
        verifyScrollY(0);

        scrollYBy(1600);
        verifyScrollY(1600);

        back();

        verifyView("ScrollView2");
        verifyHistoryStatePosition("1");
        verifyScrollY(600);

        back();

        verifyView("ScrollView");
        verifyHistoryStatePosition("0");
        verifyScrollY(100);

        forward();

        verifyView("ScrollView2");
        verifyHistoryStatePosition("1");
        verifyScrollY(600);

        forward();

        verifyView("ScrollView3");
        verifyHistoryStatePosition("2");
        verifyScrollY(1600);
    }

    @Test
    public void testScrollRestoration_sameViewFragmentChanges() {
        open();

        verifyView("ScrollView");
        verifyHistoryStatePosition("nan");

        scrollYBy(100);
        verifyScrollY(100);

        clickLink("ScrollView#row3");

        verifyView("ScrollView#row3");
        verifyHistoryStatePosition("1");
        verifyScrollY(1044);

        scrollYBy(-500);
        verifyScrollY(1044 - 500);

        back();

        verifyView("ScrollView");
        verifyHistoryStatePosition("0");
        verifyScrollY(100);

        scrollYBy(200);
        verifyScrollY(100 + 200);

        forward();

        verifyView("ScrollView#row3");
        verifyHistoryStatePosition("1");
        verifyScrollY(1044 - 500);

        back();

        verifyView("ScrollView");
        verifyHistoryStatePosition("0");
        verifyScrollY(100 + 200);

        forward();

        verifyView("ScrollView#row3");
        verifyHistoryStatePosition("1");
        verifyScrollY(1044 - 500);
    }

    @Test
    public void testScrollRestoration_reclickingSameFragment_scrollsBackToFragmentButDoesntAddNewState() {
        open();

        verifyView("ScrollView");
        verifyHistoryStatePosition("nan");

        clickLink("ScrollView#row3");

        verifyView("ScrollView#row3");
        verifyHistoryStatePosition("1");
        verifyScrollY(1044);

        scrollYBy(-600);
        verifyScrollY(1044 - 600);

        clickLink("ScrollView#row3");

        verifyView("ScrollView#row3");
        verifyHistoryStatePosition("1");
        verifyScrollY(1044);

        back();

        verifyView("ScrollView");
        verifyHistoryStatePosition("0");
        verifyScrollY(0);

        forward();

        verifyView("ScrollView#row3");
        verifyHistoryStatePosition("1");
        verifyScrollY(1044);
    }

    @Test
    public void testScrollRestoration_samePageFragmentNotRouterLink() {
        open();

        verifyView("ScrollView");
        verifyHistoryStatePosition("nan");

        clickLink("ScrollView3");

        verifyView("ScrollView3");
        verifyHistoryStatePosition("1");
        verifyScrollY(0);

        scrollYBy(500);
        verifyScrollY(500);

        // this is not a router link, but should work since only fragment
        // changes
        clickLink("ScrollView3#row10");

        verifyView("ScrollView3#row10");
        verifyHistoryStatePosition("2");
        verifyScrollY(4544);

        scrollYBy(-1000);
        verifyScrollY(4544 - 1000);

        back();

        verifyView("ScrollView3");
        verifyHistoryStatePosition("1");
        verifyScrollY(500);

        forward();

        verifyView("ScrollView3#row10");
        verifyHistoryStatePosition("2");
        verifyScrollY(4544 - 1000);
    }

    private void back() {
        getTestBenchCommandExecutor().executeScript("history.back();");
    }

    private void forward() {
        getTestBenchCommandExecutor().executeScript("history.forward();");
    }

    private void verifyScrollY(int scrollY) {
        int actualScrollY = getScrollY();
        if (scrollY - allowedScrollYVariance <= actualScrollY
                && actualScrollY <= scrollY + allowedScrollYVariance) {
            return;
        }
        Assert.fail("Invalid scrollY, expected " + scrollY + " actual "
                + actualScrollY);
    }

    private void verifyView(String viewPath) {
        Assert.assertEquals("Invalid URL", getRootURL()
                + "/view/com.vaadin.hummingbird.uitest.ui.scroll." + viewPath,
                getDriver().getCurrentUrl());
        if (viewPath.contains("#")) {
            viewPath = viewPath.substring(0, viewPath.indexOf("#"));
        }
        Assert.assertEquals("Invalid view header", viewPath,
                findElement(By.tagName("h1")).getText());
    }

    private void verifyHistoryStatePosition(String position) {
        findElement(By.id("historylength")).click();
        Assert.assertEquals("Invalid history length", position,
                findElement(By.id("historylength")).getText());
    }

    private void scrollYBy(int delta) {
        getTestBenchCommandExecutor().executeScript(
                "window.scrollBy(0," + Integer.toString(delta) + ");");
    }

    private void clickLink(String linkText) {
        findElement(By.linkText(linkText)).click();
    }

    private int getScrollY() {
        findElement(By.id("scrolly")).click();
        return Integer.parseInt(findElement(By.id("scrolly")).getText());
    }
}
