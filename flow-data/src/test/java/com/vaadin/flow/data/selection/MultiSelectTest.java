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
package com.vaadin.flow.data.selection;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.data.binder.testcomponents.TestLabel;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.shared.Registration;

class MultiSelectTest {

    private static class MultiSelectMock
            implements MultiSelect<TestLabel, String> {

        private final TestLabel component = new TestLabel();
        private final Set<String> selectedItems = new LinkedHashSet<>();

        public MultiSelectMock(String... initialItems) {
            Collections.addAll(selectedItems, initialItems);
        }

        @Override
        public Registration addValueChangeListener(
                ValueChangeListener<? super ComponentValueChangeEvent<TestLabel, Set<String>>> listener) {
            return null;
        }

        @Override
        public Element getElement() {
            return component.getElement();
        }

        @Override
        public void updateSelection(Set<String> addedItems,
                Set<String> removedItems) {
            selectedItems.removeAll(removedItems);
            selectedItems.addAll(addedItems);
        }

        @Override
        public Set<String> getSelectedItems() {
            return new LinkedHashSet<>(selectedItems);
        }

        @Override
        public Registration addSelectionListener(
                MultiSelectionListener<TestLabel, String> listener) {
            return null;
        }

    }

    @Test
    public void selectItem_previousItemsAreStillSelected() {
        MultiSelectMock select = new MultiSelectMock("item 1");
        Assertions.assertEquals(1, select.getSelectedItems().size());
        Assertions.assertArrayEquals(new Object[] { "item 1" },
                select.getSelectedItems().toArray());
        Assertions.assertTrue(select.isSelected("item 1"));
        Assertions.assertFalse(select.isSelected("item 2"));

        select.select("item 2");

        Assertions.assertEquals(2, select.getSelectedItems().size());
        Assertions.assertArrayEquals(new Object[] { "item 1", "item 2" },
                select.getSelectedItems().toArray());
        Assertions.assertTrue(select.isSelected("item 1"));
        Assertions.assertTrue(select.isSelected("item 2"));

        select.select(Arrays.asList("item 3", "item 4"));

        Assertions.assertEquals(4, select.getSelectedItems().size());
        Assertions.assertArrayEquals(
                new Object[] { "item 1", "item 2", "item 3", "item 4" },
                select.getSelectedItems().toArray());
        Assertions.assertTrue(select.isSelected("item 1"));
        Assertions.assertTrue(select.isSelected("item 2"));
        Assertions.assertTrue(select.isSelected("item 3"));
        Assertions.assertTrue(select.isSelected("item 4"));
    }

    @Test
    public void deselectItem_deselectNonSelectedItem_nothingHappens() {
        MultiSelectMock select = new MultiSelectMock("item 1");
        select.deselect("item 2");

        Assertions.assertEquals(1, select.getSelectedItems().size());
        Assertions.assertArrayEquals(new Object[] { "item 1" },
                select.getSelectedItems().toArray());
        Assertions.assertTrue(select.isSelected("item 1"));
        Assertions.assertFalse(select.isSelected("item 2"));
    }

    @Test
    public void deselectItem_deselectSelectedItem_itemIsDesselected() {
        MultiSelectMock select = new MultiSelectMock("item 1", "item 2",
                "item 3");
        select.deselect("item 2");

        Assertions.assertEquals(2, select.getSelectedItems().size());
        Assertions.assertArrayEquals(new Object[] { "item 1", "item 3" },
                select.getSelectedItems().toArray());
        Assertions.assertTrue(select.isSelected("item 1"));
        Assertions.assertFalse(select.isSelected("item 2"));
        Assertions.assertTrue(select.isSelected("item 3"));

        select.deselect(Arrays.asList("item 3", "item 1"));

        Assertions.assertEquals(0, select.getSelectedItems().size());
        Assertions.assertFalse(select.isSelected("item 1"));
        Assertions.assertFalse(select.isSelected("item 2"));
        Assertions.assertFalse(select.isSelected("item 3"));
    }

    @Test
    public void setValue_previousItemsAreOverridden() {
        MultiSelectMock select = new MultiSelectMock("item 1", "item 2");
        select.setValue(new HashSet<>(Arrays.asList("item 3")));

        Assertions.assertEquals(1, select.getSelectedItems().size());
        Assertions.assertArrayEquals(new Object[] { "item 3" },
                select.getSelectedItems().toArray());
        Assertions.assertFalse(select.isSelected("item 1"));
        Assertions.assertFalse(select.isSelected("item 2"));
        Assertions.assertTrue(select.isSelected("item 3"));

        select.setValue(Collections.emptySet());

        Assertions.assertEquals(0, select.getSelectedItems().size());
        Assertions.assertFalse(select.isSelected("item 1"));
        Assertions.assertFalse(select.isSelected("item 2"));
        Assertions.assertFalse(select.isSelected("item 3"));
    }

    @Test
    public void getValue_selectAndDeselect_correctItemsAreReturned() {
        MultiSelectMock select = new MultiSelectMock("item 1", "item 2");

        Assertions.assertEquals(2, select.getValue().size());
        Assertions.assertArrayEquals(new Object[] { "item 1", "item 2" },
                select.getValue().toArray());

        select.select("item 3");

        Assertions.assertEquals(3, select.getValue().size());
        Assertions.assertArrayEquals(
                new Object[] { "item 1", "item 2", "item 3" },
                select.getValue().toArray());

        select.deselect("item 2");

        Assertions.assertEquals(2, select.getValue().size());
        Assertions.assertArrayEquals(new Object[] { "item 1", "item 3" },
                select.getValue().toArray());
    }

    @Test
    public void deselectAll_noValuesAreReturned() {
        MultiSelectMock select = new MultiSelectMock("item 1", "item 2");
        select.deselectAll();

        Assertions.assertEquals(0, select.getValue().size());
        Assertions.assertEquals(0, select.getSelectedItems().size());
    }

}
