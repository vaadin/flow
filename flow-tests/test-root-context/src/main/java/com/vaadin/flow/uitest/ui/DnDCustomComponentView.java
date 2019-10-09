/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import java.util.stream.Stream;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.dnd.DragEndEvent;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DragStartEvent;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.DnDCustomComponentView", layout = ViewTestLayout.class)
public class DnDCustomComponentView extends Div {

    private final Div dropTarget;

    public class DraggableItem extends Div
            implements DragSource<DraggableItem> {

        private final Div dragHandle;

        public DraggableItem(EffectAllowed effectAllowed) {
            dragHandle = new Div();
            dragHandle.setHeight("50px");
            dragHandle.setWidth("80px");
            dragHandle.getStyle().set("background", "#000 ");
            dragHandle.getStyle().set("margin", "5 10px");
            dragHandle.getStyle().set("display", "inline-block");

            setDraggable(true);
            setDragData(effectAllowed);
            addDragStartListener(DnDCustomComponentView.this::onDragStart);
            addDragEndListener(DnDCustomComponentView.this::onDragEnd);

            setHeight("50px");
            setWidth("200px");
            getStyle().set("border", "1px solid black");
            getStyle().set("display", "inline-block");

            add(dragHandle, new Span(effectAllowed.toString()));
        }

        @Override
        public Element getDraggableElement() {
            return dragHandle.getElement();
        }
    }

    public DnDCustomComponentView() {
        Stream.of(EffectAllowed.values()).map(DraggableItem::new)
                .forEach(this::add);

        dropTarget = new Div();
        dropTarget.add(new Text("Drop Here"));
        dropTarget.setWidth("200px");
        dropTarget.setHeight("200px");
        dropTarget.getStyle().set("border", "solid 1px pink");
        add(dropTarget);

        DropTarget.create(dropTarget).addDropListener(event -> event.getSource()
                .add(new Span(event.getDragData().get().toString())));
    }

    private void onDragStart(DragStartEvent<DraggableItem> event) {
        dropTarget.getStyle().set("background-color", "lightgreen");
    }

    private void onDragEnd(DragEndEvent<DraggableItem> event) {
        dropTarget.getStyle().remove("background-color");
    }

}
