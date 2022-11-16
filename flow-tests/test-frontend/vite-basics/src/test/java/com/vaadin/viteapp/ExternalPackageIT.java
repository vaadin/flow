package com.vaadin.viteapp;

import org.junit.jupiter.api.Assertions;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.ParagraphElement;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.viteapp.views.empty.MainView;

public class ExternalPackageIT extends ViteDevModeIT {

    @BrowserTest
    public void packageOutsideNpmWorks() {
        $(NativeButtonElement.class).id(MainView.OUTSIDE).click();
        Assertions.assertEquals("It works - It works", $(ParagraphElement.class)
                .id(MainView.OUTSIDE_RESULT).getText());
    }
}
