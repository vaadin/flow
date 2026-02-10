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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TextTest {

    @Test
    void elementAttached() {
        // This will throw an assertion error if the element is not attached to
        // the component
        new Text("Foo").getParent();
    }

    @Test
    void nullText_transformsToEmptyAndDoesNotThrowException() {
        Assertions.assertEquals("", new Text(null).getText());
    }

    @Test
    void emptyText() {
        Assertions.assertEquals("", new Text("").getText());
    }

    @Test
    void setText_emptyTextCanBeChangedLater() {
        Text text = new Text(null);
        text.setText("Non Empty");
        Assertions.assertEquals("Non Empty", text.getText());
    }

    @Test
    void setText_nullIsChangedToEmptyAndDoesNotThrowException() {
        Text text = new Text("Default");
        text.setText(null);
        Assertions.assertEquals("", text.getText());
    }

    @Test
    void setGetText() {
        Assertions.assertEquals("Simple", new Text("Simple").getText());
        Assertions.assertEquals("\u00e5\u00e4\u00f6 \u20ac#%\u00b0#",
                new Text("\u00e5\u00e4\u00f6 \u20ac#%\u00b0#").getText());
    }

    @Test
    void setId_throwsWithMeaningfulMessage() {
        UnsupportedOperationException ex = Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> new Text("").setId("foo"));
        Assertions.assertTrue(ex.getMessage().contains("Cannot set 'id' "));
        Assertions.assertTrue(ex.getMessage().contains(
                "component because it doesn't represent an HTML Element"));
    }

    @Test
    void setFooProperty_throwsWithMeaningfulMessage() {
        UnsupportedOperationException ex = Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> new Text("").set(
                        PropertyDescriptors.propertyWithDefault("foo", true),
                        false));
        Assertions.assertTrue(ex.getMessage().contains("Cannot set 'foo' "));
        Assertions.assertTrue(ex.getMessage().contains(
                "component because it doesn't represent an HTML Element"));
    }

    @Test
    void setVisibility_throwsWithMeaningfulMessage() {
        UnsupportedOperationException ex = Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> new Text("").setVisible(false));
        Assertions.assertTrue(ex.getMessage()
                .contains("Cannot change Text component visibility"));
        Assertions.assertTrue(ex.getMessage()
                .contains("because it doesn't represent an HTML Element"));
    }

    @Test
    void addClassName_throwsWithMeaningfulMessage() {
        UnsupportedOperationException ex = Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> new Text("").addClassName("foo"));
        Assertions.assertTrue(
                ex.getMessage().contains("Cannot add a class to the Text"));
        Assertions.assertTrue(ex.getMessage()
                .contains("because it doesn't represent an HTML Element"));
    }

    @Test
    void addClassNames_throwsWithMeaningfulMessage() {
        UnsupportedOperationException ex = Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> new Text("").addClassNames("foor", "bar"));
        Assertions.assertTrue(
                ex.getMessage().contains("Cannot add classes to the Text"));
        Assertions.assertTrue(ex.getMessage()
                .contains("because it doesn't represent an HTML Element"));
    }

    @Test
    void removeClassName_throwsWithMeaningfulMessage() {
        UnsupportedOperationException ex = Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> new Text("").removeClassName("foo"));
        Assertions.assertTrue(ex.getMessage()
                .contains("Cannot remove a class from the Text"));
        Assertions.assertTrue(ex.getMessage()
                .contains("because it doesn't represent an HTML Element"));
    }

    @Test
    void removeClassNames_throwsWithMeaningfulMessage() {
        UnsupportedOperationException ex = Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> new Text("").removeClassNames("foo", "bar"));
        Assertions.assertTrue(ex.getMessage()
                .contains("Cannot remove classes from the Text"));
        Assertions.assertTrue(ex.getMessage()
                .contains("because it doesn't represent an HTML Element"));
    }

    @Test
    void setClassName_throwsWithMeaningfulMessage() {
        UnsupportedOperationException ex = Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> new Text("").setClassName("foo"));
        Assertions.assertTrue(ex.getMessage()
                .contains("Cannot set the Text component class"));
        Assertions.assertTrue(ex.getMessage()
                .contains("because it doesn't represent an HTML Element"));
    }

    @Test
    void setClassName_withBooleanParameter_throwsWithMeaningfulMessage() {
        UnsupportedOperationException ex = Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> new Text("").setClassName("foo", true));
        Assertions.assertTrue(ex.getMessage()
                .contains("Cannot set the Text component class"));
        Assertions.assertTrue(ex.getMessage()
                .contains("because it doesn't represent an HTML Element"));
    }
}
