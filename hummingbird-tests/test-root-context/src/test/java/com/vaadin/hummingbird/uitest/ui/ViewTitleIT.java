package com.vaadin.hummingbird.uitest.ui;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.testutil.PhantomJSTest;
import com.vaadin.testbench.By;

public class ViewTitleIT extends PhantomJSTest {

    @Override
    protected void open() {
        getDriver().get(getRootURL() + "/view/");
    }

    @Test
    public void testNoViewTitle() {
        open();
        openView("BasicElementView");

        verifyTitle("");
    }

    @Test
    public void testViewTitleAnnotation() {
        open();
        openView("TitleView");

        verifyTitle("Title view");
    }

    @Test
    public void testViewDynamicTitle() {
        open();
        openView("DynamicTitleView");

        verifyTitle("dynamic title view");
    }

    private void openView(String viewName) {
        findElement(By.tagName("select")).click();
        findElement(By.id(viewName)).click();
    }

    private void verifyTitle(String title) {
        Assert.assertEquals("Page title does not match", title,
                getDriver().getTitle());
    }

}
