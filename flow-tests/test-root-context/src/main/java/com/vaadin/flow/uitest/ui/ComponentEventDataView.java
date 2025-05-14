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

import java.util.Objects;
import java.util.Optional;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ComponentEventDataView", layout = ViewTestLayout.class)
public class ComponentEventDataView extends AbstractEventDataView {

    public static final String CHILD_COMPONENT = "child-component";
    public static final String FIRST_CHILD = "first-child";
    public static final String HEADER_CLICKED = "header-clicked";

    public ComponentEventDataView() {
        Div childComponent = new Div();
        childComponent.getStyle().set("display", "list-item");
        childComponent.setId(CHILD_COMPONENT);
        Div clickedComponent = new Div();
        clickedComponent.getStyle().set("display", "list-item");
        clickedComponent.setId(TARGET_ID);
        Div firstChild = new Div();
        firstChild.getStyle().set("display", "list-item");
        firstChild.setId(FIRST_CHILD);
        Div headerClicked = new Div();
        headerClicked.getStyle().set("display", "list-item");
        headerClicked.setId(HEADER_CLICKED);
        add(new Text("direct of listener child based on event.target"),
                childComponent, new Text("event.target"), clickedComponent,
                new Text("event.currentTarget.children[0]"), firstChild,
                new Text("H3 if clicked"), headerClicked);

        addListener(LayoutClickEvent.class, event -> {
            childComponent.setText(event.getChildComponent()
                    .flatMap(Component::getId).orElse(EMPTY_VALUE));
            clickedComponent.setText(event.getClickedComponent()
                    .flatMap(Component::getId).orElse(EMPTY_VALUE));
            firstChild.setText(event.getFirstChild()
                    .map(element -> element.getAttribute("id"))
                    .orElse(EMPTY_VALUE));
            headerClicked.setText(event.header == null ? EMPTY_VALUE
                    : event.header.getText());
        });

        createComponents();
    }

    public static class LayoutClickEvent extends ClickEvent<Component> {

        private final Component clickedComponent;
        private final Component childComponent;
        private final Element firstChild;
        private final H3 header;

        public LayoutClickEvent(Component source, boolean fromClient,
                @EventData("event.screenX") int screenX,
                @EventData("event.screenY") int screenY,
                @EventData("event.clientX") int clientX,
                @EventData("event.clientY") int clientY,
                @EventData("event.detail") int clickCount,
                @EventData("event.button") int button,
                @EventData("event.ctrlKey") boolean ctrlKey,
                @EventData("event.shiftKey") boolean shiftKey,
                @EventData("event.altKey") boolean altKey,
                @EventData("event.metaKey") boolean metaKey,
                @EventData("event.target") Component clickedComponent,
                // this is just to test that element references work too
                @EventData("event.target.children[0]") Element firstChild,
                // testing that subtypes and null element works
                @EventData("event.target.tagName === 'H3' ? event.target : undefined") H3 header) {
            super(source, fromClient, screenX, screenY, clientX, clientY,
                    clickCount, button, ctrlKey, shiftKey, altKey, metaKey);

            this.clickedComponent = clickedComponent;
            this.firstChild = firstChild;
            this.header = header;
            // just to showcase that this is possible
            childComponent = getSourcesDirectChildWith(clickedComponent)
                    .orElse(null);
        }

        private Optional<Component> getSourcesDirectChildWith(
                Component component) {
            if (component == source) {
                return Optional.empty();
            }
            Optional<Component> potentialDirectChild = Optional
                    .ofNullable(component);
            while (potentialDirectChild.flatMap(Component::getParent)
                    .filter(that -> !Objects.equals(that, source))
                    .isPresent()) {
                potentialDirectChild = potentialDirectChild.get().getParent();
            }
            return potentialDirectChild;
        }

        public Optional<Component> getClickedComponent() {
            return Optional.ofNullable(clickedComponent);
        }

        public Optional<Component> getChildComponent() {
            return Optional.ofNullable(childComponent);
        }

        public Optional<Element> getFirstChild() {
            return Optional.ofNullable(firstChild);
        }
    }
}
