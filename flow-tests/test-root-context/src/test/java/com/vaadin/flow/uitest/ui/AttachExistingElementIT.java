/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.LabelElement;
import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testutil.ChromeBrowserTest;

@Category(IgnoreOSGi.class)
public class AttachExistingElementIT extends ChromeBrowserTest {

    @Test
    public void attachExistingElement() {
        open();

        // attach label
        findElement(By.id("attach-label")).click();

        Assert.assertTrue(isElementPresent(By.id("label")));
        WebElement label = findElement(By.id("label"));
        Assert.assertEquals("label",
                label.getTagName().toLowerCase(Locale.ENGLISH));

        WebElement parentDiv = findElement(By.id("root-div"));
        List<WebElement> children = parentDiv
                .findElements(By.xpath("./child::*"));
        boolean labelIsFoundAsChild = false;
        WebElement removeButton = null;
        for (int i = 0; i < children.size(); i++) {
            WebElement child = children.get(i);
            if (child.equals(label)) {
                labelIsFoundAsChild = true;
                WebElement attachButton = children.get(i + 1);
                Assert.assertEquals("The first inserted component after "
                        + "attached label has wrong index on the client side",
                        "attach-populated-label",
                        attachButton.getAttribute("id"));
                removeButton = children.get(i + 2);
                Assert.assertEquals("The second inserted component after "
                        + "attached label has wrong index on the client side",
                        "remove-self", removeButton.getAttribute("id"));
                break;
            }
        }

        Assert.assertTrue(
                "The attached label is not found as a child of its parent",
                labelIsFoundAsChild);

        removeButton.click();
        Assert.assertFalse(isElementPresent(By.id("remove-self")));

        // attach existing server-side element
        findElement(By.id("attach-populated-label")).click();
        Assert.assertEquals("already-populated", label.getAttribute("class"));

        // attach header element
        findElement(By.id("attach-header")).click();

        Assert.assertTrue(isElementPresent(By.id("header")));
        Assert.assertEquals("h1", findElement(By.id("header")).getTagName()
                .toLowerCase(Locale.ENGLISH));

        // attach a child in the shadow root of the div

        findElement(By.id("attach-label-inshadow")).click();
        LabelElement labelInShadow = $(DivElement.class)
                .id("element-with-shadow").$(LabelElement.class)
                .id("label-in-shadow");
        Assert.assertEquals("label",
                labelInShadow.getTagName().toLowerCase(Locale.ENGLISH));

        // Try to attach non-existing element
        findElement(By.id("non-existing-element")).click();
        Assert.assertTrue(isElementPresent(By.id("non-existing-element")));
    }
}
