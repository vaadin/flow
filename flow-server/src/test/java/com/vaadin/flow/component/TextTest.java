/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.component;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Text;

public class TextTest {

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
}
