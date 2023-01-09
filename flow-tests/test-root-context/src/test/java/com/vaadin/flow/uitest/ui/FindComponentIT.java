package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.Keys;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class FindComponentIT extends ChromeBrowserTest {

    @Test
    public void componentsFound() {
        open();

        DivElement div1 = $(DivElement.class).id("div1");
        DivElement div2 = $(DivElement.class).id("div2");
        NativeButtonElement check = $(NativeButtonElement.class).id("check");
        InputTextElement nodeIdInput = $(InputTextElement.class).id("nodeId");
        TestBenchElement result = $("*").id("result");
        nodeIdInput.sendKeys(getNodeId(div1) + "" + Keys.ENTER);
        check.click();
        Assert.assertEquals("Found component with id div1", result.getText());

        nodeIdInput.clear();
        nodeIdInput.sendKeys(getNodeId(div2) + "" + Keys.ENTER);
        check.click();
        Assert.assertEquals("Found component with id div2", result.getText());
    }

    private long getNodeId(TestBenchElement element) {
        return (long) executeScript(
                "return window.Vaadin.Flow.clients[arguments[0]].getNodeId(arguments[1])",
                "view", element);
    }

}
