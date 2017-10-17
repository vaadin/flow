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
package com.vaadin.flow.tests.components.grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.data.provider.bov.Person;
import com.vaadin.data.selection.MultiSelectionEvent;
import com.vaadin.data.selection.MultiSelectionListener;
import com.vaadin.ui.grid.Grid;
import com.vaadin.ui.grid.Grid.SelectionMode;
import com.vaadin.ui.grid.GridMultiSelectionModel;
import com.vaadin.ui.grid.GridSelectionModel;

public class GridMultiSelectionModelTest {

    public static final Person PERSON_C = new Person("c", 3);
    public static final Person PERSON_B = new Person("b", 2);
    public static final Person PERSON_A = new Person("a", 1);

    private Grid<Person> grid;
    private GridMultiSelectionModel<Person> selectionModel;
    private Capture<List<Person>> currentSelectionCapture;
    private Capture<List<Person>> oldSelectionCapture;
    private AtomicInteger events;

    public final Map<String, Boolean> generatedData = new LinkedHashMap<>();

    @Before
    public void generateData() {

    }

    @Before
    public void setUp() {
        grid = new Grid<>();
        selectionModel = (GridMultiSelectionModel<Person>) grid
                .setSelectionMode(SelectionMode.MULTI);

        grid.setItems(PERSON_A, PERSON_B, PERSON_C);

        currentSelectionCapture = EasyMock.newCapture();
        oldSelectionCapture = EasyMock.newCapture();
        events = new AtomicInteger();

        selectionModel.addMultiSelectionListener(event -> {
            currentSelectionCapture
                    .setValue(new ArrayList<>(event.getNewSelection()));
            oldSelectionCapture
                    .setValue(new ArrayList<>(event.getOldSelection()));
            events.incrementAndGet();
        });
    }

    @Test(expected = IllegalStateException.class)
    @Ignore
    // Ignored because there's no mechanism to disallow selections yet
    public void throwExcpetionWhenSelectionIsDisallowed() {

        selectionModel.updateSelection(Collections.emptySet(),
                Collections.emptySet());
    }

    @Test(expected = IllegalStateException.class)
    public void selectionModelChanged_usingPreviousSelectionModel_throws() {
        grid.setSelectionMode(SelectionMode.SINGLE);
        selectionModel.select(PERSON_A);
    }

    @Test
    public void changingSelectionModel_firesSelectionEvent() {
        Grid<String> customGrid = new Grid<>();
        customGrid.setSelectionMode(SelectionMode.MULTI);
        customGrid.setItems("Foo", "Bar", "Baz");

        List<String> selectionChanges = new ArrayList<>();
        Capture<List<String>> oldSelectionCapture = EasyMock.newCapture();
        ((GridMultiSelectionModel<String>) customGrid.getSelectionModel())
                .addMultiSelectionListener(e -> {
                    selectionChanges.addAll(e.getValue());
                    oldSelectionCapture
                            .setValue(new ArrayList<>(e.getOldSelection()));
                });

        customGrid.getSelectionModel().select("Foo");
        assertEquals(Arrays.asList("Foo"), selectionChanges);
        selectionChanges.clear();

        customGrid.getSelectionModel().select("Bar");
        assertEquals("Foo",
                customGrid.getSelectionModel().getFirstSelectedItem().get());
        assertEquals(Arrays.asList("Foo", "Bar"), selectionChanges);
        selectionChanges.clear();

        customGrid.setSelectionMode(SelectionMode.SINGLE);
        assertFalse(customGrid.getSelectionModel().getFirstSelectedItem()
                .isPresent());
        assertEquals(Arrays.asList(), selectionChanges);
        assertEquals(Arrays.asList("Foo", "Bar"),
                oldSelectionCapture.getValue());
    }

