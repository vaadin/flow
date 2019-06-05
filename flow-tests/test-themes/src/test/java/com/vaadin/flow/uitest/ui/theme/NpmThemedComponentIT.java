/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.uitest.ui.theme;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

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
