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

package com.vaadin.flow.component.dnd;

import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dnd.internal.DndUtil;
import com.vaadin.flow.router.RouterLink;
import org.junit.Assert;
import org.junit.Test;

public class DragSourceTest extends AbstractDnDUnitTest {

    @Tag("div")
    class TestComponent extends Component implements DragSource<TestComponent> {

    }

    @Override
    protected void runStaticCreateMethodForExtension(Component component) {
        DragSource.create(component);
    }

    @Test
    public void testDragSource_mixinInterface() {
        TestComponent component = new TestComponent();
        ui.add(component);
        component.setDraggable(true);

        Assert.assertEquals("component element not set draggable", "true",
                component.getElement().getProperty("draggable"));

        AtomicReference<DragStartEvent<TestComponent>> startEventCapture = new AtomicReference<>();
        AtomicReference<DragEndEvent<TestComponent>> endEventCapture = new AtomicReference<>();
        component.addDragStartListener(startEventCapture::set);
        component.addDragEndListener(endEventCapture::set);

        DragStartEvent<TestComponent> startEvent = new DragStartEvent<TestComponent>(
                component, true);
        ComponentUtil.fireEvent(component, startEvent);

        Assert.assertEquals(startEvent, startEventCapture.get());
        Assert.assertEquals(component,
                UI.getCurrent().getActiveDragSourceComponent());
        Assert.assertTrue(startEvent.isFromClient());

        DragEndEvent<TestComponent> endEvent = new DragEndEvent<TestComponent>(
                component, false, DropEffect.MOVE.name().toLowerCase());
        ComponentUtil.fireEvent(component, endEvent);

        DragEndEvent<TestComponent> endEvent2 = endEventCapture.get();
        Assert.assertEquals(endEvent, endEvent2);
        Assert.assertNull(UI.getCurrent().getActiveDragSourceComponent());
        Assert.assertEquals(DropEffect.MOVE, endEvent2.getDropEffect());
        Assert.assertTrue(endEvent2.isSuccesful());
        Assert.assertFalse(endEvent2.isFromClient());
    }

    @Test
    public void testDragSource_dragStartEvent_canSetDragData() {
        TestComponent component = new TestComponent();
        ui.add(component);
        component.setDraggable(true);

        final String dragData = "FOOBAR";
        component.addDragStartListener(event -> {
            event.setDragData(dragData);
        });
        component.addDragEndListener(DragEndEvent::clearDragData);

        Assert.assertNull(component.getDragData());

        DragStartEvent<TestComponent> startEvent = new DragStartEvent<TestComponent>(
                component, true);
        ComponentUtil.fireEvent(component, startEvent);

        Assert.assertEquals("Drag data not set from event", dragData,
                component.getDragData());

        ComponentUtil.fireEvent(component,
                new DragEndEvent<>(component, true, "none"));

        Assert.assertNull(component.getDragData());
    }

    @Test
    public void testDragSource_staticBuilder_wrapsComponent() {
        RouterLink component = new RouterLink();

        DragSource<RouterLink> dragSource = DragSource.create(component);

        Assert.assertEquals("component element not set draggable", "true",
                component.getElement().getProperty("draggable"));

        Assert.assertEquals(component, dragSource.getDragSourceComponent());
        Assert.assertEquals(EffectAllowed.UNINITIALIZED,
                dragSource.getEffectAllowed());

        dragSource.setEffectAllowed(EffectAllowed.COPY_MOVE);

        Assert.assertEquals(
                component.getElement()
                        .getProperty(DndUtil.EFFECT_ALLOWED_ELEMENT_PROPERTY),
                EffectAllowed.COPY_MOVE.getClientPropertyValue());

        DragSource.configure(component, false);
        Assert.assertNull(component.getElement().getProperty("draggable"));

        DropTarget.configure(component);
        Assert.assertNull(component.getElement().getProperty("draggable"));

        DragSource.configure(component, true);
        Assert.assertEquals("component element not set draggable", "true",
                component.getElement().getProperty("draggable"));

        DropTarget.configure(component);
        Assert.assertEquals("component element not set draggable", "true",
                component.getElement().getProperty("draggable"));
    }

    @Test
    public void testDragSource_serverSideEvents_correctData() {
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

        Assert.assertEquals(startEvent, startEventCapture.get());
        Assert.assertEquals(component,
                UI.getCurrent().getActiveDragSourceComponent());
        Assert.assertTrue(startEvent.isFromClient());

        DragEndEvent<RouterLink> endEvent = new DragEndEvent<RouterLink>(
                component, false, DropEffect.MOVE.name().toLowerCase());
        ComponentUtil.fireEvent(component, endEvent);

        DragEndEvent<RouterLink> endEvent2 = endEventCapture.get();
        Assert.assertEquals(endEvent, endEvent2);
        Assert.assertNull(UI.getCurrent().getActiveDragSourceComponent());
        Assert.assertEquals(DropEffect.MOVE, endEvent2.getDropEffect());
        Assert.assertTrue(endEvent2.isSuccesful());
        Assert.assertFalse(endEvent2.isFromClient());

        endEvent = new DragEndEvent<RouterLink>(component, true, "None");
        ComponentUtil.fireEvent(component, endEvent);

        endEvent2 = endEventCapture.get();
        Assert.assertEquals(endEvent, endEvent2);
        Assert.assertNull(UI.getCurrent().getActiveDragSourceComponent());
        Assert.assertEquals(DropEffect.NONE, endEvent2.getDropEffect());
        Assert.assertFalse(endEvent2.isSuccesful());
        Assert.assertTrue(endEvent2.isFromClient());
    }

    @Test(expected = IllegalStateException.class)
    public void testDragSource_notAttachedToUIAndCatchesDragStartEvent_throws() {
        RouterLink component = new RouterLink();
        DragSource<RouterLink> dragSource = DragSource.create(component);

        DragStartEvent<RouterLink> startEvent = new DragStartEvent<RouterLink>(
                component, true);
        ComponentUtil.fireEvent(component, startEvent);
    }

    @Test
    public void testDragSource_notAttachedToUIAndCatchesDragEndEvent_doesNotThrow() {
        RouterLink component = new RouterLink();
        ui.add(component);
        DragSource<RouterLink> dragSource = DragSource.create(component);

        DragStartEvent<RouterLink> startEvent = new DragStartEvent<RouterLink>(
                component, true);
        ComponentUtil.fireEvent(component, startEvent);
        Assert.assertEquals(component, ui.getActiveDragSourceComponent());

        // the drop event could remove the component if in same UI
        ui.remove(component);

        DragEndEvent<RouterLink> endEvent = new DragEndEvent<>(component, true,
                "move");
        // should not throw for removing the active drag source
        ComponentUtil.fireEvent(component, endEvent);

        Assert.assertNull(ui.getActiveDragSourceComponent());
    }

}
