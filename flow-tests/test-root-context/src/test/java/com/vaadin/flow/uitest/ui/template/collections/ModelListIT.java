package com.vaadin.flow.uitest.ui.template.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.hamcrest.CoreMatchers;
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
        WebElement repeat3 = findInShadowRoot(modelList, By.id("repeat-3"))
                .get(0);
        WebElement repeat4 = findInShadowRoot(modelList, By.id("repeat-4"))
                .get(0);

        assertClickedStates();
        repeat1.findElements(By.tagName("div")).get(0).click();
        assertClickedStates(0);
        repeat2.findElements(By.tagName("div")).get(0).click();
        assertClickedStates(0, 2);
        repeat3.findElements(By.tagName("div")).get(0).click();
        assertClickedStates(0, 2, 4);
        repeat3.findElements(By.tagName("div")).get(2).click();
        assertClickedStates(0, 2, 4, 6);
        repeat4.findElements(By.tagName("div")).get(0).click();
        assertClickedStates(0, 2, 4, 6, 8);
        findInShadowRoot(modelList, By.id("item-with-item-div")).get(0).click();
        assertClickedStates(0, 2, 4, 6, 8, 10);
    }

    @Test
    public void clickOnAddedItems_itemsAreUpdated() {
        WebElement repeat1 = findInShadowRoot(modelList, By.id("repeat-1"))
                .get(0);
        WebElement repeat2 = findInShadowRoot(modelList, By.id("repeat-2"))
                .get(0);
        WebElement repeat3 = findInShadowRoot(modelList, By.id("repeat-3"))
                .get(0);
        WebElement repeat4 = findInShadowRoot(modelList, By.id("repeat-4"))
                .get(0);

        assertClickedStates();
        repeat1.findElements(By.tagName("div")).get(1).click();
        assertClickedStates(1);
        repeat2.findElements(By.tagName("div")).get(1).click();
        assertClickedStates(1, 3);
        repeat3.findElements(By.tagName("div")).get(1).click();
        assertClickedStates(1, 3, 5);
        repeat3.findElements(By.tagName("div")).get(3).click();
        assertClickedStates(1, 3, 5, 7);
        repeat4.findElements(By.tagName("div")).get(1).click();
        assertClickedStates(1, 3, 5, 7, 9);
    }

    @Test
    public void setNullValues_itemsAreUpdated() {
        WebElement repeat1 = findInShadowRoot(modelList, By.id("repeat-1"))
                .get(0);
        WebElement repeat2 = findInShadowRoot(modelList, By.id("repeat-2"))
                .get(0);
        WebElement repeat3 = findInShadowRoot(modelList, By.id("repeat-3"))
                .get(0);
        WebElement repeat4 = findInShadowRoot(modelList, By.id("repeat-4"))
                .get(0);

        assertClickedStates();
        findInShadowRoot(modelList, By.id("set-null")).get(0).click();

        List<WebElement> repeated1 = repeat1.findElements(By.tagName("div"));
        List<WebElement> repeated2 = repeat2.findElements(By.tagName("div"));
        List<WebElement> repeated3 = repeat3.findElements(By.tagName("div"));
        List<WebElement> repeated4 = repeat4.findElements(By.tagName("div"));

        Assert.assertEquals("false", repeated1.get(0).getText());
        Assert.assertEquals("false", repeated1.get(1).getText());
        Assert.assertEquals("false", repeated2.get(0).getText());
        Assert.assertEquals("false", repeated2.get(1).getText());
        Assert.assertEquals("false", repeated3.get(0).getText());
        Assert.assertEquals("false", repeated3.get(1).getText());
        Assert.assertEquals("false", repeated4.get(0).getText());
        Assert.assertEquals("false", repeated4.get(1).getText());
    }

    private void assertClickedStates(int... clicked) {

        WebElement repeat1 = findInShadowRoot(modelList, By.id("repeat-1"))
                .get(0);
        WebElement repeat2 = findInShadowRoot(modelList, By.id("repeat-2"))
                .get(0);
        WebElement repeat3 = findInShadowRoot(modelList, By.id("repeat-3"))
                .get(0);
        WebElement repeat4 = findInShadowRoot(modelList, By.id("repeat-4"))
                .get(0);

        List<WebElement> divs = new ArrayList<>();
        divs.addAll(repeat1.findElements(By.tagName("div")));
        divs.addAll(repeat2.findElements(By.tagName("div")));
        divs.addAll(repeat3.findElements(By.tagName("div")));
        divs.addAll(repeat4.findElements(By.tagName("div")));
        divs.addAll(findInShadowRoot(modelList, By.id("item-with-item-div")));

        for (int i = 0; i < divs.size(); i++) {
            int index = i;
            boolean clickedState = IntStream.of(clicked)
                    .anyMatch(x -> x == index);
            Assert.assertThat(divs.get(index).getText(),
                    CoreMatchers.startsWith(String.valueOf(clickedState)));
        }
    }
}
