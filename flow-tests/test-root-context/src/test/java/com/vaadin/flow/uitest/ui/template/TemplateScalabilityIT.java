package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class TemplateScalabilityIT extends ChromeBrowserTest {

    @Test
    public void openPage_allButtonsRenderSuccessfully() {
        open();


        Assert.assertEquals("Template should have created 50 native buttons.",
                50, $(NativeButtonElement.class).all().size());

        Assert.assertTrue("The 'completed' div should be on the page.",
                $(DivElement.class).id("completed").isDisplayed());
    }
}
