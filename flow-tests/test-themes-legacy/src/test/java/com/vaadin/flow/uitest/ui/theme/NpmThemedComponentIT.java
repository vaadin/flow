/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.theme;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class NpmThemedComponentIT extends ChromeBrowserTest {

    @Test
    public void importedClientSideComponentIsThemed() {
        open();

        TestBenchElement themedComponent = $("npm-themed-component").first();
        TestBenchElement nestedDiv = themedComponent.$("div").first();

        String id = nestedDiv.getAttribute("id");
        // make sure that component which is created from the server side is
        // themed
        Assert.assertEquals("The server side component is not themed", "themed",
                id);

        TestBenchElement nestedClientSideComponent = themedComponent
                .$("client-side-component").first();
        List<TestBenchElement> divsInClientSideComponent = nestedClientSideComponent
                .$("div").all();
        // Fist of all: the client side component is correctly rsolved so it has
        // something inside its shadow root
        Assert.assertTrue(
                "The client side component inside themed component is not resolved",
                divsInClientSideComponent.size() > 0);

        TestBenchElement divInClientSideComponent = divsInClientSideComponent
                .get(0);
        // make sure that the nested client side is themed
        Assert.assertEquals("The server side component is not themed", "themed",
                divInClientSideComponent.getAttribute("id"));
    }

    @Override
    protected String getTestPath() {
        String path = super.getTestPath();
        String view = "view/";
        String result;
        if (path.startsWith("/")) {
            result = path.substring(view.length() + 1);
        }
        result = path.substring(view.length());
        return result;
    }

}
