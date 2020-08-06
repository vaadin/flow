package com.vaadin.flow.uitest.ui;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class FragmentLinkIT extends ChromeBrowserTest {

    @Test
    public void testInsidePageNavigation_noRouterLinkHandling() {
        open();

        if (hasClientIssue("8236")) {
            return;
        }

        clickScrollerLink2();

        verifyInsideServletLocation(
                "com.vaadin.flow.uitest.ui.FragmentLinkView#Scroll_Target2");
        verifyHashChangeEvents(1);
        verifyScrollTarget2Visible();

        clickScrollerLink();

        verifyInsideServletLocation(
                "com.vaadin.flow.uitest.ui.FragmentLinkView#Scroll_Target");
        verifyHashChangeEvents(2);
        verifyScrollTargetVisible();
    }

    @Test
    public void testViewChangeWithFragment_scrollToPageAndHashChangeEventWorks() {
        open();

        if (hasClientIssue("7575")) {
            return;
        }

        clickAnotherViewLink();

        verifyInsideServletLocation(
                "com.vaadin.flow.uitest.ui.FragmentLinkView2#Scroll_Target");
        verifyHashChangeEvents(1);
        verifyScrollTargetVisible();
        verifyView2Open();
    }

    @Test
    public void testViewChangeWithFragment_serverOverridesLocation_noScrollOrHashChange() {
        open();

        if (hasClientIssue("7575")) {
            return;
        }

        clickOverriddenLink();

        verifyInsideServletLocation("overridden#Scroll_Target2");
        // history.replaceState won't fire fragment change
        verifyHashChangeEvents(0);
        verifyTopOfThePage();
    }

    private void clickScrollerLink() {
        clickLink("Scroller link");
    }

    private void clickScrollerLink2() {
        clickLink("Scroller link 2");
    }

    private void clickAnotherViewLink() {
        clickLink("Scroller link with different view");
    }

    private void clickOverriddenLink() {
        clickLink("Link that server overrides");
    }

    private void clickLink(String linkText) {
        WebElement link = findElement(By.linkText(linkText));
        scrollIntoViewAndClick(link);
    }

    private void verifyScrollTargetVisible() {
        int scrollPos = findElement(By.id("Scroll_Target")).getLocation()
                .getY();
        int expected = getScrollLocatorPosition();
        assertScrollPosition(expected, scrollPos);
    }

    private void verifyScrollTarget2Visible() {
        int scrollPos = findElement(By.id("Scroll_Target2")).getLocation()
                .getY();
        int expected = getScrollLocatorPosition();
        assertScrollPosition(expected, scrollPos);
    }

    private int getScrollLocatorPosition() {
        return findElement(By.id("scrollLocator")).getLocation().getY();
    }

    private void verifyTopOfThePage() {
        assertScrollPosition(0, getScrollLocatorPosition());
    }

    private void verifyView2Open() {
        Assert.assertNotNull("FragmentView2 not opened",
                findElement(By.id("view2")));
    }

    private void assertScrollPosition(int expected, int actual) {
        int lowerBound = expected - 2 > 0 ? expected - 2 : 0;
        int higherBound = expected + 2;
        Assert.assertTrue(
                "Invalid scroll position, expected " + expected
                        + " +-2px. actual " + actual,
                lowerBound <= expected && expected <= higherBound);
    }

    private void verifyHashChangeEvents(int numberOfEvents) {
        List<WebElement> spans = findElement(By.id("placeholder"))
                .findElements(By.tagName("span"));
        Assert.assertEquals("Invalid amount of hash change events",
                numberOfEvents, spans.size());
    }

    private void verifyInsideServletLocation(String pathAfterServletMapping) {
        Assert.assertEquals("Invalid URL",
                getRootURL() + "/view/" + pathAfterServletMapping,
                getDriver().getCurrentUrl());
    }

}
