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
package com.vaadin.flow.uitest.ui.signal;

import java.util.Optional;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Table;
import com.vaadin.flow.component.html.TableDataCell;
import com.vaadin.flow.component.html.TableRow;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Test view for binding the rows of a {@link Table} body to a
 * {@link ListSignal}, so that inserting, renaming, moving and removing an entry
 * is reflected in the rendered <code>&lt;tbody&gt;</code>.
 */
@Route(value = "com.vaadin.flow.uitest.ui.signal.BindTableRowsView", layout = ViewTestLayout.class)
public class BindTableRowsView extends Div {

    private final ListSignal<String> planets = new ListSignal<>();

    public BindTableRowsView() {
        Table table = new Table();
        table.setId("table");
        table.addHeaderRow("Planet");
        table.getBody().bindChildren(planets,
                signal -> new TableRow(new TableDataCell(signal)));
        add(table);

        add(button("add", "Add", e -> planets
                .insertLast("Planet " + (planets.peek().size() + 1))));
        add(button("rename-first", "Rename first", e -> first()
                .ifPresent(entry -> entry.set(entry.peek() + " renamed"))));
        add(button("move-first-last", "Move first to last",
                e -> first().ifPresent(entry -> planets.moveTo(entry,
                        planets.peek().size() - 1))));
        add(button("remove-first", "Remove first",
                e -> first().ifPresent(planets::remove)));
        add(button("clear", "Clear", e -> planets.clear()));
    }

    private Optional<ValueSignal<String>> first() {
        return planets.peek().stream().findFirst();
    }

    private NativeButton button(String id, String caption,
            ComponentEventListener<ClickEvent<NativeButton>> listener) {
        NativeButton button = new NativeButton(caption, listener);
        button.setId(id);
        return button;
    }
}
