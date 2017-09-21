/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.data;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.dom.Element;
import com.vaadin.ui.Text;
import com.vaadin.ui.button.Button;
import com.vaadin.ui.icon.Icon;

public class ButtonTest {

    private Button button;

    @Test
    public void emptyCtor() {
        button = new Button();
        Assert.assertEquals("", button.getText());
        Assert.assertNull(button.getIcon());
    }

    @Test
    public void textCtor() {
        button = new Button("foo");
        Assert.assertEquals("foo", button.getText());
        Assert.assertNull(button.getIcon());
    }

    @Test
    public void iconCtor() {
        Icon icon = new Icon();
        button = new Button(icon);
        Assert.assertEquals("", button.getText());
        Assert.assertEquals(icon, button.getIcon());
    }

    @Test
    public void textAndIconCtor() {
        Icon icon = new Icon();
        button = new Button("foo", icon);
        Assert.assertEquals("foo", button.getText());
        Assert.assertEquals(icon, button.getIcon());
    }

    @Test
    public void iconAndTextPosition() {
        Icon icon = new Icon();
        button = new Button("foo", icon);
        Assert.assertEquals(icon.getElement(), getButtonChild(0));
        Assert.assertEquals("foo", getButtonChild(1).getText());

        button.setIconAfterText(true);
        Assert.assertEquals("foo", getButtonChild(0).getText());
        Assert.assertEquals(icon.getElement(), getButtonChild(1));
    }

    @Test
    public void setIcon() {
        button = new Button("foo", new Icon());
        Icon icon = new Icon();
        button.setIcon(icon);
        Assert.assertEquals(icon.getElement(), getButtonChild(0));
    }

    @Test
    public void setText() {
        button = new Button("foo", new Icon());
        button.add(new Text("bar"), new Text("baz"));
        button.setText("qux");
        Assert.assertEquals("qux", button.getText());
    }

    @Test(expected = IllegalArgumentException.class)
    public void textNodeAsIcon_throws() {
        button = new Button("foo", new Text("bar"));
    }

    private Element getButtonChild(int index) {
        return button.getElement().getChild(index);
    }

}
