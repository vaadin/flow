package com.vaadin.flow;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class FlowInReactComponentIT extends ChromeBrowserTest {

    @Test
    public void validateComponentPlacesAndFunction() {
        open();

        waitForDevServer();

        Assert.assertTrue("No react component displayed",
                $("react-layout").first().isDisplayed());

        List<WebElement> list = $("react-layout").first()
                .findElements(By.xpath("./child::*"));
        Assert.assertEquals("React component child count wrong", list.size(),
                4);

        Assert.assertEquals("span", list.get(0).getTagName());
        Assert.assertEquals("flow-content-container", list.get(1).getTagName());
        Assert.assertEquals("div", list.get(2).getTagName());
        Assert.assertEquals("span", list.get(3).getTagName());

        TestBenchElement content = $("react-layout").first()
                .findElement(By.name("content"));

        list = content.findElements(By.xpath("./child::*"));
        Assert.assertEquals("Flow content container count wrong", list.size(),
                3);

        $(NativeButtonElement.class).id("add").click();
        Assert.assertEquals(1, content.$(DivElement.class).all().size());

        list = $("react-layout").first().findElements(By.xpath("./child::*"));
        Assert.assertEquals(
                "Adding flow component should not add to main react component",
                list.size(), 4);

        list = content.findElements(By.xpath("./child::*"));
        Assert.assertEquals("Flow content container count wrong", list.size(),
                4);

        $(NativeButtonElement.class).id("add").click();
        Assert.assertEquals(2, content.$(DivElement.class).all().size());
        $(NativeButtonElement.class).id("add").click();
        Assert.assertEquals(3, content.$(DivElement.class).all().size());

        $(NativeButtonElement.class).id("remove").click();
        Assert.assertEquals(2, content.$(DivElement.class).all().size());
        $(NativeButtonElement.class).id("remove").click();
        Assert.assertEquals(1, content.$(DivElement.class).all().size());
        $(NativeButtonElement.class).id("remove").click();
        Assert.assertEquals(0, content.$(DivElement.class).all().size());

    }

}
