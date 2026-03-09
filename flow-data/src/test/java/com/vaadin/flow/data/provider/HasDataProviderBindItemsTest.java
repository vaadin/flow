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
package com.vaadin.flow.data.provider;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.binder.HasDataProvider;
import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.signals.shared.SharedListSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HasDataProviderBindItemsTest extends SignalsUnitTest {

    @Tag("test-component")
    private static class TestComponent extends Component
            implements HasDataProvider<String> {

        private DataProvider<String, ?> dataProvider;

        @Override
        public void setDataProvider(DataProvider<String, ?> dataProvider) {
            DataViewUtils.checkNoActiveItemsBinding(this);
            this.dataProvider = dataProvider;
        }

        public DataProvider<String, ?> getDataProvider() {
            return dataProvider;
        }
    }

    @Test
    public void bindItems_listSignal_setsDataProvider() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ListSignal<String> itemsSignal = new ListSignal<>();
        itemsSignal.insertLast("Item 1");
        itemsSignal.insertLast("Item 2");
        itemsSignal.insertLast("Item 3");

        component.bindItems(itemsSignal);

        assertNotNull(component.getDataProvider());
        var items = component.getDataProvider().fetch(new Query<>()).toList();
        assertEquals(3, items.size());
        assertEquals("Item 1", items.get(0));
        assertEquals("Item 2", items.get(1));
        assertEquals("Item 3", items.get(2));
    }

    @Test
    public void bindItems_emptyList_setsDataProvider() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ListSignal<String> itemsSignal = new ListSignal<>();

        component.bindItems(itemsSignal);

        assertNotNull(component.getDataProvider());
        var items = component.getDataProvider().fetch(new Query<>()).toList();
        assertEquals(0, items.size());
    }

    @Test
    public void bindItems_nullSignal_throwsException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        assertThrows(NullPointerException.class,
                () -> component.bindItems(null));
    }

    @Test
    public void bindItems_alreadyBound_throwsBindingActiveException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ListSignal<String> itemsSignal = new ListSignal<>();
        itemsSignal.insertLast("Item 1");

        component.bindItems(itemsSignal);

        // Second binding should throw BindingActiveException
        ListSignal<String> secondSignal = new ListSignal<>();
        secondSignal.insertLast("Item 2");

        assertThrows(com.vaadin.flow.signals.BindingActiveException.class,
                () -> component.bindItems(secondSignal));
    }

    @Test
    public void bindItems_sharedListSignal_setsDataProvider() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        SharedListSignal<String> itemsSignal = new SharedListSignal<>(
                String.class);
        itemsSignal.insertLast("Item 1");
        itemsSignal.insertLast("Item 2");

        component.bindItems(itemsSignal);

        assertNotNull(component.getDataProvider());
        var items = component.getDataProvider().fetch(new Query<>()).toList();
        assertEquals(2, items.size());
        assertEquals("Item 1", items.get(0));
        assertEquals("Item 2", items.get(1));
    }

    @Test
    public void bindItems_genericSignal_setsDataProvider() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        List<ValueSignal<String>> signalList = new ArrayList<>();
        signalList.add(new ValueSignal<>("Item 1"));
        signalList.add(new ValueSignal<>("Item 2"));

        var itemsSignal = new ValueSignal<>(signalList);

        component.bindItems(itemsSignal);

        assertNotNull(component.getDataProvider());
        var items = component.getDataProvider().fetch(new Query<>()).toList();
        assertEquals(2, items.size());
        assertEquals("Item 1", items.get(0));
        assertEquals("Item 2", items.get(1));
    }

    @Test
    public void setDataProvider_afterBindItems_throwsBindingActiveException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ListSignal<String> itemsSignal = new ListSignal<>();
        itemsSignal.insertLast("Item 1");

        component.bindItems(itemsSignal);

        assertThrows(com.vaadin.flow.signals.BindingActiveException.class,
                () -> component
                        .setDataProvider(DataProvider.ofItems("Item 2")));
    }

    @Test
    public void setItems_collection_afterBindItems_throwsBindingActiveException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ListSignal<String> itemsSignal = new ListSignal<>();
        itemsSignal.insertLast("Item 1");

        component.bindItems(itemsSignal);

        assertThrows(com.vaadin.flow.signals.BindingActiveException.class,
                () -> component.setItems(List.of("Item 2")));
    }

    @Test
    public void setItems_varargs_afterBindItems_throwsBindingActiveException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ListSignal<String> itemsSignal = new ListSignal<>();
        itemsSignal.insertLast("Item 1");

        component.bindItems(itemsSignal);

        assertThrows(com.vaadin.flow.signals.BindingActiveException.class,
                () -> component.setItems("Item 2", "Item 3"));
    }
}
