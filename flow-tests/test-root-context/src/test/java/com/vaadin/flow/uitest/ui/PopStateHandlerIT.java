package com.vaadin.flow.uitest.ui;

import java.util.concurrent.atomic.AtomicInteger;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.router.internal.PathUtil;
import com.vaadin.flow.testutil.ChromeBrowserTest;

@NotThreadSafe
public class PopStateHandlerIT extends ChromeBrowserTest {

    private static final String FORUM = "com.vaadin.flow.uitest.ui.PopStateHandlerUI/forum/";
    private static final String FORUM_SUBCATEGORY = "com.vaadin.flow.uitest.ui.PopStateHandlerUI/forum/#!/category/1";
    private static final String FORUM_SUBCATEGORY2 = "com.vaadin.flow.uitest.ui.PopStateHandlerUI/forum/#!/category/2";
    private static final String ANOTHER_PATH = "com.vaadin.flow.uitest.ui.PopStateHandlerUI/another/";

    @Test
    public void testDifferentPath_ServerSideEvent() {
        open();
        verifyNoServerVisit();
        verifyInsideServletLocation(getViewClass().getName());

        pushState(FORUM);

        verifyInsideServletLocation(FORUM);

        pushState(ANOTHER_PATH);

        verifyInsideServletLocation(ANOTHER_PATH);

        goBack();
    }

    @Test
    public void testDifferentPath_doubleBack_ServerSideEvent() {
        open();

        pushState(FORUM);
        pushState(ANOTHER_PATH);

        goBack();

        verifyPopStateEvent(FORUM);
        verifyInsideServletLocation(FORUM);

        goBack();

        verifyPopStateEvent(getViewClass().getName());
        verifyInsideServletLocation(getViewClass().getName());
    }

    @Test
    public void testSamePathHashChanges_noServerSideEvent() {
        open();
        verifyNoServerVisit();
        verifyInsideServletLocation(getViewClass().getName());

        pushState(FORUM);

        verifyInsideServletLocation(FORUM);

        pushState(FORUM_SUBCATEGORY);

        verifyInsideServletLocation(FORUM_SUBCATEGORY);

        pushState(FORUM_SUBCATEGORY2);

        verifyInsideServletLocation(FORUM_SUBCATEGORY2);

        goBack();
    }

    @Test
    public void testSamePathHashChanges_tripleeBack_noServerSideEvent() {
        open();

        pushState(FORUM);

        pushState(FORUM_SUBCATEGORY);

        pushState(FORUM_SUBCATEGORY2);

        goBack();

        verifyInsideServletLocation(FORUM_SUBCATEGORY);

        goBack();

        verifyInsideServletLocation(FORUM);

        goBack();

        verifyPopStateEvent(getViewClass().getName());
        verifyInsideServletLocation(getViewClass().getName());
    }

    private void goBack() {
        // executeScript("window.history.back()");
        driver.navigate().back();
    }

    private void pushState(String id) {
        findElement(By.id(id)).click();
    }

    private String trimPathForClientRouter(String path) {
        // NOTE: see https://github.com/vaadin/flow/issues/10865
        return isClientRouter() ? PathUtil.trimPath(path) : path;
    }

    private final AtomicInteger counter = new AtomicInteger();

    private void verifyInsideServletLocation(String pathAfterServletMapping) {
        int idx = counter.incrementAndGet();
        waitUntil(driver -> {
            String expected = trimPathForClientRouter(
                    getRootURL() + "/view/" + pathAfterServletMapping);
            String actual = trimPathForClientRouter(driver.getCurrentUrl());
            System.out.println("================ PopStateHandlerIT " + idx
                    + " :: ACT: " + actual + ", EXP: " + expected);
            return expected.equals(actual);
        });
    }

    @After
    public void dumpLogs() {
        int idx = counter.get();
        getLogEntries(java.util.logging.Level.ALL).stream()
                .map(le -> idx + " " + le.toString())
                .forEach(System.out::println);
    }

    private void verifyNoServerVisit() {
        verifyPopStateEvent("no location");
    }

    private void verifyPopStateEvent(String location) {
        Assert.assertEquals("Invalid server side event location",
                trimPathForClientRouter(
                        findElement(By.id("location")).getText()),
                trimPathForClientRouter(location));
    }
}
