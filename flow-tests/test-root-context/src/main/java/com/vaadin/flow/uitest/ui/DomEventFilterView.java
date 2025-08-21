/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.KeyDownEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.DebouncePhase;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.DomEventFilterView", layout = ViewTestLayout.class)
public class DomEventFilterView extends AbstractDivView {

    @Tag("input")
    public static class DebounceComponent extends Component {
        public Registration addInputListener(
                ComponentEventListener<InputEvent> listener,
                int debounceTimeout) {
            return getEventBus().addListener(InputEvent.class, listener,
                    domReg -> domReg.debounce(debounceTimeout));
        }
    }

    @DomEvent("input")
    public static class InputEvent extends ComponentEvent<DebounceComponent> {
        private String value;

        public InputEvent(DebounceComponent source, boolean fromClient,
                @EventData("element.value") String value) {
            super(source, fromClient);
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private final Element messages = new Element("div");

    public DomEventFilterView() {
        Element space = new Element("input");
        space.setAttribute("id", "space");

        space.addEventListener("keypress",
                e -> addMessage("Space listener triggered"))
                .setFilter("event.key == ' ' || event.key == 'Spacebar'");
        // The key is called 'Spacebar' on IE11

        Element debounce = new Element("input");
        debounce.setAttribute("id", "debounce");
        debounce.addEventListener("input", e -> {
            addMessage("input:%s, phase:%s".formatted(
                    e.getEventData().get("element.value").asText(), e.getPhase()));
        }).addEventData("element.value").debounce(1000);
        debounce.addEventListener("click", e -> {
            addMessage("click");
        });

        Element leading = new Element("input");
        leading.setAttribute("id", "leading");
        leading.addEventListener("input", e -> {
            addMessage("input:%s, phase:%s".formatted(
                    e.getEventData().get("element.value").asText(), e.getPhase()));
        }).addEventData("element.value").debounce(1000, DebouncePhase.LEADING);

        Element leadingAndTrailing = new Element("input");
        leadingAndTrailing.setAttribute("id", "leading-trailing");
        leadingAndTrailing.addEventListener("input", e -> {
            addMessage("input:%s, phase:%s".formatted(
                    e.getEventData().get("element.value").asText(), e.getPhase()));
        }).addEventData("element.value").debounce(1000, DebouncePhase.LEADING,
                DebouncePhase.TRAILING);

        Element throttle = new Element("input");
        throttle.setAttribute("id", "throttle");
        throttle.addEventListener("input", e -> {
            addMessage("input:%s, phase:%s".formatted(
                    e.getEventData().get("element.value").asText(), e.getPhase()));
        }).addEventData("element.value").throttle(2000); // this is leading +
                                                         // intermediate
        throttle.addEventListener("click", e -> {
            addMessage("click");
        });

        Element godMode = new Element("input");
        godMode.setAttribute("id", "godMode");
        godMode.addEventListener("input", e -> {
            addMessage("godmode:%s, phase:%s".formatted(
                    e.getEventData().get("element.value").asText(), e.getPhase()));
        }).addEventData("element.value").debounce(1000, DebouncePhase.LEADING,
                DebouncePhase.TRAILING, DebouncePhase.INTERMEDIATE); // this is
                                                                     // leading
                                                                     // +

        Element twoEvents = new Element("input");
        twoEvents.setAttribute("id", "twoEvents");
        // keydown fires k-event always, g-event if g is pressed down
        twoEvents.executeJs("""
                        const el = this;
                        var id = 0;
                        this.addEventListener('keydown', function(event) {
                            id++;
                            const ke = new Event("k-event");
                            ke.id = id;
                            el.dispatchEvent(ke);

                            if(event.key == 'g') {
                                const ge = new Event("g-event");
                                ge.id = id;
                                el.dispatchEvent(ge);
                            }
                        });
                """);
        DomListenerRegistration keyreg = twoEvents.addEventListener("k-event",
                e -> {
                    addMessage(
                            "k-event " + e.getEventData().get("event.id").doubleValue()
                                    + " phase: " + e.getPhase());
                });
        keyreg.addEventData("event.id");
        // lazily listen k-events
        keyreg.debounce(3000);

        DomListenerRegistration greg = twoEvents.addEventListener("g-event",
                e -> {
                    addMessage("g-event "
                            + e.getEventData().get("event.id").doubleValue());
                });
        // this are listened eagerly, k-events should still come before
        greg.addEventData("event.id");

        DebounceComponent component = new DebounceComponent();
        component.setId("debounce-component");
        component.addInputListener(
                e -> addMessage("Component: " + e.getValue()), 2000);

        messages.setAttribute("id", "messages");
        getElement().appendChild(space, debounce, leading, leadingAndTrailing,
                throttle, godMode, twoEvents, component.getElement(), messages);

        // tests for#5090
        final AtomicReference<DomListenerRegistration> atomicReference = new AtomicReference<>();
        final Paragraph resultParagraph = new Paragraph();
        resultParagraph.setId("result-paragraph");

        NativeButton removalButton = new NativeButton("Remove DOM listener",
                event -> {
                    resultParagraph.setText("REMOVED");
                    atomicReference.get().remove();
                });
        removalButton.setId("listener-removal-button");

        Input listenerInput = new Input(ValueChangeMode.ON_CHANGE);
        listenerInput.setId("listener-input");

        /*
         * The event.preventDefault() is here to make sure that the listener has
         * been cleaned on the client-side as well. The server-side cleaning is
         * not really in question.
         */
        ComponentUtil.addListener(listenerInput, KeyDownEvent.class,
                event -> resultParagraph.setText("A"), registration -> {
                    atomicReference.set(registration);
                    registration.setFilter("event.key === 'a' && "
                            + "(event.preventDefault() || true)");
                });
        ComponentUtil.addListener(listenerInput, KeyDownEvent.class,
                event -> resultParagraph.setText("B"),
                registration -> registration.setFilter("event.key === 'b' && "
                        + "(event.preventDefault() || true)"));

        add(listenerInput, removalButton, resultParagraph);
    }

    private void addMessage(String message) {
        Element element = new Element("div");
        element.setText(message);
        messages.appendChild(element);
    }
}
