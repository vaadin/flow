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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.DnDDisabledView", layout = ViewTestLayout.class)
public class DnDDisabledView extends Div {

    private Div eventLog;
    private int eventCounter = 0;

    private Component box;

    private boolean data;

    public DnDDisabledView() {
        setWidth("1000px");
        setHeight("800px");
        getStyle().set("display", "flex");

        createEventLog();
        add(eventLog);

        Div dropLane = createDropLane(DropEffect.MOVE);
        Div startLane = createLane("start");
        box = createDraggableBox(EffectAllowed.MOVE);
        startLane.add(createDisableButton(), createEnableButton(), box);

        add(startLane, dropLane);
        addLogEntry(box.getId() + "enabled!");
    }

    private Component createDisableButton() {
        NativeButton nativeButton = new NativeButton("Disable Box", event -> {
            box.getElement().setEnabled(false);
            addLogEntry(box.getId() + "disabled!");
        });
        return nativeButton;
    }

    private Component createEnableButton() {
        NativeButton nativeButton = new NativeButton("Enable Box", event -> {
            box.getElement().setEnabled(true);
            addLogEntry(box.getId() + "enabled!");
        });
        return nativeButton;
    }

    private Component createDraggableBox(EffectAllowed effectAllowed) {
        String identifier = "test";

        Div box = createBox(identifier);

        DragSource<Div> dragSource = DragSource.create(box);
        dragSource.setDraggable(true);
        if (effectAllowed != null) {
            dragSource.setEffectAllowed(effectAllowed);
        }
        dragSource.addDragStartListener(event -> {
            addLogEntry("Start: " + event.getComponent().getText());
            if (data) {
                dragSource.setDragData(identifier);
            }
        });
        dragSource.addDragEndListener(event -> {
            addLogEntry("End: " + event.getComponent().getText() + " "
                    + event.getDropEffect());
        });
        return box;
    }

    private Div createBox(String identifier) {
        Div box = new Div();
        box.setText(identifier);
        box.setWidth("100px");
        box.setHeight("60px");
        box.getStyle().set("border", "1px solid").set("margin", "10px");
        box.setId("box-" + identifier);
        return box;
    }

    private Div createDropLane(DropEffect dropEffect) {
        String identifier = dropEffect == null ? "no-effect"
                : dropEffect.toString();

        Div lane = createLane(identifier);

        DropTarget<Div> dropTarget = DropTarget.create(lane);
        dropTarget.setActive(true);
        if (dropEffect != null) {
            dropTarget.setDropEffect(dropEffect);
        }
        dropTarget.addDropListener(event -> addLogEntry("Drop: "
                + event.getEffectAllowed() + " " + event.getDropEffect()
                + (data ? (" " + event.getDragData()) : "")));

        return lane;
    }

    private void createEventLog() {
        eventLog = new Div();
        eventLog.add(new Text("Events:"));
        eventLog.add(new NativeButton("Clear", event -> {
            eventLog.getChildren().filter(component -> component instanceof Div)
                    .forEach(eventLog::remove);
            eventCounter = 0;
        }));
        eventLog.add(new NativeButton("Data: " + data, event -> {
            data = !data;
            event.getSource().setText("Data: " + data);
        }));
        eventLog.setHeightFull();
        eventLog.setWidth("400px");
        eventLog.getStyle().set("display", "inline-block").set("border",
                "2px " + "solid");
    }

    private Div createLane(String identifier) {
        Div lane = new Div();
        lane.add(identifier);
        lane.setId("lane-" + identifier);
        lane.getStyle().set("margin", "20px").set("border", "1px solid black")
                .set("display", "inline-block");
        lane.setHeightFull();
        lane.setWidth("150px");
        return lane;
    }

    private void addLogEntry(String eventDetails) {
        Div div = new Div();
        eventCounter++;
        div.add(eventCounter + ": " + eventDetails);
        div.setId("event-" + eventCounter);
        eventLog.add(div);
    }
}
