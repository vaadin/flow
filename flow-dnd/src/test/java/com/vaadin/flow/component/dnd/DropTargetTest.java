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
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dnd.internal.DndUtil;
import com.vaadin.flow.router.RouterLink;
import org.junit.Assert;
import org.junit.Test;

public class DropTargetTest extends AbstractDnDUnitTest {

    @Tag("div")
    class TestComponent extends Component
            implements DropTarget<TestComponent>, HasComponents {

    }

    @Override
    protected void runStaticCreateMethodForExtension(Component component) {
        DropTarget.create(component);
    }

    @Test
    public void testDropTarget_mixinInterface() {
        TestComponent component = new TestComponent();
        ui.add(component);

        component.setActive(true);
        Assert.assertTrue(component.getElement()
                .getProperty(DndUtil.DROP_TARGET_ACTIVE_PROPERTY, false));

        AtomicReference<DropEvent<TestComponent>> eventCapture = new AtomicReference<>();
        component.addDropListener(eventCapture::set);

        DropEvent<TestComponent> dropEvent = new DropEvent<>(component, true,
                "all");
        ComponentUtil.fireEvent(component, dropEvent);

        DropEvent<TestComponent> actualEvent = eventCapture.get();
        Assert.assertEquals(dropEvent, actualEvent);
        Assert.assertTrue(actualEvent.isFromClient());
        Assert.assertEquals(component, actualEvent.getComponent());
        Assert.assertEquals(EffectAllowed.ALL, actualEvent.getEffectAllowed());
        Assert.assertEquals(null, actualEvent.getDropEffect());
        Assert.assertFalse(actualEvent.getDragData().isPresent());
    }

    @Test
    public void testDropTarget_staticBuilder_wrapsComponent() {
        RouterLink component = new RouterLink();
        DropTarget<RouterLink> dropTarget = DropTarget.create(component);

        Assert.assertTrue(component.getElement()
                .getProperty(DndUtil.DROP_TARGET_ACTIVE_PROPERTY, false));
        Assert.assertNull(dropTarget.getDropEffect());

        DropTarget.configure(component, false);
        Assert.assertFalse(component.getElement()
                .getProperty(DndUtil.DROP_TARGET_ACTIVE_PROPERTY, false));

        DropTarget.configure(component);
        Assert.assertFalse(component.getElement()
                .getProperty(DndUtil.DROP_TARGET_ACTIVE_PROPERTY, false));

        DropTarget.configure(component, true);
        Assert.assertTrue(component.getElement()
                .getProperty(DndUtil.DROP_TARGET_ACTIVE_PROPERTY, false));

        DropTarget.configure(component);
        Assert.assertTrue(component.getElement()
                .getProperty(DndUtil.DROP_TARGET_ACTIVE_PROPERTY, false));
    }

    @Test
    public void testDropTarget_dropListener_correctData() {
        RouterLink component = new RouterLink();
        ui.add(component);
        DropTarget<RouterLink> dropTarget = DropTarget.create(component);

        AtomicReference<DropEvent<RouterLink>> eventCapture = new AtomicReference<>();
        dropTarget.addDropListener(eventCapture::set);

        DropEvent<RouterLink> dropEvent = new DropEvent<>(component, true,
                "all");
        ComponentUtil.fireEvent(component, dropEvent);

        DropEvent<RouterLink> actualEvent = eventCapture.get();
        Assert.assertEquals(dropEvent, actualEvent);
        Assert.assertTrue(actualEvent.isFromClient());
        Assert.assertEquals(component, actualEvent.getComponent());
        Assert.assertEquals(EffectAllowed.ALL, actualEvent.getEffectAllowed());
        Assert.assertEquals(null, actualEvent.getDropEffect());
        Assert.assertFalse(actualEvent.getDragData().isPresent());

        dropTarget.setDropEffect(DropEffect.COPY);
        dropEvent = new DropEvent<>(component, false, "copymove");
        ComponentUtil.fireEvent(component, dropEvent);

        actualEvent = eventCapture.get();
        Assert.assertEquals(dropEvent, actualEvent);
        Assert.assertFalse(actualEvent.isFromClient());
        Assert.assertEquals(component, actualEvent.getComponent());
        Assert.assertEquals(EffectAllowed.COPY_MOVE,
                actualEvent.getEffectAllowed());
        Assert.assertEquals(DropEffect.COPY, actualEvent.getDropEffect());
        Assert.assertFalse(actualEvent.getDragData().isPresent());
        Assert.assertFalse(actualEvent.getDragSourceComponent().isPresent());
    }

    @Test
    public void testDragAndDrop_serverSideDragData() {
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
        Assert.assertEquals(dropEvent, actualEvent);
        Assert.assertEquals("FOOBAR", actualEvent.getDragData().orElse(null));
        Assert.assertEquals(source,
                actualEvent.getDragSourceComponent().orElse(null));

        // not firing end event for this test but it should be fine as the drag
        // data and active source is overridden
        ComponentUtil.fireEvent(source, new DragStartEvent<>(source, true));
        dragSource.setDragData("another");
        ComponentUtil.fireEvent(target, dropEvent);

        actualEvent = eventCapture.get();
        Assert.assertEquals(dropEvent, actualEvent);
        Assert.assertEquals("another", actualEvent.getDragData().orElse(null));
        Assert.assertEquals(source,
                actualEvent.getDragSourceComponent().orElse(null));
    }

    @Test(expected = IllegalStateException.class)
    public void testDropTarget_notAttachedToUIAndReceivesDropEvent_throws() {
        RouterLink component = new RouterLink();
        DropTarget<RouterLink> dropTarget = DropTarget.create(component);

        DropEvent<RouterLink> dropEvent = new DropEvent<>(component, true,
                "all");
        ComponentUtil.fireEvent(component, dropEvent);
    }

    @Test
    public void testDropTarget_dropInSideSameUI_moveComponentToTargetInDropEvent() {
        RouterLink source = new RouterLink();
        TestComponent target = new TestComponent();
        ui.add(source, target);

        DragSource<RouterLink> dragSource = DragSource.create(source);
        DropTarget<TestComponent> dropTarget = DropTarget.create(target);

        DragStartEvent<RouterLink> startEvent = new DragStartEvent<RouterLink>(
                source, true);
        ComponentUtil.fireEvent(source, startEvent);
        Assert.assertEquals(source, ui.getActiveDragSourceComponent());

        ComponentEventListener<DropEvent<TestComponent>> dropListener = event -> {
            event.getDragSourceComponent().ifPresent(dragSourceComponent -> {
                TestComponent component = event.getComponent();

                Assert.assertEquals("Invalid drag source component",
                        dragSourceComponent, source);
                Assert.assertEquals("Invalid event source", component, target);

                target.add(dragSourceComponent);
            });
            // ELSE will be failed by the checks outside the listener
        };
        target.addDropListener(dropListener);

        DropEvent<TestComponent> dropEvent = new DropEvent<>(target, true,
                "all");
        ComponentUtil.fireEvent(target, dropEvent);

        Assert.assertEquals("Drag source component should have been moved",
                source.getParent().get(), target);

        DragEndEvent<RouterLink> endEvent = new DragEndEvent<>(source, true,
                "move");
        // should not throw for removing the active drag source
        ComponentUtil.fireEvent(source, endEvent);

        Assert.assertNull(ui.getActiveDragSourceComponent());
    }
}
