/*
 * Copyright 2000-2020 Vaadin Ltd.
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

@Route(value = "com.vaadin.flow.uitest.ui.DnDView", layout = ViewTestLayout.class)
public class DnDView extends Div {

    public static final String NO_EFFECT_SETUP = "no-effect";
    private Div eventLog;
    private int eventCounter = 0;

    private boolean data;

    public DnDView() {
        setWidth("1000px");
        setHeight("800px");
        getStyle().set("display", "flex");

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
        add(eventLog);

        Div startLane = createLane("start");
        startLane.add(createDraggableBox(null));
        Stream.of(EffectAllowed.values()).map(this::createDraggableBox)
                .forEach(startLane::add);

        Div noEffectLane = createDropLane(null);
        Div copyDropLane = createDropLane(DropEffect.COPY);
        Div moveDropLane = createDropLane(DropEffect.MOVE);
        Div linkDropLane = createDropLane(DropEffect.LINK);
        Div noneDropLane = createDropLane(DropEffect.NONE);

        Div deactivatedLane = createDropLane(DropEffect.COPY);
        deactivatedLane.setId("lane-deactivated");
        deactivatedLane.getChildren().findFirst().ifPresent(
                component -> component.getElement().setText("deactivated"));
        DropTarget.configure(deactivatedLane, false);

        eventLog.add(createToggleDropTargetsButton(noEffectLane, copyDropLane,
                moveDropLane, linkDropLane, noneDropLane));
        eventLog.add(createToggleDragSourcesButton(startLane));

        add(startLane, noEffectLane, copyDropLane, moveDropLane, linkDropLane,
                noneDropLane, deactivatedLane);
    }

    private void addLogEntry(String eventDetails) {
        Div div = new Div();
        eventCounter++;
        div.add(eventCounter + ": " + eventDetails);
        div.setId("event-" + eventCounter);
        eventLog.add(div);
    }

    private Component createToggleDragSourcesButton(Div startLane) {
        NativeButton nativeButton = new NativeButton("Disable DragSources",
                event -> {
            if (event.getSource().getText().equals("Enable DragSources")) {
                startLane.getChildren().iterator().forEachRemaining(box -> {
                    box.getElement().setEnabled(true);
                    addLogEntry(box.getId() + " enabled");
                });
                event.getSource().setText("Disable DragSources");
            } else {
                startLane.getChildren().iterator().forEachRemaining(box -> {
                    box.getElement().setEnabled(false);
                    addLogEntry(box.getId() + " disabled");
                });
                event.getSource().setText("Enable DragSources");
            }
        });
        nativeButton.setId("button-disable-enable-drag-sources");
        return nativeButton;
    }

    private Component createToggleDropTargetsButton(Div noEffectLane,
                                                    Div copyDropLane,
                                                    Div moveDropLane,
                                                    Div linkDropLane,
                                                    Div noneDropLane) {
        NativeButton nativeButton = new NativeButton("Disable DropTargets",
                event -> {
            if (event.getSource().getText().equals("Enable DropTargets")) {
                noEffectLane.setEnabled(true);
                addLogEntry(noEffectLane.getId().get() + " enabled");
                copyDropLane.setEnabled(true);
                addLogEntry(copyDropLane.getId().get() + " enabled");
                moveDropLane.setEnabled(true);
                addLogEntry(moveDropLane.getId().get() + " enabled");
                linkDropLane.setEnabled(true);
                addLogEntry(linkDropLane.getId().get() + " enabled");
                noneDropLane.setEnabled(true);
                addLogEntry(noneDropLane.getId().get() + " enabled");
                event.getSource().setText("Disable DropTargets");
            } else {
                noEffectLane.setEnabled(false);
                addLogEntry(noEffectLane.getId().get() + " disabled");
                copyDropLane.setEnabled(false);
                addLogEntry(copyDropLane.getId().get() + " disabled");
                moveDropLane.setEnabled(false);
                addLogEntry(moveDropLane.getId().get() + " disabled");
                linkDropLane.setEnabled(false);
                addLogEntry(linkDropLane.getId().get() + " disabled");
                noneDropLane.setEnabled(false);
                addLogEntry(noneDropLane.getId().get() + " disabled");
                event.getSource().setText("Enable DropTargets");
            }
        });
        nativeButton.setId("button-disable-enable-drop-targets");
        return nativeButton;
    }

    private Component createDraggableBox(EffectAllowed effectAllowed) {
        String identifier = effectAllowed == null ? NO_EFFECT_SETUP
                : effectAllowed.toString();

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

    private Div createBox(String identifier) {
        Div box = new Div();
        box.setText(identifier);
        box.setWidth("100px");
        box.setHeight("60px");
        box.getStyle().set("border", "1px solid").set("margin", "10px");
        box.setId("box-" + identifier);
        return box;
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
}
