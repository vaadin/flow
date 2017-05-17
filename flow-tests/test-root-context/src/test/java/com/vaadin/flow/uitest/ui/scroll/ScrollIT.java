package com.vaadin.flow.uitest.ui.scroll;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.Dimension;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.By;

public class ScrollIT extends ChromeBrowserTest {

    // PhantomJS scroll position may differ a little locally and in Travis
    private static final int allowedScrollVariance = 5;

    @Test
    public void testScrollRestoration_basicBackForward() {
        openTestWithBrowserSized();

        verifyView("ScrollView");
        verifyHistoryStatePosition("nan");
        verifyScroll(0, 0);

        scrollYBy(100, 222);
        verifyScroll(100, 222);

        clickLink("ScrollView2");

        verifyView("ScrollView2");
        verifyHistoryStatePosition("1");
        verifyScroll(0, 0);

        scrollYBy(600, 600);
        verifyScroll(600, 600);

        clickLink("ScrollView3");

        verifyView("ScrollView3");
        verifyHistoryStatePosition("2");
        verifyScroll(0, 0);

        scrollYBy(1600, 1600);
        verifyScroll(1600, 1600);

        back();

        verifyView("ScrollView2");
        verifyHistoryStatePosition("1");
        verifyScroll(600, 600);

        back();

        verifyView("ScrollView");
        verifyHistoryStatePosition("0");
        verifyScroll(100, 222);

        forward();

        verifyView("ScrollView2");
        verifyHistoryStatePosition("1");
        verifyScroll(600, 600);

        forward();

        verifyView("ScrollView3");
        verifyHistoryStatePosition("2");
        verifyScroll(1600, 1600);
    }

    @Test
    public void testScrollRestoration_pagesWithFraments() {
        openTestWithBrowserSized();

        verifyView("ScrollView");
        verifyHistoryStatePosition("nan");
        verifyScroll(0, 0);

        scrollYBy(100, 222);
        verifyScroll(100, 222);

        clickLink("ScrollView2#row5");

        verifyView("ScrollView2#row5");
        verifyHistoryStatePosition("1");
        verifyScroll(2037, 222);

        scrollYBy(-600, 600);
        verifyScroll(2037 - 600, 222 + 600);

        clickLink("ScrollView#row3");

        verifyView("ScrollView#row3");
        verifyHistoryStatePosition("2");
        verifyScroll(1037, 222 + 600);

        scrollYBy(-500, -500);
        verifyScroll(1037 - 500, 222 + 600 - 500);

        back();

        verifyView("ScrollView2#row5");
        verifyHistoryStatePosition("1");
        verifyScroll(2037 - 600, 222 + 600);

        back();

        verifyView("ScrollView");
        verifyHistoryStatePosition("0");
        verifyScroll(100, 222);

        forward();

        verifyView("ScrollView2#row5");
        verifyHistoryStatePosition("1");
        verifyScroll(2037 - 600, 222 + 600);

        forward();

        verifyView("ScrollView#row3");
        verifyHistoryStatePosition("2");
        verifyScroll(1037 - 500, 222 + 600 - 500);
    }

    @Test
    public void testScrollRestoration_sameViewFragmentChanges() {
        openTestWithBrowserSized();

        verifyView("ScrollView");
        verifyHistoryStatePosition("nan");

        scrollYBy(100, 100);
        verifyScroll(100, 100);

        // browser only modifies the scrollY on fragment click if page stays
        // the same
        clickLink("ScrollView#row3");

        verifyView("ScrollView#row3");
        verifyHistoryStatePosition("1");
        verifyScroll(1037, 100);

        scrollYBy(-500, 200);
        verifyScroll(1037 - 500, 100 + 200);

        back();

        verifyView("ScrollView");
        verifyHistoryStatePosition("0");
        verifyScroll(100, 100);

        scrollYBy(200, 50);
        verifyScroll(100 + 200, 100 + 50);

        forward();

        verifyView("ScrollView#row3");
        verifyHistoryStatePosition("1");
        verifyScroll(1037 - 500, 100 + 200);

        back();

        verifyView("ScrollView");
        verifyHistoryStatePosition("0");
        verifyScroll(100 + 200, 100 + 50);

        forward();

        verifyView("ScrollView#row3");
        verifyHistoryStatePosition("1");
        verifyScroll(1037 - 500, 100 + 200);
    }

    @Test
    public void testScrollRestoration_reclickingSameFragment_scrollsBackToFragmentButDoesntAddNewState() {
        openTestWithBrowserSized();

        verifyView("ScrollView");
        verifyHistoryStatePosition("nan");

        clickLink("ScrollView#row3");

        verifyView("ScrollView#row3");
        verifyHistoryStatePosition("1");
        verifyScroll(1037, 0);

        scrollYBy(-600, 0);
        verifyScroll(1037 - 600, 0);

        clickLink("ScrollView#row3");

        verifyView("ScrollView#row3");
        verifyHistoryStatePosition("1");
        verifyScroll(1037, 0);

        back();

        verifyView("ScrollView");
        verifyHistoryStatePosition("0");
        verifyScroll(1037, 0);

        forward();

        verifyView("ScrollView#row3");
        verifyHistoryStatePosition("1");
        verifyScroll(1037, 0);
    }

