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
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dnd.internal.DndUtil;
import com.vaadin.flow.router.RouterLink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DropTargetTest extends AbstractDnDUnitTest {

    @Tag("div")
    class TestComponent extends Component
            implements DropTarget<TestComponent>, HasComponents {

    }

    @Override
    protected void runStaticCreateMethodForExtension(Component component) {
        DropTarget.create(component);
    }

    @Test
    void testDropTarget_mixinInterface() {
        TestComponent component = new TestComponent();
        ui.add(component);

        component.setActive(true);
        assertTrue(component.getElement()
                .getProperty(DndUtil.DROP_TARGET_ACTIVE_PROPERTY, false));

        AtomicReference<DropEvent<TestComponent>> eventCapture = new AtomicReference<>();
        component.addDropListener(eventCapture::set);

        DropEvent<TestComponent> dropEvent = new DropEvent<>(component, true,
                "all");
        ComponentUtil.fireEvent(component, dropEvent);

        DropEvent<TestComponent> actualEvent = eventCapture.get();
        assertEquals(dropEvent, actualEvent);
        assertTrue(actualEvent.isFromClient());
        assertEquals(component, actualEvent.getComponent());
        assertEquals(EffectAllowed.ALL, actualEvent.getEffectAllowed());
        assertEquals(null, actualEvent.getDropEffect());
        assertFalse(actualEvent.getDragData().isPresent());
    }

    @Test
    void testDropTarget_staticBuilder_wrapsComponent() {
        RouterLink component = new RouterLink();
        DropTarget<RouterLink> dropTarget = DropTarget.create(component);

        assertTrue(component.getElement()
                .getProperty(DndUtil.DROP_TARGET_ACTIVE_PROPERTY, false));
        assertNull(dropTarget.getDropEffect());

        DropTarget.configure(component, false);
        assertFalse(component.getElement()
                .getProperty(DndUtil.DROP_TARGET_ACTIVE_PROPERTY, false));

        DropTarget.configure(component);
        assertFalse(component.getElement()
                .getProperty(DndUtil.DROP_TARGET_ACTIVE_PROPERTY, false));

        DropTarget.configure(component, true);
        assertTrue(component.getElement()
                .getProperty(DndUtil.DROP_TARGET_ACTIVE_PROPERTY, false));

        DropTarget.configure(component);
        assertTrue(component.getElement()
                .getProperty(DndUtil.DROP_TARGET_ACTIVE_PROPERTY, false));
    }

    @Test
    void testDropTarget_dropListener_correctData() {
        RouterLink component = new RouterLink();
        ui.add(component);
        DropTarget<RouterLink> dropTarget = DropTarget.create(component);

        AtomicReference<DropEvent<RouterLink>> eventCapture = new AtomicReference<>();
        dropTarget.addDropListener(eventCapture::set);

        DropEvent<RouterLink> dropEvent = new DropEvent<>(component, true,
                "all");
        ComponentUtil.fireEvent(component, dropEvent);

        DropEvent<RouterLink> actualEvent = eventCapture.get();
        assertEquals(dropEvent, actualEvent);
        assertTrue(actualEvent.isFromClient());
        assertEquals(component, actualEvent.getComponent());
        assertEquals(EffectAllowed.ALL, actualEvent.getEffectAllowed());
        assertEquals(null, actualEvent.getDropEffect());
        assertFalse(actualEvent.getDragData().isPresent());

        dropTarget.setDropEffect(DropEffect.COPY);
        dropEvent = new DropEvent<>(component, false, "copymove");
        ComponentUtil.fireEvent(component, dropEvent);

        actualEvent = eventCapture.get();
        assertEquals(dropEvent, actualEvent);
        assertFalse(actualEvent.isFromClient());
        assertEquals(component, actualEvent.getComponent());
        assertEquals(EffectAllowed.COPY_MOVE, actualEvent.getEffectAllowed());
        assertEquals(DropEffect.COPY, actualEvent.getDropEffect());
        assertFalse(actualEvent.getDragData().isPresent());
        assertFalse(actualEvent.getDragSourceComponent().isPresent());
    }

    @Test
    void testDragAndDrop_serverSideDragData() {
        RouterLink source = new RouterLink();
        ui.add(source);
        DragSource<RouterLink> dragSource = DragSource.create(source);

        RouterLink target = new RouterLink();
        ui.add(target);
        DropTarget<RouterLink> dropTarget = DropTarget.create(target);

        dragSource.setDragData("FOOBAR");

        AtomicReference<DropEvent<RouterLink>> eventCapture = new AtomicReference<>();
        dropTarget.addDropListener(eventCapture::set);

        ComponentUtil.fireEvent(source, new DragStartEvent<>(source, true));

        DropEvent<RouterLink> dropEvent = new DropEvent<>(target, true, "all");
        ComponentUtil.fireEvent(target, dropEvent);

        DropEvent<RouterLink> actualEvent = eventCapture.get();
        assertEquals(dropEvent, actualEvent);
        assertEquals("FOOBAR", actualEvent.getDragData().orElse(null));
        assertEquals(source, actualEvent.getDragSourceComponent().orElse(null));

        // not firing end event for this test but it should be fine as the drag
        // data and active source is overridden
        ComponentUtil.fireEvent(source, new DragStartEvent<>(source, true));
        dragSource.setDragData("another");
        ComponentUtil.fireEvent(target, dropEvent);

        actualEvent = eventCapture.get();
        assertEquals(dropEvent, actualEvent);
        assertEquals("another", actualEvent.getDragData().orElse(null));
        assertEquals(source, actualEvent.getDragSourceComponent().orElse(null));
    }

    @Test
    void testDropTarget_notAttachedToUIAndReceivesDropEvent_throws() {
        RouterLink component = new RouterLink();
        DropTarget.create(component);

        assertThrows(IllegalStateException.class,
                () -> new DropEvent<>(component, true, "all"));
    }

    @Test
    void testDropTarget_dropInSideSameUI_moveComponentToTargetInDropEvent() {
        RouterLink source = new RouterLink();
        TestComponent target = new TestComponent();
        ui.add(source, target);

        DragSource<RouterLink> dragSource = DragSource.create(source);
        DropTarget<TestComponent> dropTarget = DropTarget.create(target);

        DragStartEvent<RouterLink> startEvent = new DragStartEvent<RouterLink>(
                source, true);
        ComponentUtil.fireEvent(source, startEvent);
        assertEquals(source, ui.getActiveDragSourceComponent());

        ComponentEventListener<DropEvent<TestComponent>> dropListener = event -> {
            event.getDragSourceComponent().ifPresent(dragSourceComponent -> {
                TestComponent component = event.getComponent();

                assertEquals(dragSourceComponent, source,
                        "Invalid drag source component");
                assertEquals(component, target, "Invalid event source");

                target.add(dragSourceComponent);
            });
            // ELSE will be failed by the checks outside the listener
        };
        target.addDropListener(dropListener);

        DropEvent<TestComponent> dropEvent = new DropEvent<>(target, true,
                "all");
        ComponentUtil.fireEvent(target, dropEvent);

        assertEquals(source.getParent().get(), target,
                "Drag source component should have been moved");

        DragEndEvent<RouterLink> endEvent = new DragEndEvent<>(source, true,
                "move");
        // should not throw for removing the active drag source
        ComponentUtil.fireEvent(source, endEvent);

        assertNull(ui.getActiveDragSourceComponent());
    }
}
