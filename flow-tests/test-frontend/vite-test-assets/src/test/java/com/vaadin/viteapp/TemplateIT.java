package com.vaadin.viteapp;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.vaadin.example.addon.AddonLitComponent;

import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.jupiter.ChromeBrowserTest;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.viteapp.views.template.LitComponent;
import com.vaadin.viteapp.views.template.PolymerComponent;
import com.vaadin.viteapp.views.template.ReflectivelyReferencedComponent;
import com.vaadin.viteapp.views.template.TemplateView;

public class TemplateIT extends ChromeBrowserTest {
    @BeforeAll
    public static void driver() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    public void openView() {
        getDriver().get(getRootURL() + "/" + TemplateView.ROUTE);
        waitForDevServer();
        getCommandExecutor().waitForVaadin();
    }

    @BrowserTest
    public void testElementIdMapping() {
        final String initialValue = "Default";

        SpanElement litSpan = $(LitComponent.TAG).first().$(SpanElement.class)
                .first();
        Assertions.assertEquals(initialValue, litSpan.getText());

        SpanElement polymerSpan = $(PolymerComponent.TAG).first()
                .$(SpanElement.class).first();
        Assertions.assertEquals(initialValue, polymerSpan.getText());

        SpanElement addonLitSpan = $(AddonLitComponent.TAG).first()
                .$(SpanElement.class).first();
        Assertions.assertEquals(initialValue, addonLitSpan.getText());

        final String newLabel = "New label";
        $(InputTextElement.class).first().setValue(newLabel);
        $(NativeButtonElement.class).first().click();

        Assertions.assertEquals(newLabel, litSpan.getText());
        Assertions.assertEquals(newLabel, polymerSpan.getText());
        Assertions.assertEquals(newLabel, addonLitSpan.getText());
    }

    @BrowserTest
    public void testElementReferencedByReflection() {
        SpanElement span = $(ReflectivelyReferencedComponent.TAG).first()
                .$(SpanElement.class).first();
        Assertions.assertEquals("ReflectivelyReferencedComponent contents",
                span.getText());
    }
}
