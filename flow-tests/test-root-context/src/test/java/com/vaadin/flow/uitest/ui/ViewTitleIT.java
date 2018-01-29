package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ViewTitleIT extends ChromeBrowserTest {

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
        TestBenchElement select = (TestBenchElement) findElement(
                By.tagName("select"));
        new Select(select.getWrappedElement()).selectByVisibleText(viewName);
    }

    private void verifyTitle(String title) {
        Assert.assertEquals("Page title does not match", title,
                getDriver().getTitle());
    }

}
