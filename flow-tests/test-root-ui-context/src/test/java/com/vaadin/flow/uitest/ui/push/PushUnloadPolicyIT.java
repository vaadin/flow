package com.vaadin.flow.uitest.ui.push;

import java.util.Collections;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.logging.LogEntry;

import com.vaadin.flow.testcategory.PushTests;
import com.vaadin.flow.testutil.ChromeBrowserTest;

@Category(PushTests.class)
public class PushUnloadPolicyIT extends ChromeBrowserTest {

    @Test
    public void unloadBlocked_pushClientLoaded_noPermissionsPolicyViolation() {
        open();

        waitUntil(driver -> (Boolean) executeScript(
                "return !!(window.vaadinPush && window.vaadinPush.atmosphere)"));

        Assert.assertEquals(
                "The push client should not register an unload listener",
                Collections.emptyList(),
                getLogEntries(java.util.logging.Level.ALL).stream()
                        .map(LogEntry::getMessage)
                        .filter(message -> message
                                .contains("Permissions policy violation"))
                        .collect(Collectors.toList()));
    }
}
