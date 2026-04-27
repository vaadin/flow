/*
 * Copyright 2000-2026 Vaadin Ltd.
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
        // data-expected="line_column" encodes the expected
        // __debugSourceDefine on the component function that produced
        // this element (i.e. the position of the function body start).
        List<TestBenchElement> elements = $("*").hasAttribute("data-expected")
                .all();
        Assert.assertTrue(elements.size() > 5);
        for (TestBenchElement element : elements) {
            String expected = element.getAttribute("data-expected");
            Long line = Long.parseLong(expected.split("_")[0]);
            Long column = Long.parseLong(expected.split("_")[1]);
            String filenameEnd = expectedFilenameEnd();

            Map<String, Object> result = (Map<String, Object>) executeScript(
                    """
                            const key = Object.keys(arguments[0]).filter(a => a.startsWith("__reactFiber"))[0];
                            const fiber = arguments[0][key];
                            return fiber.return.type.__debugSourceDefine;
                            """,
                    element);

            Assert.assertNotNull("__debugSourceDefine not set on component for "
                    + element.getAttribute("data-expected"), result);
            Assert.assertTrue(
                    result.get("fileName").toString().endsWith(filenameEnd));
            Assert.assertSame(line, result.get("lineNumber"));
            Assert.assertSame(column, result.get("columnNumber"));
        }
    }

    @Test
    public void jsxSourceLocationsAvailable() {
        // data-jsx-expected="line_column" encodes the expected
        // _debugInfo.source on the JSX element itself (i.e. the position
        // of the `<` opening the element in the source). This is populated
        // from the __source argument that the JSX dev transform embeds in
        // _jsxDEV(...) calls, based on the original source AST. The test
        // guards against regressions where the JSX transform runs on code
        // whose line/column positions no longer match the original source
        // (e.g. after a prior Babel pass has reformatted the file).
        List<TestBenchElement> elements = $("*")
                .hasAttribute("data-jsx-expected").all();
        Assert.assertTrue(elements.size() > 5);
        for (TestBenchElement element : elements) {
            String expected = element.getAttribute("data-jsx-expected");
            Long line = Long.parseLong(expected.split("_")[0]);
            Long column = Long.parseLong(expected.split("_")[1]);
            String filenameEnd = expectedFilenameEnd();

            Map<String, Object> result = (Map<String, Object>) executeScript(
                    """
                            const key = Object.keys(arguments[0]).filter(a => a.startsWith("__reactFiber"))[0];
                            const fiber = arguments[0][key];
                            return fiber._debugInfo && fiber._debugInfo.source;
                            """,
                    element);

            Assert.assertNotNull(
                    "_debugInfo.source not set on JSX element for "
                            + element.getAttribute("data-jsx-expected"),
                    result);
            Assert.assertTrue(
                    result.get("fileName").toString().endsWith(filenameEnd));
            Assert.assertSame(line, result.get("lineNumber"));
            Assert.assertSame(column, result.get("columnNumber"));
        }
    }

    private static String expectedFilenameEnd() {
        String filenameEnd = "vite-basics/src/main/frontend/ReactComponents.tsx";
        if (FrontendUtils.isWindows()) {
            filenameEnd = filenameEnd.replaceAll("/", "\\\\");
        }
        return filenameEnd;
    }
}
