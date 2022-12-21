package com.vaadin.viteapp;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.ParagraphElement;
import com.vaadin.flow.testutil.jupiter.ChromeBrowserTest;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.viteapp.views.empty.MainView;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

public class ExternalPackageIT extends ChromeBrowserTest {

    @BeforeAll
    public static void driver() {
        WebDriverManager.chromedriver().setup();
    }

    @BrowserTest
    public void packageOutsideNpmWorks() {
        getDriver().get(getRootURL());
        waitForDevServer();
        $(NativeButtonElement.class).id(MainView.OUTSIDE).click();
        Assertions.assertEquals("It works - It works", $(ParagraphElement.class)
                .id(MainView.OUTSIDE_RESULT).getText());
    }
}
