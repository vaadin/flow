/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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

    @Test
    public void nullText_transformsToEmptyAndDoesNotThrowException() {
        Assert.assertEquals("", new Text(null).getText());
    }

    @Test
    public void emptyText() {
        Assert.assertEquals("", new Text("").getText());
    }

    @Test
    public void setText_emptyTextCanBeChangedLater() {
        Text text = new Text(null);
        text.setText("Non Empty");
        Assert.assertEquals("Non Empty", text.getText());
    }

    @Test
    public void setText_nullIsChangedToEmptyAndDoesNotThrowException() {
        Text text = new Text("Default");
        text.setText(null);
        Assert.assertEquals("", text.getText());
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
