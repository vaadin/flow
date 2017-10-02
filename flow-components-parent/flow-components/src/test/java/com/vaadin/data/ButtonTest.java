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
import com.vaadin.ui.Text;
import com.vaadin.ui.button.Button;
import com.vaadin.ui.icon.Icon;

public class ButtonTest {

    private Button button;
    private Icon icon;

    private static final String TEST_STRING = "lorem ipsum";

    private static final String THEME_ATTRIBUTE = "theme";
    private static final String THEME_ATTRIBUTE_ICON = "icon";
    private static final String THEME_ATTRIBUTE_PRIMARY = "primary";

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

        icon = new Icon();
        button.setIcon(icon);
        Assert.assertEquals(icon.getElement(), getButtonChild(0));

        button.setIcon(null);
        Assert.assertNull(button.getIcon());
        Assert.assertFalse(
                button.getChildren().anyMatch(child -> child.equals(icon)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void textNodeAsIcon_throws() {
        button = new Button("foo", new Text("bar"));
    }

    @Test
    public void setText() {
        button = new Button("foo", new Icon());
        button.setText(null);
        Assert.assertEquals("", button.getText());

        button.setText("bar");
        Assert.assertEquals("bar", button.getText());

        button.setText("");
        Assert.assertEquals("", button.getText());
    }

    @Test
    public void setText_setIcon_changeOrder() {
        icon = new Icon();
        button = new Button();

        button.setText(TEST_STRING);
        button.setIcon(icon);

        assertIconBeforeText();
        button.setIconAfterText(true);
        assertIconAfterText();
    }

    @Test
    public void changeOrder_setIcon_setText_changeOrder() {
        icon = new Icon();
        button = new Button();

        button.setIconAfterText(true);

        button.setIcon(icon);
        button.setText(TEST_STRING);

        assertIconAfterText();
        button.setIconAfterText(false);
        assertIconBeforeText();
    }

    @Test
    public void updatingThemeAttribute() {
        button = new Button();
        assertButtonHasThemeAttribute(null);

        button.setIcon(new Icon());
        assertButtonHasThemeAttribute(THEME_ATTRIBUTE_ICON);

        button.setText("foo");
        assertButtonHasThemeAttribute(null);

        button.setIcon(null);
        assertButtonHasThemeAttribute(null);

        button = new Button("foo", new Icon());
        assertButtonHasThemeAttribute(null);

        button.setText("");
        assertButtonHasThemeAttribute(THEME_ATTRIBUTE_ICON);

        button = new Button("foo", new Icon());
        button.setText(null);
        assertButtonHasThemeAttribute(THEME_ATTRIBUTE_ICON);

        // don't override explicitly set theme-attribute
        button = new Button();
        button.getElement().setAttribute(THEME_ATTRIBUTE,
                THEME_ATTRIBUTE_PRIMARY);
        assertButtonHasThemeAttribute(THEME_ATTRIBUTE_PRIMARY);
        button.setIcon(new Icon());
        assertButtonHasThemeAttribute(THEME_ATTRIBUTE_PRIMARY);
        button.setText("foo");
        assertButtonHasThemeAttribute(THEME_ATTRIBUTE_PRIMARY);
        button.setIcon(null);
        assertButtonHasThemeAttribute(THEME_ATTRIBUTE_PRIMARY);
        button.setText(null);
        assertButtonHasThemeAttribute(THEME_ATTRIBUTE_PRIMARY);
    }

    @Test
    public void wrappingTextInSpan() {
        button = new Button();
        button.setText(TEST_STRING);

        assertOnlyChildIsText();
        button.setIcon(new Icon());
        assertIconAndSpan(false);
        button.setIconAfterText(true);
        assertIconAndSpan(true);
        button.setText(null);
        assertOnlyChildIsIcon();

        button = new Button();
        button.setIconAfterText(true);
        button.setText(TEST_STRING);

        assertOnlyChildIsText();
        button.setIcon(new Icon());
        assertIconAndSpan(true);
        button.setIconAfterText(false);
        assertIconAndSpan(false);
        button.setText("");
        assertOnlyChildIsIcon();
    }

    private void assertOnlyChildIsText() {
        Assert.assertEquals(1, button.getElement().getChildCount());
        Element child = getButtonChild(0);
        Assert.assertTrue(child.isTextNode());
        Assert.assertEquals(TEST_STRING, child.getText());
    }

    private void assertOnlyChildIsIcon() {
        Assert.assertEquals(1, button.getElement().getChildCount());
        Element child = getButtonChild(0);
        Assert.assertEquals("iron-icon", child.getTag());
    }

    private void assertIconAndSpan(boolean iconAfterText) {
        Assert.assertEquals(2, button.getElement().getChildCount());

        Element icon = getButtonChild(iconAfterText ? 1 : 0);
        Element span = getButtonChild(iconAfterText ? 0 : 1);

        Assert.assertEquals("iron-icon", icon.getTag());
        Assert.assertEquals("span", span.getTag());
        Assert.assertEquals(TEST_STRING, span.getText());
    }

    private void assertButtonHasThemeAttribute(String theme) {
        if (theme == null) {
            Assert.assertFalse(
                    button.getElement().hasAttribute(THEME_ATTRIBUTE));
        } else {
            Assert.assertEquals(theme,
                    button.getElement().getAttribute(THEME_ATTRIBUTE));
        }
    }

    private void assertIconBeforeText() {
        Assert.assertFalse(button.isIconAfterText());
        Assert.assertTrue(indexOfIcon() < indexOfText());
    }

    private void assertIconAfterText() {
        Assert.assertTrue(button.isIconAfterText());
        Assert.assertTrue(indexOfText() < indexOfIcon());
    }

    /**
     * Returns the first index of an element inside the button that has the
     * {@link #TEST_STRING} as it's text content. If such element is not found,
     * assertion error occurs.
     */
    private int indexOfText() {
        return indexOf(element -> element.getText().equals(TEST_STRING));
    }

    /**
     * Returns the index of icon inside the button. The icon is expected to be
     * in the button, or an assertion error occurs.
     */
    private int indexOfIcon() {
        int index = button.getElement().indexOfChild(icon.getElement());

        if (index < 0) {
            Assert.fail("Expected icon was not found in the button.");
        }

        return index;
    }

    /**
     * Finds a child element of the button that matches the given predicate and
     * returns the index of that element. It is expected that at least one such
     * element exists or an assertion error occurs.
     *
     * @param elementPredicate
     *            condition for the element to look for
     *
     * @return the index of the first child of the button that matches the
     *         elementPredicate
     */
    private int indexOf(Predicate<Element> elementPredicate) {
        for (int i = 0; i < button.getElement().getChildCount(); i++) {
            if (elementPredicate.test(getButtonChild(i))) {
                return i;
            }
        }
        Assert.fail("Expected element was not found in the button.");
        return -1;
    }

    private Element getButtonChild(int index) {
        return button.getElement().getChild(index);
    }

}
