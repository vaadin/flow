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
package com.vaadin.flow.internal;

import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SignalFieldTransferTest extends SignalsUnitTest {

    @Tag("div")
    static class ViewWithValueSignal extends Component {
        ValueSignal<String> name = new ValueSignal<>("default");
    }

    @Tag("div")
    static class ViewWithListSignal extends Component {
        ListSignal<String> items = new ListSignal<>();
    }

    @Tag("div")
    static class ViewWithNullSignal extends Component {
        ValueSignal<String> name = null;
    }

    @Tag("div")
    static class ViewWithStaticSignal extends Component {
        static ValueSignal<String> shared = new ValueSignal<>("static");
        ValueSignal<String> name = new ValueSignal<>("default");
    }

    @Tag("div")
    static class BaseView extends Component {
        ValueSignal<Integer> count = new ValueSignal<>(0);
    }

    @Tag("div")
    static class DerivedView extends BaseView {
        ValueSignal<String> label = new ValueSignal<>("default");
    }

    @Tag("div")
    static class ViewWithExtraField extends Component {
        ValueSignal<String> name = new ValueSignal<>("default");
        ValueSignal<String> extra = new ValueSignal<>("extra-default");
    }

    @Tag("div")
    static class ViewWithoutExtraField extends Component {
        ValueSignal<String> name = new ValueSignal<>("default");
    }

    @Tag("div")
    static class ViewWithNonSignalField extends Component {
        String name = "not a signal";
    }

    @Test
    void valueSignalIsTransferred() {
        var oldView = new ViewWithValueSignal();
        oldView.name.set("updated");

        var newView = new ViewWithValueSignal();
        SignalFieldTransfer.transferLocalSignalValues(oldView, newView);

        assertEquals("updated", newView.name.peek());
    }

    @Test
    void listSignalEntriesAreTransferred() {
        var oldView = new ViewWithListSignal();
        oldView.items.insertLast("a");
        oldView.items.insertLast("b");
        oldView.items.insertLast("c");

        var newView = new ViewWithListSignal();
        SignalFieldTransfer.transferLocalSignalValues(oldView, newView);

        var values = newView.items.peek().stream().map(ValueSignal::peek)
                .collect(Collectors.toList());
        assertEquals(java.util.List.of("a", "b", "c"), values);
    }

    @Test
    void nullSignalFieldIsSkipped() {
        var oldView = new ViewWithNullSignal();
        var newView = new ViewWithNullSignal();

        SignalFieldTransfer.transferLocalSignalValues(oldView, newView);

        assertNull(newView.name);
    }

    @Test
    void staticFieldsAreIgnored() {
        var oldView = new ViewWithStaticSignal();
        oldView.name.set("instance-value");
        ViewWithStaticSignal.shared.set("modified-static");

        var newView = new ViewWithStaticSignal();
        SignalFieldTransfer.transferLocalSignalValues(oldView, newView);

        assertEquals("instance-value", newView.name.peek());
    }

    @Test
    void inheritedSignalFieldsAreTransferred() {
        var oldView = new DerivedView();
        oldView.count.set(42);
        oldView.label.set("hello");

        var newView = new DerivedView();
        SignalFieldTransfer.transferLocalSignalValues(oldView, newView);

        assertEquals(42, newView.count.peek());
        assertEquals("hello", newView.label.peek());
    }

    @Test
    void missingFieldInOldInstanceIsSkipped() {
        var oldView = new ViewWithoutExtraField();
        oldView.name.set("transferred");

        var newView = new ViewWithExtraField();
        SignalFieldTransfer.transferLocalSignalValues(oldView, newView);

        assertEquals("transferred", newView.name.peek());
        assertEquals("extra-default", newView.extra.peek());
    }

    @Test
    void typeMismatchIsHandledGracefully() {
        var oldView = new ViewWithNonSignalField();
        var newView = new ViewWithValueSignal();

        SignalFieldTransfer.transferLocalSignalValues(oldView, newView);

        assertEquals("default", newView.name.peek());
    }
}
