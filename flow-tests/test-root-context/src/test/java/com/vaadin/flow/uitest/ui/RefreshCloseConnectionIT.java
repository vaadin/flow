package com.vaadin.flow.uitest.ui;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class RefreshCloseConnectionIT extends ChromeBrowserTest {

    @Test
    public void sessionRefresh() {
        String param = UUID.randomUUID().toString();
        open(param);

        if (hasClientIssue("7587")) {
            return;
        }

        waitUntil(driver -> getLastLog() != null);
        Assert.assertEquals("Init", getLastLog());

        open(param);

        waitUntil(driver -> getLastLog() != null);

        List<WebElement> logs = findElements(By.className("log"));
        Set<String> set = logs.stream().map(element -> element.getText())
                .collect(Collectors.toSet());

        Assert.assertTrue(set.contains("Refresh"));
        Assert.assertTrue(set.contains("Push"));
    }

    private String getLastLog() {
        List<WebElement> logs = findElements(By.className("log"));
        if (logs.isEmpty()) {
            return null;
        }
        return logs.get(logs.size() - 1).getText();
    }
}
