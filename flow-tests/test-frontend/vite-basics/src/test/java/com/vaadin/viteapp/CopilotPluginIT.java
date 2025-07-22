package com.vaadin.viteapp;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CopilotPluginIT extends ChromeBrowserTest {

    @Before
    public void openView() {
        getDriver().get(getTestURL());
        waitForDevServer();
    }

    @Override
    protected String getTestPath() {
        return "/react-custom-components";
    }

    @Test
    public void expectAvailableFeaturesInjectedIntoWindow() {
        final Set<String> expectedFeaturesSet = new HashSet<>(Set.of("COMPONENT_ANALYZER"));
        List<String> features = (List<String>) executeScript("return window.Vaadin.copilot.VITE_COPILOT_PLUGIN.availableFeatures");
        for (String feature : features) {
            Assert.assertTrue(expectedFeaturesSet.contains(feature));
            expectedFeaturesSet.remove(feature);
        }
        Assert.assertTrue(expectedFeaturesSet.isEmpty());
    }

    @Test
    public void expectUnExpectedRequestsHandled() {
        executeScript("window.META_HOT.send('vaadin-copilot:analyze-component', { });");
        WebElement element = findElement(By.cssSelector("div#response"));
        waitUntil(driver -> StringUtils.isNotEmpty(findElement(By.cssSelector("div#response")).getText()));
        String text = element.getText();
        ObjectNode responseBody = JacksonUtils.readTree(text);
        Assert.assertTrue(responseBody.hasNonNull("error"));
        Assert.assertTrue(responseBody.hasNonNull("errorMessage"));
        Assert.assertTrue(responseBody.get("error").asBoolean());
    }

    @Test
    public void expectAllNodesAreAnalyzedInGivenFilesWhenRequested() {
        String buttonContainingFilePath = getFilePathOfNode(findElement(By.cssSelector("button")));
        executeScript("window.META_HOT.send('vaadin-copilot:analyze-component', { filePaths: [ \"" + buttonContainingFilePath + "\"] });");
        waitUntil(driver -> StringUtils.isNotEmpty(findElement(By.cssSelector("div#response")).getText()));
        String text = findElement(By.cssSelector("div#response")).getText();
        ObjectNode responseBody = JacksonUtils.readTree(text);
        Assert.assertFalse(responseBody.has("error"));
        Assert.assertTrue(responseBody.hasNonNull("body"));
        Assert.assertTrue(responseBody.get("body").isArray());
    }


    private String getFilePathOfNode(WebElement element) {
        return (String) executeScript("""
                const fiberKey = Object.keys(arguments[0]).find(a => a.startsWith('__reactFiber'));
                const fiber = arguments[0][fiberKey];
                const debugSource = fiber._debugSource;
                const fileName = debugSource.fileName;
                return fileName;
                """, element);
    }

}