    @Test
    public void testScrollRestoration_samePageFragmentNotRouterLink() {
        openTestWithBrowserSized();

        verifyView("ScrollView");
        verifyHistoryStatePosition("nan");

        clickLink("ScrollView3");

        verifyView("ScrollView3");
        verifyHistoryStatePosition("1");
        verifyScroll(0, 0);

        scrollYBy(500, 0);
        verifyScroll(500, 0);

        // this is not a router link, but should work since only fragment
        // changes
        clickLink("ScrollView3#row10");

        verifyView("ScrollView3#row10");
        verifyHistoryStatePosition("2");
        verifyScroll(4537, 0);

        scrollYBy(-1000, 0);
        verifyScroll(4537 - 1000, 0);

        back();

        verifyView("ScrollView3");
        verifyHistoryStatePosition("1");
        verifyScroll(500, 0);

        forward();

        verifyView("ScrollView3#row10");
        verifyHistoryStatePosition("2");
        verifyScroll(4537 - 1000, 0);
    }

    @Test
    public void testNavigatingBackFromExternalSite() {
        getDriver().get("https://dev.vaadin.com");
        Assert.assertTrue(
                getDriver().getCurrentUrl().contains("dev.vaadin.com"));

        openTestWithBrowserSized();

        verifyView("ScrollView");
        verifyHistoryStatePosition("nan");
        verifyScroll(0, 0);

        scrollYBy(100, 222);
        verifyScroll(100, 222);

        clickLink("ScrollView2#row5");

        verifyView("ScrollView2#row5");
        verifyHistoryStatePosition("1");
        verifyScroll(2037, 222);

        scrollYBy(-600, 200);
        verifyScroll(2037 - 600, 222 + 200);

        getDriver().get("https://dev.vaadin.com");
        Assert.assertTrue(
                getDriver().getCurrentUrl().contains("dev.vaadin.com"));

        back();

        verifyView("ScrollView2#row5");
        verifyHistoryStatePosition("1");
        verifyScroll(2037 - 600, 222 + 200);

        back();

        verifyView("ScrollView");
        verifyHistoryStatePosition("0");
        verifyScroll(100, 222);

        scrollYBy(200, 100);
        verifyScroll(100 + 200, 222 + 100);

        back();
        Assert.assertTrue(
                getDriver().getCurrentUrl().contains("dev.vaadin.com"));

        forward();

        verifyView("ScrollView");
        verifyHistoryStatePosition("0");
        verifyScroll(100 + 200, 222 + 100);

        forward();

        verifyView("ScrollView2#row5");
        verifyHistoryStatePosition("1");
        verifyScroll(2037 - 600, 222 + 200);
    }

    private void openTestWithBrowserSized() {
        getDriver().manage().window().setSize(new Dimension(400, 400));
        open();
    }

    private void back() {
        getDriver().navigate().back();
    }

    private void forward() {
        getDriver().navigate().forward();
    }

    private void verifyScroll(int scrollY, int scrollX) {
        verifyScrollY(scrollY);
        verifyScrollX(scrollX);
    }

    private void verifyScrollY(int scrollY) {
        int actualScrollY = getScrollY();
        if (scrollY - allowedScrollVariance <= actualScrollY
                && actualScrollY <= scrollY + allowedScrollVariance) {
            return;
        }
        Assert.fail("Invalid scrollY, expected " + scrollY + " actual "
                + actualScrollY);
    }

    private void verifyScrollX(int scrollX) {
        int actualScrollX = getScrollX();
        if (scrollX - allowedScrollVariance <= actualScrollX
                && actualScrollX <= scrollX + allowedScrollVariance) {
            return;
        }
        Assert.fail("Invalid scrollX, expected " + scrollX + " actual "
                + actualScrollX);
    }

    private void verifyView(String viewPath) {
        Assert.assertEquals("Invalid URL", getRootURL()
                + "/view/com.vaadin.flow.uitest.ui.scroll." + viewPath,
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

    private void scrollYBy(int deltaY, int deltaX) {
        executeScript("window.scrollBy(" + Integer.toString(deltaX) + ","
                + Integer.toString(deltaY) + ");");
    }

    private void clickLink(String linkText) {
        findElement(By.linkText(linkText)).click();
    }

    private int getScrollY() {
        findElement(By.id("scrolly")).click();
        return Integer.parseInt(findElement(By.id("scrolly")).getText());
    }

    private int getScrollX() {
        findElement(By.id("scrollx")).click();
        return Integer.parseInt(findElement(By.id("scrollx")).getText());
    }
}
