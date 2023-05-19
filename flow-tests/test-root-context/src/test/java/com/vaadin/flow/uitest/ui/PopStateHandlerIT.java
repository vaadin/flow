package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.router.internal.PathUtil;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PopStateHandlerIT extends ChromeBrowserTest {

    private static final String FORUM = "com.vaadin.flow.uitest.ui.PopStateHandlerUI/forum/";
    private static final String FORUM_SUBCATEGORY = "com.vaadin.flow.uitest.ui.PopStateHandlerUI/forum/#!/category/1";
    private static final String FORUM_SUBCATEGORY2 = "com.vaadin.flow.uitest.ui.PopStateHandlerUI/forum/#!/category/2";
    private static final String ANOTHER_PATH = "com.vaadin.flow.uitest.ui.PopStateHandlerUI/another/";
    private static final String EMPTY_HASH = "com.vaadin.flow.uitest.ui.PopStateHandlerUI/forum/#";

    @Test
    public void testDifferentPath_ServerSideEvent() {
        open();
        verifyNoServerVisit();
        verifyInsideServletLocation(getViewClass().getName());

        pushState(FORUM);

        verifyInsideServletLocation(FORUM);
        verifyNoServerVisit();

        pushState(ANOTHER_PATH);

        verifyInsideServletLocation(ANOTHER_PATH);
        verifyNoServerVisit();

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
        verifyNoServerVisit();

        pushState(FORUM_SUBCATEGORY);

        verifyInsideServletLocation(FORUM_SUBCATEGORY);
        verifyNoServerVisit();

        pushState(FORUM_SUBCATEGORY2);

        verifyInsideServletLocation(FORUM_SUBCATEGORY2);
        verifyNoServerVisit();

        goBack();
    }

    @Test
    @Ignore("Ignored because of the issue https://github.com/vaadin/flow/issues/10825")
    public void testSamePathHashChanges_tripleeBack_noServerSideEvent() {
        open();

        pushState(FORUM);

        pushState(FORUM_SUBCATEGORY);

        pushState(FORUM_SUBCATEGORY2);

        goBack();

        verifyNoServerVisit();
        verifyInsideServletLocation(FORUM_SUBCATEGORY);

        goBack();

        verifyNoServerVisit();
        verifyInsideServletLocation(FORUM);

        goBack();

        verifyPopStateEvent(getViewClass().getName());
        verifyInsideServletLocation(getViewClass().getName());
    }

    @Test
    public void testEmptyHash_noHashServerToServer() {
        open();
        verifyNoServerVisit();
        verifyInsideServletLocation(getViewClass().getName());

        pushState(EMPTY_HASH);

        verifyInsideServletLocation(EMPTY_HASH);
        verifyNoServerVisit();

        pushState(FORUM);

        verifyInsideServletLocation(FORUM);
        verifyNoServerVisit();

        pushState(EMPTY_HASH);

        verifyInsideServletLocation(EMPTY_HASH);
        verifyNoServerVisit();

        pushState(ANOTHER_PATH);

        verifyInsideServletLocation(ANOTHER_PATH);
        verifyNoServerVisit();

        goBack();
    }

    @Test
    public void testEmptyHash_quadrupleBack_noHashServerToServer() {
        open();

        pushState(EMPTY_HASH);

        pushState(FORUM);

        pushState(EMPTY_HASH);

        pushState(ANOTHER_PATH);

        goBack();

        verifyPopStateEvent(FORUM);
        // NOTE: see https://github.com/vaadin/flow/issues/10865
        verifyInsideServletLocation(isClientRouter() ? FORUM : EMPTY_HASH);

        goBack();

        verifyPopStateEvent(FORUM);
        verifyInsideServletLocation(FORUM);

        goBack();

        verifyPopStateEvent(FORUM);
        // NOTE: see https://github.com/vaadin/flow/issues/10865
        verifyInsideServletLocation(isClientRouter() ? FORUM : EMPTY_HASH);

        goBack();

        verifyPopStateEvent(getViewClass().getName());
        verifyInsideServletLocation(getViewClass().getName());
    }

    private void goBack() {
        executeScript("window.history.back()");
    }

    private void pushState(String id) {
        findElement(By.id(id)).click();
    }

    private String trimPathForClientRouter(String path) {
        // NOTE: see https://github.com/vaadin/flow/issues/10865
        return isClientRouter() ? PathUtil.trimPath(path) : path;
    }

    private void verifyInsideServletLocation(String pathAfterServletMapping) {
        Assert.assertEquals("Invalid URL",
                trimPathForClientRouter(
                        getRootURL() + "/view/" + pathAfterServletMapping),
                trimPathForClientRouter(getDriver().getCurrentUrl()));
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
