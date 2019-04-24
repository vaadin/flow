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

package com.vaadin.flow.component;

import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DragStartEvent;
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropEvent;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.Constants;
import com.vaadin.tests.util.MockUI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DropTargetTest {

    private MockUI ui;

    @Before
    public void setup() {
        ui = new MockUI();
    }

    @Test
    public void testDropTarget_staticBuilder_wrapsComponent() {
        RouterLink component = new RouterLink();
        DropTarget<RouterLink> dropTarget = DropTarget.of(component);

        Assert.assertTrue(component.getElement()
                .getProperty(Constants.DROP_TARGET_ACTIVE_PROPERTY, false));
        Assert.assertNull(dropTarget.getDropEffect());

        DropTarget.of(component, false);
        Assert.assertFalse(component.getElement()
                .getProperty(Constants.DROP_TARGET_ACTIVE_PROPERTY, false));
    }

    @Test
    public void testDropTarget_dropListener_correctData() {
        RouterLink component = new RouterLink();
        ui.add(component);
        DropTarget<RouterLink> dropTarget = DropTarget.of(component);

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
        DragSource<RouterLink> dragSource = DragSource.of(source);

        RouterLink target = new RouterLink();
        ui.add(target);
        DropTarget<RouterLink> dropTarget = DropTarget.of(target);

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
        DropTarget<RouterLink> dropTarget = DropTarget.of(component);

        DropEvent<RouterLink> dropEvent = new DropEvent<>(component, true,
                "all");
        ComponentUtil.fireEvent(component, dropEvent);
    }
}
