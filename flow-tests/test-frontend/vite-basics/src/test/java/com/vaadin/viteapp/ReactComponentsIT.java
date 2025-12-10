/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.viteapp;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ReactComponentsIT extends ChromeBrowserTest {

    @Before
    public void openView() {
        getDriver().get(getTestURL());
        waitForDevServer();
    }

    @Override
    protected String getTestPath() {
        return "/react-components";
    }

    @Test
    public void functionLocationsAvailable() {
        List<TestBenchElement> elements = $("*").hasAttribute("data-expected")
                .all();
        Assert.assertTrue(elements.size() > 5);
        for (TestBenchElement element : elements) {
            String expected = element.getAttribute("data-expected");
            Long line = Long.parseLong(expected.split("_")[0]);
            Long column = Long.parseLong(expected.split("_")[1]);
            String filenameEnd = "vite-basics/src/main/frontend/ReactComponents.tsx";
            if (FrontendUtils.isWindows()) {
                filenameEnd = filenameEnd.replaceAll("/", "\\\\");
            }

            Map<String, Object> result = (Map<String, Object>) executeScript(
                    """
                            const key = Object.keys(arguments[0]).filter(a => a.startsWith("__reactFiber"))[0];
                            const fiber = arguments[0][key];
                            return fiber.return.type.__debugSourceDefine;
                            """,
                    element);

            Assert.assertTrue(
                    result.get("fileName").toString().endsWith(filenameEnd));
            Assert.assertSame(line, result.get("lineNumber"));
            Assert.assertSame(column, result.get("columnNumber"));
        }
    }
}
