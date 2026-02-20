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
package com.vaadin.flow.component.dnd;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dnd.internal.DndUtil;
import com.vaadin.flow.router.RouterLink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DragSourceTest extends AbstractDnDUnitTest {

    @Tag("div")
    class TestComponent extends Component implements DragSource<TestComponent> {

    }

    @Override
    protected void runStaticCreateMethodForExtension(Component component) {
        DragSource.create(component);
    }

    @Test
    void testDragSource_mixinInterface() {
        TestComponent component = new TestComponent();
        ui.add(component);
        component.setDraggable(true);

        assertEquals("true", component.getElement().getProperty("draggable"),
                "component element not set draggable");

        AtomicReference<DragStartEvent<TestComponent>> startEventCapture = new AtomicReference<>();
        AtomicReference<DragEndEvent<TestComponent>> endEventCapture = new AtomicReference<>();
        component.addDragStartListener(startEventCapture::set);
        component.addDragEndListener(endEventCapture::set);

        DragStartEvent<TestComponent> startEvent = new DragStartEvent<TestComponent>(
                component, true);
        ComponentUtil.fireEvent(component, startEvent);

        assertEquals(startEvent, startEventCapture.get());
        assertEquals(component, UI.getCurrent().getActiveDragSourceComponent());
        assertTrue(startEvent.isFromClient());

        DragEndEvent<TestComponent> endEvent = new DragEndEvent<TestComponent>(
                component, false, DropEffect.MOVE.name().toLowerCase());
        ComponentUtil.fireEvent(component, endEvent);

        DragEndEvent<TestComponent> endEvent2 = endEventCapture.get();
        assertEquals(endEvent, endEvent2);
        assertNull(UI.getCurrent().getActiveDragSourceComponent());
        assertEquals(DropEffect.MOVE, endEvent2.getDropEffect());
        assertTrue(endEvent2.isSuccessful());
        assertFalse(endEvent2.isFromClient());
    }

    @Test
    void testDragSource_dragStartEvent_canSetDragData() {
        TestComponent component = new TestComponent();
        ui.add(component);
        component.setDraggable(true);

        final String dragData = "FOOBAR";
        component.addDragStartListener(event -> {
            event.setDragData(dragData);
        });
        component.addDragEndListener(DragEndEvent::clearDragData);

        assertNull(component.getDragData());

        DragStartEvent<TestComponent> startEvent = new DragStartEvent<TestComponent>(
                component, true);
        ComponentUtil.fireEvent(component, startEvent);

        assertEquals(dragData, component.getDragData(),
                "Drag data not set from event");

        ComponentUtil.fireEvent(component,
                new DragEndEvent<>(component, true, "none"));

        assertNull(component.getDragData());
    }

    @Test
    void testDragSource_staticBuilder_wrapsComponent() {
        RouterLink component = new RouterLink();

        DragSource<RouterLink> dragSource = DragSource.create(component);

        assertEquals("true", component.getElement().getProperty("draggable"),
                "component element not set draggable");

        assertEquals(component, dragSource.getDragSourceComponent());
        assertEquals(EffectAllowed.UNINITIALIZED,
                dragSource.getEffectAllowed());

        dragSource.setEffectAllowed(EffectAllowed.COPY_MOVE);

        assertEquals(
                component.getElement()
                        .getProperty(DndUtil.EFFECT_ALLOWED_ELEMENT_PROPERTY),
                EffectAllowed.COPY_MOVE.getClientPropertyValue());

        DragSource.configure(component, false);
        assertNull(component.getElement().getProperty("draggable"));

        DropTarget.configure(component);
        assertNull(component.getElement().getProperty("draggable"));

        DragSource.configure(component, true);
        assertEquals("true", component.getElement().getProperty("draggable"),
                "component element not set draggable");

        DropTarget.configure(component);
        assertEquals("true", component.getElement().getProperty("draggable"),
                "component element not set draggable");
    }

    @Test
    void testDragSource_serverSideEvents_correctData() {
        RouterLink component = new RouterLink();
        ui.add(component);
        DragSource<RouterLink> dragSource = DragSource.create(component);

        AtomicReference<DragStartEvent<RouterLink>> startEventCapture = new AtomicReference<>();
        AtomicReference<DragEndEvent<RouterLink>> endEventCapture = new AtomicReference<>();
        dragSource.addDragStartListener(startEventCapture::set);
        dragSource.addDragEndListener(endEventCapture::set);

        DragStartEvent<RouterLink> startEvent = new DragStartEvent<RouterLink>(
                component, true);
        ComponentUtil.fireEvent(component, startEvent);

        assertEquals(startEvent, startEventCapture.get());
        assertEquals(component, UI.getCurrent().getActiveDragSourceComponent());
        assertTrue(startEvent.isFromClient());

        DragEndEvent<RouterLink> endEvent = new DragEndEvent<RouterLink>(
                component, false, DropEffect.MOVE.name().toLowerCase());
        ComponentUtil.fireEvent(component, endEvent);

        DragEndEvent<RouterLink> endEvent2 = endEventCapture.get();
        assertEquals(endEvent, endEvent2);
        assertNull(UI.getCurrent().getActiveDragSourceComponent());
        assertEquals(DropEffect.MOVE, endEvent2.getDropEffect());
        assertTrue(endEvent2.isSuccessful());
        assertFalse(endEvent2.isFromClient());

        endEvent = new DragEndEvent<RouterLink>(component, true, "None");
        ComponentUtil.fireEvent(component, endEvent);

        endEvent2 = endEventCapture.get();
        assertEquals(endEvent, endEvent2);
        assertNull(UI.getCurrent().getActiveDragSourceComponent());
        assertEquals(DropEffect.NONE, endEvent2.getDropEffect());
        assertFalse(endEvent2.isSuccessful());
        assertTrue(endEvent2.isFromClient());
    }

    @Test
    void testDragSource_notAttachedToUIAndCatchesDragStartEvent_throws() {
        RouterLink component = new RouterLink();
        DragSource.create(component);

        DragStartEvent<RouterLink> startEvent = new DragStartEvent<RouterLink>(
                component, true);
        assertThrows(IllegalStateException.class,
                () -> ComponentUtil.fireEvent(component, startEvent));
    }

    @Test
    void testDragSource_notAttachedToUIAndCatchesDragEndEvent_doesNotThrow() {
        RouterLink component = new RouterLink();
        ui.add(component);
        DragSource<RouterLink> dragSource = DragSource.create(component);

        DragStartEvent<RouterLink> startEvent = new DragStartEvent<RouterLink>(
                component, true);
        ComponentUtil.fireEvent(component, startEvent);
        assertEquals(component, ui.getActiveDragSourceComponent());

        // the drop event could remove the component if in same UI
        ui.remove(component);

        DragEndEvent<RouterLink> endEvent = new DragEndEvent<>(component, true,
                "move");
        // should not throw for removing the active drag source
        ComponentUtil.fireEvent(component, endEvent);

        assertNull(ui.getActiveDragSourceComponent());
    }

}
