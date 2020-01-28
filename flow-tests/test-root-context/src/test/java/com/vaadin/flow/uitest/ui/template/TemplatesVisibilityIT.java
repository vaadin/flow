/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class TemplatesVisibilityIT extends ChromeBrowserTest {

    @Test
    public void grandParentVisibility_descendantsAreBound() {
        open();

        TestBenchElement grandParent = $("js-grand-parent").first();
        TestBenchElement subTemplate = grandParent.$("js-sub-template").first();

        // Check child and grand child property values. They shouldn't be set
        // since the elements are not bound
        TestBenchElement subTemplateProp = subTemplate.$(TestBenchElement.class)
                .id("prop");

        TestBenchElement grandChild = subTemplate.$(TestBenchElement.class)
                .id("js-grand-child");

        WebElement grandChildFooProp = grandChild.$(TestBenchElement.class)
                .id("foo-prop");

        WebElement grandChildProp = assertInitialPropertyValues(subTemplateProp,
                grandChild, grandChildFooProp);

        // make parent visible
        findElement(By.id("grand-parent-visibility")).click();

        // now all descendants should be bound and JS is executed for the
        // grandchild

        assertBound(subTemplateProp, grandChildFooProp, grandChildProp);
    }

    @Test
    public void subTemplateVisibility_grandChildIsBound() {
        open();

        TestBenchElement grandParent = $("js-grand-parent").first();
        TestBenchElement subTemplate = grandParent.$("js-sub-template").first();

        // make sub template invisible
        findElement(By.id("sub-template-visibility")).click();

        // nothing has changed: parent is not bound -> descendants are still not
        // bound
        TestBenchElement subTemplateProp = subTemplate.$(TestBenchElement.class)
                .id("prop");
        TestBenchElement grandChild = subTemplate.$(TestBenchElement.class)
                .id("js-grand-child");
        WebElement grandChildFooProp = grandChild.$(TestBenchElement.class)
                .id("foo-prop");
        assertInitialPropertyValues(subTemplateProp, grandChild,
                grandChildFooProp);

        // make parent visible
        findElement(By.id("grand-parent-visibility")).click();

        // sub template is invisible now, again: all properties have no values

        WebElement grandChildProp = assertInitialPropertyValues(subTemplateProp,
                grandChild, grandChildFooProp);

        // make sub template visible
        findElement(By.id("sub-template-visibility")).click();

        // now everything is bound
        assertBound(subTemplateProp, grandChildFooProp, grandChildProp);
    }

    @Test
    public void grandChildVisibility_grandChildIsBound() {
        open();

        TestBenchElement grandParent = $("js-grand-parent").first();
        TestBenchElement subTemplate = grandParent.$("js-sub-template").first();

        // make grand child template invisible
        findElement(By.id("grand-child-visibility")).click();

        // nothing has changed: parent is not bound -> descendants are still not
        // bound
        TestBenchElement subTemplateProp = subTemplate.$(TestBenchElement.class)
                .id("prop");
        TestBenchElement grandChild = subTemplate.$(TestBenchElement.class)
                .id("js-grand-child");
        WebElement grandChildFooProp = grandChild.$(TestBenchElement.class)
                .id("foo-prop");
        assertInitialPropertyValues(subTemplateProp, grandChild,
                grandChildFooProp);

        // make grand parent visible
        findElement(By.id("grand-parent-visibility")).click();

        // grand child template is invisible now, again: all its properties have
        // no values

        WebElement grandChildProp = grandChild.$(TestBenchElement.class)
                .id("prop");
        Assert.assertNotEquals("bar", grandChildFooProp.getText());
        Assert.assertNotEquals("foo", grandChildProp.getText());

        // make grand child template visible
        findElement(By.id("grand-child-visibility")).click();

        // now everything is bound
        assertBound(subTemplateProp, grandChildFooProp, grandChildProp);
    }

    @Test
    public void invisibleComponent_dropClientSideChanges() {
        open();

        // make parent visible
        findElement(By.id("grand-parent-visibility")).click();

        TestBenchElement grandParent = $("js-grand-parent").first();
        TestBenchElement subTemplate = grandParent.$("js-sub-template").first();

        WebElement subTemplateProp = subTemplate.$(TestBenchElement.class)
                .id("prop");

        Assert.assertEquals("bar", subTemplateProp.getText());

        // make sub template invisible
        findElement(By.id("sub-template-visibility")).click();

        // change the sub template property via client side
        findElement(By.id("client-side-update-property")).click();

        // The property value has not changed
        Assert.assertEquals("bar", subTemplateProp.getText());

        // make template visible
        findElement(By.id("sub-template-visibility")).click();

        // One more check : the property value is still the same
        Assert.assertEquals("bar", subTemplateProp.getText());

        // change the sub template property via client side one more time
        // (now the component is visible)
        findElement(By.id("client-side-update-property")).click();

        // Now the property value should be changed
        Assert.assertEquals("baz", subTemplateProp.getText());
    }

    private WebElement assertInitialPropertyValues(
            TestBenchElement subTemplateProp, TestBenchElement grandChild,
            WebElement grandChildFooProp) {
        WebElement grandChildProp = grandChild.$(TestBenchElement.class)
                .id("prop");

        Assert.assertNotEquals("bar", subTemplateProp.getText());

        Assert.assertNotEquals("bar", grandChildFooProp.getText());

        Assert.assertNotEquals("foo", grandChildProp.getText());
        return grandChildProp;
    }

    private void assertBound(WebElement subTemplateProp,
            WebElement grandChildFooProp, WebElement grandChildProp) {
        waitUntil(driver -> "bar".equals(subTemplateProp.getText()));
        // This is the result of JS execution
        waitUntil(driver -> "bar".equals(grandChildFooProp.getText()));
        // This is the value for the grand child received from the server side
        Assert.assertEquals("foo", grandChildProp.getText());
    }
}
