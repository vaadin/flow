package com.vaadin.flow.uitest.ui.template.collections;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ModelListIT extends ChromeBrowserTest {

    private WebElement modelList;

    @Before
    public void init() {
        open();
        modelList = findElement(By.tagName("model-list"));
    }

    @Test
    public void clickOnOldItems_itemsAreUpdated() {
        WebElement repeat1 = findInShadowRoot(modelList, By.id("repeat-1"))
                .get(0);
        WebElement repeat2 = findInShadowRoot(modelList, By.id("repeat-2"))
                .get(0);

        assertClickableStates(false, false, false, false);
        repeat1.findElements(By.tagName("div")).get(0).click();
        assertClickableStates(true, false, false, false);
        repeat2.findElements(By.tagName("div")).get(0).click();
        assertClickableStates(true, false, true, false);
    }

    @Test
    public void clickOnAddedItems_itemsAreUpdated() {
        WebElement repeat1 = findInShadowRoot(modelList, By.id("repeat-1"))
                .get(0);
        WebElement repeat2 = findInShadowRoot(modelList, By.id("repeat-2"))
                .get(0);

        assertClickableStates(false, false, false, false);
        repeat1.findElements(By.tagName("div")).get(1).click();
        assertClickableStates(false, true, false, false);
        repeat2.findElements(By.tagName("div")).get(1).click();
        assertClickableStates(false, true, false, true);
    }

    @Test
    public void setNullValues_itemsAreUpdated() {
        WebElement repeat1 = findInShadowRoot(modelList, By.id("repeat-1"))
                .get(0);
        WebElement repeat2 = findInShadowRoot(modelList, By.id("repeat-2"))
                .get(0);

        assertClickableStates(false, false, false, false);
        findInShadowRoot(modelList, By.id("set-null")).get(0).click();

        List<WebElement> repeated1 = repeat1.findElements(By.tagName("div"));
        List<WebElement> repeated2 = repeat2.findElements(By.tagName("div"));

        Assert.assertEquals("false", repeated1.get(0).getText());
        Assert.assertEquals("false", repeated1.get(1).getText());
        Assert.assertEquals("false", repeated2.get(0).getText());
        Assert.assertEquals("false", repeated2.get(1).getText());
    }

    private void assertClickableStates(boolean state1, boolean state2,
            boolean state3, boolean state4) {
        WebElement repeat1 = findInShadowRoot(modelList, By.id("repeat-1"))
                .get(0);
        WebElement repeat2 = findInShadowRoot(modelList, By.id("repeat-2"))
                .get(0);

        List<WebElement> repeated1 = repeat1.findElements(By.tagName("div"));
        List<WebElement> repeated2 = repeat2.findElements(By.tagName("div"));

        Assert.assertEquals(state1 + " Item 1", repeated1.get(0).getText());
        Assert.assertEquals(state2 + " New item 1", repeated1.get(1).getText());
        Assert.assertEquals(state3 + " Item 2", repeated2.get(0).getText());
        Assert.assertEquals(state4 + " New item 2", repeated2.get(1).getText());
    }

}
