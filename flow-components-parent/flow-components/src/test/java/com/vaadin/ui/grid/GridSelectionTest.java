package com.vaadin.ui.grid;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.vaadin.ui.grid.Grid.SelectionMode;

public class GridSelectionTest {

    @Test
    public void multiSelectionListeners() {
        Grid<String> grid = new Grid<>();
        grid.setSelectionMode(SelectionMode.MULTI);

        Set<String> oldSelection = new LinkedHashSet<>();
        Set<String> selection = new LinkedHashSet<>(
                Arrays.asList("0", "1", "2"));

        AtomicInteger selectionListenerCalled = new AtomicInteger();
        grid.addSelectionListener(event -> {
            selectionListenerCalled.incrementAndGet();
            assertEquals(selection, event.getAllSelectedItems());
        });

        AtomicInteger valueChangeListenerCalled = new AtomicInteger();
        grid.asMultiSelect().addValueChangeListener(event -> {
            valueChangeListenerCalled.incrementAndGet();
            assertEquals(oldSelection, event.getOldValue());
            assertEquals(selection, event.getValue());
        });

        AtomicInteger multiSelectionListenerCalled = new AtomicInteger();
        ((GridMultiSelectionModel<String>) grid.getSelectionModel())
                .addMultiSelectionListener(event -> {
                    multiSelectionListenerCalled.incrementAndGet();
                    assertEquals(oldSelection, event.getOldSelection());
                    assertEquals(selection, event.getNewSelection());

                    Set<String> oldCopy = new LinkedHashSet<>(oldSelection);
                    Set<String> copy = new LinkedHashSet<>(selection);
                    oldCopy.removeAll(copy);
                    assertEquals(oldCopy, event.getRemovedSelection());
                    oldCopy = new LinkedHashSet<>(oldSelection);
                    copy.removeAll(oldCopy);
                    assertEquals(copy, event.getAddedSelection());
                });

        grid.asMultiSelect().setValue(selection);

        oldSelection.addAll(selection);
        selection.clear();
        selection.addAll(Arrays.asList("10", "1", "5"));
        grid.asMultiSelect().setValue(selection);

        assertEquals(2, selectionListenerCalled.get());
        assertEquals(2, valueChangeListenerCalled.get());
        assertEquals(2, multiSelectionListenerCalled.get());
    }
}
