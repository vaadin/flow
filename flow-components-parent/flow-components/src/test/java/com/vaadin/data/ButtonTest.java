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

import java.util.function.Predicate;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.dom.Element;
import com.vaadin.ui.Component;
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
    public void setIcon() {
        button = new Button("foo", new Icon());
        Icon icon = new Icon();

        button.setIcon(icon);
        Assert.assertEquals(icon.getElement(), getButtonChild(0));

        button.setIcon(null);
        Assert.assertNull(button.getIcon());
        Assert.assertEquals(0, button.getChildren().count()); // icon removed
    }

    @Test(expected = IllegalArgumentException.class)
    public void textNodeAsIcon_throws() {
        button = new Button("foo", new Text("bar"));
    }

    @Test
    public void setText() {
        button = new Button("foo", new Icon());
        button.add(new Text("bar"), new Text("baz"));
        Assert.assertEquals("foobarbaz", button.getText());

        button.setText("qux");
        Assert.assertEquals("qux", button.getText());

        button.setText(null);
        Assert.assertEquals("", button.getText());
    }

    @Test
    public void iconAndTextPosition() {
        Icon icon = new Icon();
        button = new Button("foo", icon);
        assertIconBeforeText(icon);

        button.setIconAfterText(true);
        assertIconAfterText(icon);
    }

    @Test
    public void addChildren_addIcon_iconBeforeText() {
        button = new Button();
        addChildrenIncludingText();

        Icon icon = new Icon();
        button.setIcon(icon);

        assertIconBeforeText(icon);
    }

    @Test
    public void addChildren_setIconAfterText_addIcon_iconAfterText() {
        button = new Button();
        addChildrenIncludingText();

        button.setIconAfterText(true);

        Icon icon = new Icon();
        button.setIcon(icon);

        assertIconAfterText(icon);
    }

    @Test
    public void addIcon_addChildren_setIconAfterText_iconAfterText() {
        Icon icon = new Icon();
        button = new Button(icon);

        addChildrenIncludingText();

        button.setIconAfterText(true);

        assertIconAfterText(icon);
    }

    @Test
    public void addIconAfterText_addChildren_setIconBeforeText_iconBeforeText() {
        Icon icon = new Icon();
        button = new Button(icon);
        button.setIconAfterText(true);

        addChildrenIncludingText();

        button.setIconAfterText(false);

        assertIconBeforeText(icon);
    }

    private void addChildrenIncludingText() {
        button.add(new Icon(), new Text("foo"), new Icon(), new Text("bar"),
                new Icon());
    }

    private void assertIconAfterText(Component icon) {
        assertIconAfterText(icon, true);
    }

    private void assertIconBeforeText(Component icon) {
        assertIconAfterText(icon, false);
    }

    private void assertIconAfterText(Component icon, boolean iconAfterText) {
        Assert.assertEquals(iconAfterText, button.isIconAfterText());

        if (iconAfterText) {
            Assert.assertTrue(getLastIndexOfTextNode() < indexOf(icon));
        } else {
            Assert.assertTrue(getFirstIndexOfTextNode() > indexOf(icon));
        }
    }

    private int getFirstIndexOfTextNode() {
        return indexOf(Element::isTextNode, true);
    }

    private int getLastIndexOfTextNode() {
        return indexOf(Element::isTextNode, false);
    }

    private int indexOf(Component component) {
        return indexOf(element -> element.equals(component.getElement()), true);
    }

    /**
     * Finds a child element of the button that matches the given predicate and
     * returns the index of that element. It is expected that at least one such
     * element exists or an assertion error occurs.
     * 
     * @param elementPredicate
     *            condition for the element to look for
     * @param findFirstIndex
     *            true if the first matching occurrence is wanted, false if the
     *            last matching occurrence is wanted
     * 
     * @return the index of the first or the last child of the button that
     *         matches the elementPredicate
     */
    private int indexOf(Predicate<Element> elementPredicate,
            boolean findFirstIndex) {
        for (int i = 0; i < button.getElement().getChildCount(); i++) {
            int index = i;
            if (!findFirstIndex) {
                index = button.getElement().getChildCount() - i - 1;
            }

            if (elementPredicate.test(getButtonChild(index))) {
                return index;
            }
        }
        Assert.fail("Expected element was not found in the button.");
        return -1;
    }

    private Element getButtonChild(int index) {
        return button.getElement().getChild(index);
    }

}
