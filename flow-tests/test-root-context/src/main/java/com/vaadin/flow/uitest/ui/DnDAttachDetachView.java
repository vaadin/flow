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

import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropEvent;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

/* https://github.com/vaadin/flow/issues/6054 */
@Route(value = "com.vaadin.flow.uitest.ui.DnDAttachDetachView")
public class DnDAttachDetachView extends Div {

    public static final String DRAGGABLE_ID = "draggable";
    public static final String VIEW_1_ID = "view1";
    public static final String VIEW_2_ID = "view2";
    public static final String SWAP_BUTTON_ID = "swap-button";
    public static final String MOVE_BUTTON_ID = "move-button";
    private Div buttonSwitchViews = new Div();
    private Div buttonRemoveAdd = new Div();
    private Div view1 = new Div();
    private Div view2 = new Div();
    private int counter = 0;

    public DnDAttachDetachView() {
        setSizeFull();

        buttonSwitchViews.setText("Click to detach OR attach");
        buttonSwitchViews.setId(SWAP_BUTTON_ID);
        buttonSwitchViews.getStyle().set("border", "1px solid black");
        buttonSwitchViews.setHeight("20px");
        buttonSwitchViews.setWidth("200px");

        buttonRemoveAdd.setText("Click to detach AND attach");
        buttonRemoveAdd.setId(MOVE_BUTTON_ID);
        buttonRemoveAdd.getStyle().set("border", "1px solid black");
        buttonRemoveAdd.setHeight("20px");
        buttonRemoveAdd.setWidth("200px");

        add(buttonSwitchViews, buttonRemoveAdd);

        Div div = new Div();
        div.setText("Text To Drag");
        div.setId(DRAGGABLE_ID);
        div.getStyle().set("background-color", "grey");
        add(div);

        add(view1);
        view1.setWidth("500px");
        view1.setHeight("500px");
        view1.getStyle().set("background-color", "pink");
        view1.setId(VIEW_1_ID);

        view2.setWidth("500px");
        view2.setHeight("500px");
        view2.setId(VIEW_2_ID);

        // need to set the effect allowed and drop effect for the simulation
        DragSource<Div> dragSource = DragSource.create(div);
        dragSource.addDragStartListener(
                event -> add(new Span("Start: " + counter)));
        dragSource.setEffectAllowed(EffectAllowed.COPY);

        DropTarget<Div> dt = DropTarget.create(view1);
        dt.setDropEffect(DropEffect.COPY);

        buttonSwitchViews.addClickListener(event -> {
            if (getChildren().anyMatch(component -> component == view1)) {
                remove(div, view1);
                add(view2);
            } else {
                remove(view2);
                add(div, view1);
            }
        });
        buttonRemoveAdd.addClickListener(event -> {
            remove(div, view1);
            add(div, view1);
        });
        DropTarget.configure(view1).addDropListener(this::onDrop);
    }

    private void onDrop(DropEvent<Div> divDropEvent) {
        Span span = new Span("Drop: " + counter);
        span.setId("drop-" + counter);
        add(span);
        counter++;
    }
}
