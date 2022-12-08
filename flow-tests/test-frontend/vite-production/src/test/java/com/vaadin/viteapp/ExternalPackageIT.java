package com.vaadin.viteapp;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.ParagraphElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.viteapp.views.empty.MainView;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ExternalPackageIT extends ChromeBrowserTest {

    @BeforeClass
    public static void driver() {
        WebDriverManager.chromedriver().setup();
    }

    @Test
    public void packageOutsideNpmWorks() {
        getDriver().get(getRootURL());
        waitForDevServer();
        $(NativeButtonElement.class).id(MainView.OUTSIDE).click();
        Assert.assertEquals("It works - It works", $(ParagraphElement.class)
                .id(MainView.OUTSIDE_RESULT).getText());
    }
}
