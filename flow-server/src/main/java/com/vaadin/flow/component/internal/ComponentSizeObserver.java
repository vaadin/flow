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
package com.vaadin.flow.component.internal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Per-UI shared ResizeObserver manager that tracks component sizes using a
 * single browser {@code ResizeObserver} instance.
 * <p>
 * One instance is created per UI, lazily via {@link #get(UI)} when the first
 * component's size is observed. A single browser {@code ResizeObserver} is used
 * to track all observed elements, dispatching a custom
 * {@code "vaadin-component-resize"} event on the UI element with aggregated
 * size data.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class ComponentSizeObserver implements Serializable {

    private static final String EVENT_NAME = "vaadin-component-resize";

    private final Element uiElement;
    private final Map<Integer, ValueSignal<Component.Size>> idToSignal = new HashMap<>();
    private final Map<ValueSignal<Component.Size>, Integer> signalToId = new HashMap<>();
    private int nextId = 0;

    /**
     * Returns the ComponentSizeObserver for the given UI, creating it lazily.
     *
     * @param ui
     *            the UI to get the observer for
     * @return the observer instance
     */
    public static ComponentSizeObserver get(UI ui) {
        ComponentSizeObserver observer = ComponentUtil.getData(ui,
                ComponentSizeObserver.class);
        if (observer == null) {
            observer = new ComponentSizeObserver(ui);
            ComponentUtil.setData(ui, ComponentSizeObserver.class, observer);
        }
        return observer;
    }

    private ComponentSizeObserver(UI ui) {
        this.uiElement = ui.getElement();

        uiElement.executeJs(
                "window.Vaadin.Flow.componentSizeObserver.init(this)");

        uiElement.addEventListener(EVENT_NAME, event -> {
            ObjectNode sizes = (ObjectNode) event.getEventData()
                    .get("event.sizes");
            for (String idStr : sizes.propertyNames()) {
                int id = Integer.parseInt(idStr);
                ValueSignal<Component.Size> signal = idToSignal.get(id);
                if (signal != null) {
                    ObjectNode size = (ObjectNode) sizes.get(idStr);
                    int w = size.get("w").intValue();
                    int h = size.get("h").intValue();
                    signal.set(new Component.Size(w, h));
                }
            }
        }).addEventData("event.sizes").debounce(100).allowInert();
    }

    /**
     * Starts observing the given element and updates the signal with size
     * changes.
     *
     * @param element
     *            the element to observe
     * @param signal
     *            the signal to update
     */
    public void observe(Element element, ValueSignal<Component.Size> signal) {
        int id = nextId++;
        idToSignal.put(id, signal);
        signalToId.put(signal, id);

        uiElement.executeJs(
                "window.Vaadin.Flow.componentSizeObserver.observe(this, $0, $1)",
                element, id);
    }

    /**
     * Stops observing the component associated with the given signal.
     *
     * @param signal
     *            the signal whose component should stop being observed
     */
    public void unobserve(ValueSignal<Component.Size> signal) {
        Integer id = signalToId.remove(signal);
        if (id != null) {
            idToSignal.remove(id);
        }
    }
}
