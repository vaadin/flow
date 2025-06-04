package com.vaadin.viteapp;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.server.frontend.FrontendUtils;
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
        List<TestBenchElement> elements = $("*").withAttribute("data-expected")
                .all();
        Assert.assertTrue(elements.size() > 5);
        for (TestBenchElement element : elements) {
            String expected = element.getDomAttribute("data-expected");
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
