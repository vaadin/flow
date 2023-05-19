package com.vaadin.flow.uitest.ui.push;

import java.util.List;
import java.util.function.Supplier;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public abstract class AbstractLogTest extends ChromeBrowserTest {

    protected WebElement getLastLog() {
        List<WebElement> logs = findElements(By.className("log"));
        if (logs.isEmpty()) {
            return null;
        }
        return logs.get(logs.size() - 1);
    }

    public static ExpectedCondition<Boolean> textToBePresentInElement(
            Supplier<WebElement> supplier, String text) {

        return new ExpectedCondition<Boolean>() {
            @Override
            public Boolean apply(WebDriver driver) {
                try {
                    WebElement element = supplier.get();
                    if (element == null) {
                        return false;
                    }
                    String elementText = element.getText();
                    return elementText.contains(text);
                } catch (StaleElementReferenceException e) {
                    return null;
                }
            }

            @Override
            public String toString() {
                return String.format("text ('%s') to be present in element %s",
                        text, supplier.get());
            }
        };
    }
}
