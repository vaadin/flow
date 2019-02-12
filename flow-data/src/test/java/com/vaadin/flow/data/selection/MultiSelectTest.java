/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import com.vaadin.flow.data.binder.testcomponents.TestLabel;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.shared.Registration;

public class MultiSelectTest {

    private static class MultiSelectMock implements MultiSelect<TestLabel, String> {

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
        Assert.assertEquals(1, select.getSelectedItems().size());
        Assert.assertArrayEquals(new Object[] { "item 1" },
                select.getSelectedItems().toArray());
        Assert.assertTrue(select.isSelected("item 1"));
        Assert.assertFalse(select.isSelected("item 2"));

        select.select("item 2");

        Assert.assertEquals(2, select.getSelectedItems().size());
        Assert.assertArrayEquals(new Object[] { "item 1", "item 2" },
                select.getSelectedItems().toArray());
        Assert.assertTrue(select.isSelected("item 1"));
        Assert.assertTrue(select.isSelected("item 2"));

        select.select(Arrays.asList("item 3", "item 4"));

        Assert.assertEquals(4, select.getSelectedItems().size());
        Assert.assertArrayEquals(
                new Object[] { "item 1", "item 2", "item 3", "item 4" },
                select.getSelectedItems().toArray());
        Assert.assertTrue(select.isSelected("item 1"));
        Assert.assertTrue(select.isSelected("item 2"));
        Assert.assertTrue(select.isSelected("item 3"));
        Assert.assertTrue(select.isSelected("item 4"));
    }

    @Test
    public void deselectItem_deselectNonSelectedItem_nothingHappens() {
        MultiSelectMock select = new MultiSelectMock("item 1");
        select.deselect("item 2");

        Assert.assertEquals(1, select.getSelectedItems().size());
        Assert.assertArrayEquals(new Object[] { "item 1" },
                select.getSelectedItems().toArray());
        Assert.assertTrue(select.isSelected("item 1"));
        Assert.assertFalse(select.isSelected("item 2"));
    }

    @Test
    public void deselectItem_deselectSelectedItem_itemIsDesselected() {
        MultiSelectMock select = new MultiSelectMock("item 1", "item 2",
                "item 3");
        select.deselect("item 2");

        Assert.assertEquals(2, select.getSelectedItems().size());
        Assert.assertArrayEquals(new Object[] { "item 1", "item 3" },
                select.getSelectedItems().toArray());
        Assert.assertTrue(select.isSelected("item 1"));
        Assert.assertFalse(select.isSelected("item 2"));
        Assert.assertTrue(select.isSelected("item 3"));

        select.deselect(Arrays.asList("item 3", "item 1"));

        Assert.assertEquals(0, select.getSelectedItems().size());
        Assert.assertFalse(select.isSelected("item 1"));
        Assert.assertFalse(select.isSelected("item 2"));
        Assert.assertFalse(select.isSelected("item 3"));
    }

    @Test
    public void setValue_previousItemsAreOverridden() {
        MultiSelectMock select = new MultiSelectMock("item 1", "item 2");
        select.setValue(new HashSet<>(Arrays.asList("item 3")));

        Assert.assertEquals(1, select.getSelectedItems().size());
        Assert.assertArrayEquals(new Object[] { "item 3" },
                select.getSelectedItems().toArray());
        Assert.assertFalse(select.isSelected("item 1"));
        Assert.assertFalse(select.isSelected("item 2"));
        Assert.assertTrue(select.isSelected("item 3"));

        select.setValue(Collections.emptySet());

        Assert.assertEquals(0, select.getSelectedItems().size());
        Assert.assertFalse(select.isSelected("item 1"));
        Assert.assertFalse(select.isSelected("item 2"));
        Assert.assertFalse(select.isSelected("item 3"));
    }

    @Test
    public void getValue_selectAndDeselect_correctItemsAreReturned() {
        MultiSelectMock select = new MultiSelectMock("item 1", "item 2");

        Assert.assertEquals(2, select.getValue().size());
        Assert.assertArrayEquals(new Object[] { "item 1", "item 2" },
                select.getValue().toArray());

        select.select("item 3");

        Assert.assertEquals(3, select.getValue().size());
        Assert.assertArrayEquals(new Object[] { "item 1", "item 2", "item 3" },
                select.getValue().toArray());

        select.deselect("item 2");

        Assert.assertEquals(2, select.getValue().size());
        Assert.assertArrayEquals(new Object[] { "item 1", "item 3" },
                select.getValue().toArray());
    }

    @Test
    public void deselectAll_noValuesAreReturned() {
        MultiSelectMock select = new MultiSelectMock("item 1", "item 2");
        select.deselectAll();

        Assert.assertEquals(0, select.getValue().size());
        Assert.assertEquals(0, select.getSelectedItems().size());
    }

}
