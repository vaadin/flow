/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextTest {

    @Test
    void elementAttached() {
        // This will throw an assertion error if the element is not attached to
        // the component
        new Text("Foo").getParent();
    }

    @Test
    void nullText_transformsToEmptyAndDoesNotThrowException() {
        assertEquals("", new Text(null).getText());
    }

    @Test
    void emptyText() {
        assertEquals("", new Text("").getText());
    }

    @Test
    void setText_emptyTextCanBeChangedLater() {
        Text text = new Text(null);
        text.setText("Non Empty");
        assertEquals("Non Empty", text.getText());
    }

    @Test
    void setText_nullIsChangedToEmptyAndDoesNotThrowException() {
        Text text = new Text("Default");
        text.setText(null);
        assertEquals("", text.getText());
    }

    @Test
    void setGetText() {
        assertEquals("Simple", new Text("Simple").getText());
        assertEquals("\u00e5\u00e4\u00f6 \u20ac#%\u00b0#",
                new Text("\u00e5\u00e4\u00f6 \u20ac#%\u00b0#").getText());
    }

    @Test
    void setId_throwsWithMeaningfulMessage() {
        Text text = new Text("");
        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class, () -> text.setId("foo"));
        assertTrue(ex.getMessage().contains("Cannot set 'id' "));
        assertTrue(ex.getMessage().contains(
                "component because it doesn't represent an HTML Element"));
    }

    @Test
    void setFooProperty_throwsWithMeaningfulMessage() {
        Text text = new Text("");
        PropertyDescriptor<Boolean, Boolean> property = PropertyDescriptors
                .propertyWithDefault("foo", true);
        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> text.set(property, false));
        assertTrue(ex.getMessage().contains("Cannot set 'foo' "));
        assertTrue(ex.getMessage().contains(
                "component because it doesn't represent an HTML Element"));
    }

    @Test
    void setVisibility_throwsWithMeaningfulMessage() {
        Text text = new Text("");
        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> text.setVisible(false));
        assertTrue(ex.getMessage()
                .contains("Cannot change Text component visibility"));
        assertTrue(ex.getMessage()
                .contains("because it doesn't represent an HTML Element"));
    }

    @Test
    void addClassName_throwsWithMeaningfulMessage() {
        Text text = new Text("");
        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> text.addClassName("foo"));
        assertTrue(ex.getMessage().contains("Cannot add a class to the Text"));
        assertTrue(ex.getMessage()
                .contains("because it doesn't represent an HTML Element"));
    }

    @Test
    void addClassNames_throwsWithMeaningfulMessage() {
        Text text = new Text("");
        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> text.addClassNames("foor", "bar"));
        assertTrue(ex.getMessage().contains("Cannot add classes to the Text"));
        assertTrue(ex.getMessage()
                .contains("because it doesn't represent an HTML Element"));
    }

    @Test
    void removeClassName_throwsWithMeaningfulMessage() {
        Text text = new Text("");
        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> text.removeClassName("foo"));
        assertTrue(ex.getMessage()
                .contains("Cannot remove a class from the Text"));
        assertTrue(ex.getMessage()
                .contains("because it doesn't represent an HTML Element"));
    }

    @Test
    void removeClassNames_throwsWithMeaningfulMessage() {
        Text text = new Text("");
        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> text.removeClassNames("foo", "bar"));
        assertTrue(ex.getMessage()
                .contains("Cannot remove classes from the Text"));
        assertTrue(ex.getMessage()
                .contains("because it doesn't represent an HTML Element"));
    }

    @Test
    void setClassName_throwsWithMeaningfulMessage() {
        Text text = new Text("");
        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> text.setClassName("foo"));
        assertTrue(ex.getMessage()
                .contains("Cannot set the Text component class"));
        assertTrue(ex.getMessage()
                .contains("because it doesn't represent an HTML Element"));
    }

    @Test
    void setClassName_withBooleanParameter_throwsWithMeaningfulMessage() {
        Text text = new Text("");
        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> text.setClassName("foo", true));
        assertTrue(ex.getMessage()
                .contains("Cannot set the Text component class"));
        assertTrue(ex.getMessage()
                .contains("because it doesn't represent an HTML Element"));
    }
}