    @Test
    public void select_gridWithStrings() {
        Grid<String> gridWithStrings = new Grid<>();
        gridWithStrings.setSelectionMode(SelectionMode.MULTI);
        gridWithStrings.setItems("Foo", "Bar", "Baz");

        GridSelectionModel<String> model = gridWithStrings.getSelectionModel();
        assertFalse(model.isSelected("Foo"));

        model.select("Foo");
        assertTrue(model.isSelected("Foo"));
        assertEquals(Optional.of("Foo"), model.getFirstSelectedItem());

        model.select("Bar");
        assertTrue(model.isSelected("Foo"));
        assertTrue(model.isSelected("Bar"));
        assertEquals(Arrays.asList("Foo", "Bar"),
                new ArrayList<>(model.getSelectedItems()));

        model.deselect("Bar");
        assertFalse(model.isSelected("Bar"));
        assertTrue(model.getFirstSelectedItem().isPresent());
        assertEquals(Arrays.asList("Foo"),
                new ArrayList<>(model.getSelectedItems()));
    }

    @Test
    public void select() {
        selectionModel.select(PERSON_B);

        assertEquals(PERSON_B,
                selectionModel.getFirstSelectedItem().orElse(null));
        assertEquals(Optional.of(PERSON_B),
                selectionModel.getFirstSelectedItem());

        assertFalse(selectionModel.isSelected(PERSON_A));
        assertTrue(selectionModel.isSelected(PERSON_B));
        assertFalse(selectionModel.isSelected(PERSON_C));

        assertEquals(Arrays.asList(PERSON_B),
                currentSelectionCapture.getValue());

        selectionModel.select(PERSON_A);
        assertEquals(PERSON_B,
                selectionModel.getFirstSelectedItem().orElse(null));

        assertTrue(selectionModel.isSelected(PERSON_A));
        assertTrue(selectionModel.isSelected(PERSON_B));
        assertFalse(selectionModel.isSelected(PERSON_C));

        assertEquals(Arrays.asList(PERSON_B, PERSON_A),
                currentSelectionCapture.getValue());
        assertEquals(2, events.get());
    }

    @Test
    public void deselect() {
        selectionModel.select(PERSON_B);
        selectionModel.deselect(PERSON_B);

        assertFalse(selectionModel.getFirstSelectedItem().isPresent());

        assertFalse(selectionModel.isSelected(PERSON_A));
        assertFalse(selectionModel.isSelected(PERSON_B));
        assertFalse(selectionModel.isSelected(PERSON_C));

        assertEquals(Arrays.asList(), currentSelectionCapture.getValue());
        assertEquals(2, events.get());
    }

    @Test
    public void selectItems() {
        selectionModel.selectItems(PERSON_C, PERSON_B);

        assertEquals(PERSON_C,
                selectionModel.getFirstSelectedItem().orElse(null));
        assertEquals(Optional.of(PERSON_C),
                selectionModel.getFirstSelectedItem());

        assertFalse(selectionModel.isSelected(PERSON_A));
        assertTrue(selectionModel.isSelected(PERSON_B));
        assertTrue(selectionModel.isSelected(PERSON_C));

        assertEquals(Arrays.asList(PERSON_C, PERSON_B),
                currentSelectionCapture.getValue());

        selectionModel.selectItems(PERSON_A, PERSON_C); // partly NOOP
        assertTrue(selectionModel.isSelected(PERSON_A));
        assertTrue(selectionModel.isSelected(PERSON_B));
        assertTrue(selectionModel.isSelected(PERSON_C));

        assertEquals(Arrays.asList(PERSON_C, PERSON_B, PERSON_A),
                currentSelectionCapture.getValue());
        assertEquals(2, events.get());
    }

    @Test
    public void deselectItems() {
        selectionModel.selectItems(PERSON_C, PERSON_A, PERSON_B);

        selectionModel.deselectItems(PERSON_A);
        assertEquals(PERSON_C,
                selectionModel.getFirstSelectedItem().orElse(null));
        assertEquals(Optional.of(PERSON_C),
                selectionModel.getFirstSelectedItem());

        assertFalse(selectionModel.isSelected(PERSON_A));
        assertTrue(selectionModel.isSelected(PERSON_B));
        assertTrue(selectionModel.isSelected(PERSON_C));

        assertEquals(Arrays.asList(PERSON_C, PERSON_B),
                currentSelectionCapture.getValue());

        selectionModel.deselectItems(PERSON_A, PERSON_B, PERSON_C);
        assertNull(selectionModel.getFirstSelectedItem().orElse(null));
        assertEquals(Optional.empty(), selectionModel.getFirstSelectedItem());

        assertFalse(selectionModel.isSelected(PERSON_A));
        assertFalse(selectionModel.isSelected(PERSON_B));
        assertFalse(selectionModel.isSelected(PERSON_C));

        assertEquals(Arrays.asList(), currentSelectionCapture.getValue());
        assertEquals(3, events.get());
    }

