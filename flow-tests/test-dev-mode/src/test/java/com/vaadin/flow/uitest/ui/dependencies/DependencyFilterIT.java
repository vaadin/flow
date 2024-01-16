package com.vaadin.flow.uitest.ui.dependencies;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class DependencyFilterIT extends ChromeBrowserTest {

    @Test
    public void dependenciesLoadedAsExpectedWithFiltering() {
        open();

        waitUntil(input -> !input.findElements(By.className("dependenciesTest"))
                .isEmpty());

        List<String> testMessages = findElements(
                By.className("dependenciesTest")).stream()
                .map(WebElement::getText).collect(Collectors.toList());

        assertThat(
                "4 elements are expected to be added: 1 for filtered dependency, 2 for eager dependencies and 1 for UI 'onAttach' method",
                testMessages, hasSize(4));

        boolean found = testMessages.stream()
                .anyMatch(message -> message.equals("filtered.html"));
        Assert.assertTrue("filtered.html should be in the page", found);

        found = testMessages.stream()
                .anyMatch(message -> message.equals("eager.js"));
        Assert.assertTrue("eager.js should be in the page", found);

        found = testMessages.stream()
                .anyMatch(message -> message.equals("eager.html"));
        Assert.assertTrue("eager.html should be in the page", found);
    }

}
