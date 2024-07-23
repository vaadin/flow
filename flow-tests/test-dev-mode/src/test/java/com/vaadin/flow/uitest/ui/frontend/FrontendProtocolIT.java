/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.frontend;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

/**
 * A test that ensures the correct behavior of the "frontend://" protocol under
 * 3 scenarios:
 * <ol>
 * <li>Loading a component in debug mode (not production mode)</li>
 * <li>Loading a component in production mode (assuming an ES6 capable
 * browser)</li>
 * <li>Loading a component from a custom URL defined by the user</li>
 * </ol>
 * There are three variants of the frontend-protocol.html file in the project,
 * each one with a different text inside it. This test reads the texts to make
 * sure the right file is loaded under each scenario.
 *
 * @see FrontendProtocolView
 * @see FrontendProtocolTemplate
 *
 */
public class FrontendProtocolIT extends ChromeBrowserTest {

    @Test
    @Ignore // https://github.com/vaadin/flow/issues/8063
    public void loadComponentFromEs6Path() {
        openProduction();

        // will access the frontend-protocol.html file at
        // src/main/webapp/VAADIN/static/frontend/es6/components
        Assert.assertEquals("File loaded from ES6 path",
                getComponentInnerText());

        Assert.assertEquals(
                getRootURL()
                        + "/frontend-es6/components/frontend-protocol.html",
                executeClientSideResolveUri());
    }

    @Test
    @Ignore
    public void loadComponentFromContext() {
        open();

        // will access the frontend-protocol.html file at
        // src/main/webapp/components
        Assert.assertEquals("File loaded from context path",
                getComponentInnerText());

        Assert.assertEquals(
                getRootURL() + "/frontend/components/frontend-protocol.html",
                executeClientSideResolveUri());
    }

    @Test
    @Ignore // https://github.com/vaadin/flow/issues/8063
    public void loadComponentFromEs6Property() {
        openForEs6Url();

        // will access the frontend-protocol.html file at
        // src/main/webapp/com/vaadin/flow/uitest/components
        Assert.assertEquals("File loaded from property-defined path",
                getComponentInnerText());

        Assert.assertEquals(getRootURL()
                + "/frontend/com/vaadin/flow/uitest/components/frontend-protocol.html",
                executeClientSideResolveUri());
    }

    private Object executeClientSideResolveUri() {
        return executeScript(
                "return window.Vaadin.Flow.clients[window.Vaadin.Flow.getAppIds()[0].replace(/-\\d+$/, '')].resolveUri(arguments[0]);",
                "frontend://components/frontend-protocol.html");
    }

    private String getComponentInnerText() {
        waitForElementPresent(By.tagName("frontend-protocol"));

        TestBenchElement element = $("frontend-protocol").first();
        DivElement innerList = element.$(DivElement.class)
                .id("frontend-protocol-div");

        return innerList.getText();
    }

}
