package com.vaadin.flow.uitest.ui.template.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ModelListIT extends ChromeBrowserTest {

    private TestBenchElement modelList;

    @Before
    public void init() {
        open();
        modelList = $("model-list").first();
    }

    @Test
    public void clickOnOldItems_itemsAreUpdated() {

        DivElement repeat1 = findRepeatByID("repeat-1");
        DivElement repeat2 = findRepeatByID("repeat-2");
        DivElement repeat3 = findRepeatByID("repeat-3");
        DivElement repeat4 = findRepeatByID("repeat-4");

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
        findRepeatByID("item-with-item-div").click();
        assertClickedStates(0, 2, 4, 6, 8, 10);
    }

    @Test
    public void clickOnAddedItems_itemsAreUpdated() {
        DivElement repeat1 = findRepeatByID("repeat-1");
        DivElement repeat2 = findRepeatByID("repeat-2");
        DivElement repeat3 = findRepeatByID("repeat-3");
        DivElement repeat4 = findRepeatByID("repeat-4");

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
        DivElement repeat1 = findRepeatByID("repeat-1");
        DivElement repeat2 = findRepeatByID("repeat-2");
        DivElement repeat3 = findRepeatByID("repeat-3");
        DivElement repeat4 = findRepeatByID("repeat-4");

        assertClickedStates();
        modelList.$(NativeButtonElement.class).id("set-null").click();

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

        DivElement repeat1 = findRepeatByID("repeat-1");
        DivElement repeat2 = findRepeatByID("repeat-2");
        DivElement repeat3 = findRepeatByID("repeat-3");
        DivElement repeat4 = findRepeatByID("repeat-4");

        List<DivElement> divs = new ArrayList<>();
        divs.addAll(repeat1.$(DivElement.class).all());
        divs.addAll(repeat2.$(DivElement.class).all());
        divs.addAll(repeat3.$(DivElement.class).all());
        divs.addAll(repeat4.$(DivElement.class).all());
        divs.add(findRepeatByID("item-with-item-div"));

        for (int i = 0; i < divs.size(); i++) {
            int index = i;
            boolean clickedState = IntStream.of(clicked)
                    .anyMatch(x -> x == index);
            MatcherAssert.assertThat(divs.get(index).getText(),
                    CoreMatchers.startsWith(String.valueOf(clickedState)));
        }
    }

    private DivElement findRepeatByID(String id) {
        return modelList.$(DivElement.class).id(id);
    }

}
