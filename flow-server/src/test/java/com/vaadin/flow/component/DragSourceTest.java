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

import com.vaadin.flow.component.dnd.DragEndEvent;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DragStartEvent;
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.Constants;
import com.vaadin.tests.util.MockUI;
import org.junit.Assert;
import org.junit.Test;

public class DragSourceTest {

    @Test
    public void testDragSource_staticBuilder_wrapsComponent() {
        RouterLink component = new RouterLink();

        DragSource<RouterLink> dragSource = DragSource.of(component);

        Assert.assertEquals("component element not set draggable", "true",
                component.getElement().getProperty("draggable"));

        Assert.assertEquals(component, dragSource.getDragSourceComponent());
        Assert.assertEquals(EffectAllowed.UNINITIALIZED,
                dragSource.getEffectAllowed());

        dragSource.setEffectAllowed(EffectAllowed.COPY_MOVE);

        Assert.assertEquals(
                component.getElement().getProperty(
                        Constants.EFFECT_ALLOWED_ELEMENT_PROPERTY),
                EffectAllowed.COPY_MOVE.getClientPropertyValue());
    }

    @Test
    public void testDragSource_serverSideEvents_correctData() {
        RouterLink component = new RouterLink();
        new MockUI().add(component);
        DragSource<RouterLink> dragSource = DragSource.of(component);

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
        DragSource<RouterLink> dragSource = DragSource.of(component);

        DragStartEvent<RouterLink> startEvent = new DragStartEvent<RouterLink>(
                component, true);
        ComponentUtil.fireEvent(component, startEvent);
    }

    @Test
    public void testDragSource_notAttachedToUIAndCatchesDragEndEvent_doesNotThrow() {
        RouterLink component = new RouterLink();
        MockUI mockUI = new MockUI();
        mockUI.add(component);
        DragSource<RouterLink> dragSource = DragSource.of(component);

        DragStartEvent<RouterLink> startEvent = new DragStartEvent<RouterLink>(
                component, true);
        ComponentUtil.fireEvent(component, startEvent);
        Assert.assertEquals(component, mockUI.getActiveDragSourceComponent());

        // the drop event could remove the component if in same UI
        mockUI.remove(component);

        DragEndEvent<RouterLink> endEvent = new DragEndEvent<>(component, true,
                "move");
        // should not throw for removing the active drag source
        ComponentUtil.fireEvent(component, endEvent);

        Assert.assertNull(mockUI.getActiveDragSourceComponent());
    }
}
