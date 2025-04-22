package com.vaadin.flow;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class AddQueryParamIT extends ChromeBrowserTest {

    @Test
    public void validateRouterInUse() {
        open();

        waitForDevServer();

        $(NativeButtonElement.class).id(AddQueryParamView.PARAM_BUTTON_ID)
                .click();

        waitForElementPresent(By.id(AddQueryParamView.QUERY_ID));

        Assert.assertEquals(
                $(DivElement.class).id(AddQueryParamView.QUERY_ID).getText(),
                driver.getCurrentUrl());
    }

}