    @Test
    public void selectionEvent_newSelection_oldSelection() {
        selectionModel.selectItems(PERSON_C, PERSON_A, PERSON_B);

        assertEquals(Arrays.asList(PERSON_C, PERSON_A, PERSON_B),
                currentSelectionCapture.getValue());
        assertEquals(Arrays.asList(), oldSelectionCapture.getValue());

        selectionModel.deselect(PERSON_A);

        assertEquals(Arrays.asList(PERSON_C, PERSON_B),
                currentSelectionCapture.getValue());
        assertEquals(Arrays.asList(PERSON_C, PERSON_A, PERSON_B),
                oldSelectionCapture.getValue());

        selectionModel.deselectItems(PERSON_A, PERSON_B, PERSON_C);
        assertEquals(Arrays.asList(), currentSelectionCapture.getValue());
        assertEquals(Arrays.asList(PERSON_C, PERSON_B),
                oldSelectionCapture.getValue());

        selectionModel.selectItems(PERSON_A);
        assertEquals(Arrays.asList(PERSON_A),
                currentSelectionCapture.getValue());
        assertEquals(Arrays.asList(), oldSelectionCapture.getValue());

        selectionModel.updateSelection(
                new LinkedHashSet<>(Arrays.asList(PERSON_B, PERSON_C)),
                new LinkedHashSet<>(Arrays.asList(PERSON_A)));
        assertEquals(Arrays.asList(PERSON_B, PERSON_C),
                currentSelectionCapture.getValue());
        assertEquals(Arrays.asList(PERSON_A), oldSelectionCapture.getValue());

        selectionModel.deselectAll();
        assertEquals(Arrays.asList(), currentSelectionCapture.getValue());
        assertEquals(Arrays.asList(PERSON_B, PERSON_C),
                oldSelectionCapture.getValue());

        selectionModel.select(PERSON_C);
        assertEquals(Arrays.asList(PERSON_C),
                currentSelectionCapture.getValue());
        assertEquals(Arrays.asList(), oldSelectionCapture.getValue());

        selectionModel.deselect(PERSON_C);
        assertEquals(Arrays.asList(), currentSelectionCapture.getValue());
        assertEquals(Arrays.asList(PERSON_C), oldSelectionCapture.getValue());
    }

    @Test
    public void deselectAll() {
        selectionModel.selectItems(PERSON_A, PERSON_C, PERSON_B);

        assertTrue(selectionModel.isSelected(PERSON_A));
        assertTrue(selectionModel.isSelected(PERSON_B));
        assertTrue(selectionModel.isSelected(PERSON_C));
        assertEquals(Arrays.asList(PERSON_A, PERSON_C, PERSON_B),
                currentSelectionCapture.getValue());
        assertEquals(1, events.get());

        selectionModel.deselectAll();
        assertFalse(selectionModel.isSelected(PERSON_A));
        assertFalse(selectionModel.isSelected(PERSON_B));
        assertFalse(selectionModel.isSelected(PERSON_C));
        assertEquals(Arrays.asList(), currentSelectionCapture.getValue());
        assertEquals(Arrays.asList(PERSON_A, PERSON_C, PERSON_B),
                oldSelectionCapture.getValue());
        assertEquals(2, events.get());

        selectionModel.select(PERSON_C);
        assertFalse(selectionModel.isSelected(PERSON_A));
        assertFalse(selectionModel.isSelected(PERSON_B));
        assertTrue(selectionModel.isSelected(PERSON_C));
        assertEquals(Arrays.asList(PERSON_C),
                currentSelectionCapture.getValue());
        assertEquals(Arrays.asList(), oldSelectionCapture.getValue());
        assertEquals(3, events.get());

        selectionModel.deselectAll();
        assertFalse(selectionModel.isSelected(PERSON_A));
        assertFalse(selectionModel.isSelected(PERSON_B));
        assertFalse(selectionModel.isSelected(PERSON_C));
        assertEquals(Arrays.asList(), currentSelectionCapture.getValue());
        assertEquals(Arrays.asList(PERSON_C), oldSelectionCapture.getValue());
        assertEquals(4, events.get());

        selectionModel.deselectAll();
        assertEquals(4, events.get());
    }

