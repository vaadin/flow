package com.vaadin.flow.uitest.ui.template.collections;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.openqa.selenium.By;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
public class ClearListIT extends ChromeBrowserTest {

    @Test
    public void checkThatListCanBeClearedWithModelHavingNoDefaultConstructor() {
        checkThatModelHasNoDefaultConstructor();
        open();

        WebElement template = findElement(By.id("template"));
        List<String> initialMessages = getMessages(template);

        Assert.assertEquals("Initial page does not contain expected messages",
                Arrays.asList("1", "2"), initialMessages);

        getInShadowRoot(template, By.id("clearList")).click();

        Assert.assertTrue("Page should not contain elements after we've cleared them",
                getMessages(template).isEmpty());
    }

    private void checkThatModelHasNoDefaultConstructor() {
        Constructor<?>[] modelConstructors = ClearListView.Message.class.getConstructors();
        Assert.assertEquals("Expect model to have one constructor exactly", 1, modelConstructors.length);
        Assert.assertTrue("Expect model to have at least one parameter in its single constructor",modelConstructors[0].getParameterCount() > 0);
    }

    private List<String> getMessages(WebElement template) {
        return findInShadowRoot(template, By.className("msg")).stream()
                .map(WebElement::getText).collect(Collectors.toList());
    }
}
