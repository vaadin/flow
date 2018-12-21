package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

public class IFrameIT extends ChromeBrowserTest {

    @Test
    public void testIFrame() {
        open();

        List<WebElement> iframes = findElements(By.tagName("iframe"));

        Assert.assertEquals(3, iframes.size());
        Assert.assertTrue(iframes.get(0).getAttribute("src").contains("/view/com.vaadin.flow.uitest.ui.PopupView"));
        Assert.assertTrue(iframes.get(1).getAttribute("src").contains("/view/com.vaadin.flow.uitest.ui.IFrameView"));
        Assert.assertTrue(iframes.get(2).getAttribute("src").contains("/view/com.vaadin.flow.uitest.ui.PopupView"));
    }

}