    @Test
    @Ignore
    // Ignored because selectAll is not implemented yet
    public void selectAll() {
        selectionModel.selectAll();

        assertTrue(selectionModel.isSelected(PERSON_A));
        assertTrue(selectionModel.isSelected(PERSON_B));
        assertTrue(selectionModel.isSelected(PERSON_C));
        assertEquals(Arrays.asList(PERSON_A, PERSON_B, PERSON_C),
                currentSelectionCapture.getValue());
        assertEquals(1, events.get());

        selectionModel.deselectItems(PERSON_A, PERSON_C);

        assertFalse(selectionModel.isSelected(PERSON_A));
        assertTrue(selectionModel.isSelected(PERSON_B));
        assertFalse(selectionModel.isSelected(PERSON_C));
        assertEquals(Arrays.asList(PERSON_A, PERSON_B, PERSON_C),
                oldSelectionCapture.getValue());

        selectionModel.selectAll();

        assertTrue(selectionModel.isSelected(PERSON_A));
        assertTrue(selectionModel.isSelected(PERSON_B));
        assertTrue(selectionModel.isSelected(PERSON_C));
        assertEquals(Arrays.asList(PERSON_B, PERSON_A, PERSON_C),
                currentSelectionCapture.getValue());
        assertEquals(Arrays.asList(PERSON_B), oldSelectionCapture.getValue());
        assertEquals(3, events.get());
    }

    @Test
    public void updateSelection() {
        selectionModel.updateSelection(asSet(PERSON_A), Collections.emptySet());

        assertTrue(selectionModel.isSelected(PERSON_A));
        assertFalse(selectionModel.isSelected(PERSON_B));
        assertFalse(selectionModel.isSelected(PERSON_C));
        assertEquals(Arrays.asList(PERSON_A),
                currentSelectionCapture.getValue());
        assertEquals(1, events.get());

        selectionModel.updateSelection(asSet(PERSON_B), asSet(PERSON_A));

        assertFalse(selectionModel.isSelected(PERSON_A));
        assertTrue(selectionModel.isSelected(PERSON_B));
        assertFalse(selectionModel.isSelected(PERSON_C));
        assertEquals(Arrays.asList(PERSON_B),
                currentSelectionCapture.getValue());
        assertEquals(Arrays.asList(PERSON_A), oldSelectionCapture.getValue());
        assertEquals(2, events.get());

        selectionModel.updateSelection(asSet(PERSON_B), asSet(PERSON_A)); // NOOP

        assertFalse(selectionModel.isSelected(PERSON_A));
        assertTrue(selectionModel.isSelected(PERSON_B));
        assertFalse(selectionModel.isSelected(PERSON_C));
        assertEquals(Arrays.asList(PERSON_B),
                currentSelectionCapture.getValue());
        assertEquals(Arrays.asList(PERSON_A), oldSelectionCapture.getValue());
        assertEquals(2, events.get());

        selectionModel.updateSelection(asSet(PERSON_A, PERSON_C),
                asSet(PERSON_A)); // partly NOOP

        assertFalse(selectionModel.isSelected(PERSON_A));
        assertTrue(selectionModel.isSelected(PERSON_B));
        assertTrue(selectionModel.isSelected(PERSON_C));
        assertEquals(Arrays.asList(PERSON_B, PERSON_C),
                currentSelectionCapture.getValue());
        assertEquals(Arrays.asList(PERSON_B), oldSelectionCapture.getValue());
        assertEquals(3, events.get());

        selectionModel.updateSelection(asSet(PERSON_B, PERSON_A),
                asSet(PERSON_B)); // partly NOOP

        assertTrue(selectionModel.isSelected(PERSON_A));
        assertTrue(selectionModel.isSelected(PERSON_B));
        assertTrue(selectionModel.isSelected(PERSON_C));
        assertEquals(Arrays.asList(PERSON_B, PERSON_C, PERSON_A),
                currentSelectionCapture.getValue());
        assertEquals(Arrays.asList(PERSON_B, PERSON_C),
                oldSelectionCapture.getValue());
        assertEquals(4, events.get());

        selectionModel.updateSelection(asSet(),
                asSet(PERSON_B, PERSON_A, PERSON_C));

        assertFalse(selectionModel.isSelected(PERSON_A));
        assertFalse(selectionModel.isSelected(PERSON_B));
        assertFalse(selectionModel.isSelected(PERSON_C));
        assertEquals(Arrays.asList(), currentSelectionCapture.getValue());
        assertEquals(Arrays.asList(PERSON_B, PERSON_C, PERSON_A),
                oldSelectionCapture.getValue());
        assertEquals(5, events.get());
    }

