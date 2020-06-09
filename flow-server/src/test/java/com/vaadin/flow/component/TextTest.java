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
package com.vaadin.flow.component;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TextTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void elementAttached() {
        // This will throw an assertion error if the element is not attached to
        // the component
        new Text("Foo").getParent();
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullText() {
        new Text(null);
    }

    @Test
    public void emptyText() {
        Assert.assertEquals("", new Text("").getText());
    }

    @Test
    public void setGetText() {
        Assert.assertEquals("Simple", new Text("Simple").getText());
        Assert.assertEquals("åäö €#%°#", new Text("åäö €#%°#").getText());
    }

    @Test
    public void setId_throwsWithMeaningfulMessage() {
        assertExceptionOnSetProperty("id");

        new Text("").setId("foo");
    }

    @Test
    public void setFooProperty_throwsWithMeaningfulMessage() {
        assertExceptionOnSetProperty("foo");

        new Text("").set(PropertyDescriptors.propertyWithDefault("foo", true),
                false);
    }

    @Test
    public void setVisibility_throwsWithMeaningfulMessage() {
        exception.expect(UnsupportedOperationException.class);

        exception.expectMessage(CoreMatchers.allOf(
                CoreMatchers.containsString(
                        "Cannot change Text component visibility"),
                CoreMatchers.containsString(
                        "because it doesn't represent an HTML Element")));

        new Text("").setVisible(false);
    }

    private void assertExceptionOnSetProperty(String property) {
        exception.expect(UnsupportedOperationException.class);

        exception.expectMessage(CoreMatchers.allOf(
                CoreMatchers.containsString("Cannot set '" + property + "' "),
                CoreMatchers.containsString(
                        "component because it doesn't represent an HTML Element")));
    }
}
