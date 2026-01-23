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
package com.vaadin.flow.component.html;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.Signal;
import com.vaadin.signals.ValueSignal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

/**
 * Unit tests for {@link HtmlObject#bindData(Signal)}.
 * <p>
 * This test focuses on binding semantics (double bind and manual set during
 * binding). It does not verify attach/detach lifecycle behavior which is
 * covered by Element/Signal integration tests.
 */
public class HtmlObjectBindDataTest {

    @Test
    public void bindData_doubleBind_throws() {
        HtmlObject object = new HtmlObject();
        ValueSignal<String> signal = new ValueSignal<>("");
        object.bindData(signal);
        ValueSignal<String> second = new ValueSignal<>("");
        assertThrows(BindingActiveException.class,
                () -> object.bindData(second));
    }

    @Test
    public void bindData_setWhileBound_succeeds_unbind_keepsValue() {
        HtmlObject object = new HtmlObject();
        ValueSignal<String> signal = new ValueSignal<>("/path/initial");
        object.bindData(signal);

        // Manual set via setData(String) while bound should NOT throw and
        // should update the attribute
        object.setData("/path/manual");
        assertEquals("/path/manual", object.getData());

        // Unbind using null and then set should still work
        object.bindData(null);
        object.setData("/path/ok");
        assertEquals("/path/ok", object.getData());
    }

    @Test
    public void constructor_withSignal_binds_setDataStillWorks() {
        var signal = new ValueSignal<>("/path/initial");
        var object = new HtmlObject(signal);

        // Manual set via setData(String) while bound should work (legacy
        // behavior)
        object.setData("/path/manual");
        assertEquals("/path/manual", object.getData());

        // Binding again should throw
        assertThrows(BindingActiveException.class,
                () -> object.bindData(new ValueSignal<>("/other")));
    }

    @Test
    public void constructor_withNullSignal_throwsNPE() {
        assertThrows(NullPointerException.class,
                () -> new HtmlObject((com.vaadin.signals.Signal<String>) null));
    }

    @Test
    public void constructor_withSignalAndType_setsTypeAndBinds() {
        var signal = new ValueSignal<>("/data");
        var type = "application/pdf";
        var object = new HtmlObject(signal, type);

        assertEquals(type, object.getType().orElse(null));

        // setData still works with binding present
        object.setData("/manual2");
        assertEquals("/manual2", object.getData());
    }

    @Test
    public void constructor_withSignalTypeAndParams_addsParams() {
        var p1 = new Param("a", "1");
        var p2 = new Param("b", "2");
        var object = new HtmlObject(new ValueSignal<>("/data"),
                "text/html", p1, p2);

        List<?> children = object.getChildren().toList();
        assertEquals(2, children.size());
        assertEquals(p1, children.get(0));
        assertEquals(p2, children.get(1));
    }

    @Test
    public void constructor_withSignalAndParams_addsParams() {
        var p = new Param("x", "y");
        var object = new HtmlObject(new ValueSignal<>("/data"), p);

        var child = object.getChildren().findFirst().orElse(null);
        assertNotNull(child);
        assertEquals(p, child);
    }
}