    private <T> Set<T> asSet(@SuppressWarnings("unchecked") T... people) {
        return new LinkedHashSet<>(Arrays.asList(people));
    }

    @Test
    public void selectTwice() {
        selectionModel.select(PERSON_C);
        selectionModel.select(PERSON_C);

        assertEquals(PERSON_C,
                selectionModel.getFirstSelectedItem().orElse(null));

        assertFalse(selectionModel.isSelected(PERSON_A));
        assertFalse(selectionModel.isSelected(PERSON_B));
        assertTrue(selectionModel.isSelected(PERSON_C));

        assertEquals(Optional.of(PERSON_C),
                selectionModel.getFirstSelectedItem());

        assertEquals(Arrays.asList(PERSON_C),
                currentSelectionCapture.getValue());
        assertEquals(1, events.get());
    }

    @Test
    public void deselectTwice() {
        selectionModel.select(PERSON_C);
        assertEquals(Arrays.asList(PERSON_C),
                currentSelectionCapture.getValue());
        assertEquals(1, events.get());

        selectionModel.deselect(PERSON_C);

        assertFalse(selectionModel.getFirstSelectedItem().isPresent());
        assertFalse(selectionModel.isSelected(PERSON_C));
        assertEquals(Arrays.asList(), currentSelectionCapture.getValue());
        assertEquals(2, events.get());

        selectionModel.deselect(PERSON_C);

        assertFalse(selectionModel.getFirstSelectedItem().isPresent());
        assertFalse(selectionModel.isSelected(PERSON_A));
        assertFalse(selectionModel.isSelected(PERSON_B));
        assertFalse(selectionModel.isSelected(PERSON_C));
        assertEquals(Arrays.asList(), currentSelectionCapture.getValue());
        assertEquals(2, events.get());
    }

    @SuppressWarnings({ "serial" })
    @Test
    public void addValueChangeListener() {
        String value = "foo";

        Grid<String> grid = new Grid<String>();

        AtomicReference<MultiSelectionEvent<Grid<String>, String>> event = new AtomicReference<>();
        MultiSelectionListener<Grid<String>, String> selectionListener = evt -> {
            assertNull(event.get());
            event.set(evt);
        };
        GridMultiSelectionModel<String> model = (GridMultiSelectionModel<String>) grid
                .setSelectionMode(SelectionMode.MULTI);

        model = Mockito.spy(model);

        Mockito.when(model.getSelectedItems())
                .thenReturn(new LinkedHashSet<String>(Arrays.asList(value)));

        grid.setItems("foo", "bar");

        model.addMultiSelectionListener(selectionListener);

        selectionListener.selectionChange(new MultiSelectionEvent<>(grid,
                model.asMultiSelect(), Collections.emptySet(), true));

        assertEquals(grid, event.get().getSource());
        assertEquals(new LinkedHashSet<>(Arrays.asList(value)),
                event.get().getValue());
        assertTrue(event.get().isFromClient());

        Mockito.verify(model, Mockito.times(1)).getSelectedItems();
    }
}
