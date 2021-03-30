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
