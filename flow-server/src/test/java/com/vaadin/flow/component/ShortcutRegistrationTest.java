/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.ExecutionContext;
import com.vaadin.flow.shared.Registration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ShortcutRegistrationTest {

    private UI ui;
    private Component lifecycleOwner;
    private Component listenOn;

    @Before
    public void initTests() {
        ui = mock(UI.class);
        lifecycleOwner = mock(Component.class);
        listenOn = mock(Component.class);

        when(lifecycleOwner.getUI()).thenReturn(Optional.of(ui));
        when(lifecycleOwner.addAttachListener(any())).thenReturn(mock(Registration.class));
        when(lifecycleOwner.addDetachListener(any())).thenReturn(mock(Registration.class));

        when(listenOn.getUI()).thenReturn(Optional.of(ui));
    }

    @Test
    public void registrationWillBeCompletedBeforeClientResponse() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {}, Key.KEY_A);

        clientResponse();

        // If everything went according to plan, the shortcut should be active
        // on our fake UI
        assertTrue(registration.isShortcutActive());

        // There should be no need to update the client
        assertFalse(registration.isDirty());
    }

    @Test
    public void constructedRegistrationIsDirty() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {}, Key.KEY_A);

        assertTrue(registration.isDirty());
    }

    @Test
    public void lateUpdateOfModifiersDirtiesRegistration() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {}, Key.KEY_A);

        clientResponse();

        assertFalse(registration.isDirty());

        registration.withModifiers(KeyModifier.ALT);

        assertTrue(registration.isDirty());
        assertEquals(1, registration.getModifiers().size());
    }

    @Test
    public void fluentModifiersAreAddedCorrectly() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {}, Key.KEY_A);

        registration.withAlt().withCtrl().withMeta().withShift();

        assertEquals(4, registration.getModifiers().size());
    }

    @Test
    public void preventDefaultAndStopPropagationValuesDefaultToTrue() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {}, Key.KEY_A);

        assertFalse(registration.isBrowserDefaultAllowed());
        assertFalse(registration.isEventPropagationAllowed());

        registration.allowBrowserDefault().allowEventPropagation();

        assertTrue(registration.isBrowserDefaultAllowed());
        assertTrue(registration.isEventPropagationAllowed());
    }

    @Test
    public void bindLifecycleToChangesLifecycleOwner() {
        Component newOwner = mock(Component.class);

        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {}, Key.KEY_A);

        assertEquals(lifecycleOwner, registration.getLifecycleOwner());

        registration.bindLifecycleTo(newOwner);

        assertEquals(newOwner, registration.getLifecycleOwner());

    }

    @Test
    public void settersAndGettersChangeValuesCorrectly() {

        //Component listenOn = mock(Component.class);
        ShortcutRegistration registration =
                new ShortcutRegistration(lifecycleOwner,
                        () -> listenOn, event -> {}, Key.KEY_A);

        registration.setBrowserDefaultAllowed(true);
        registration.setEventPropagationAllowed(true);

        clientResponse();

        assertTrue("Allow default was not set to true",
                registration.isBrowserDefaultAllowed());
        assertTrue("Allow propagation was not set to true",
                registration.isBrowserDefaultAllowed());

        registration.setBrowserDefaultAllowed(false);
        registration.setEventPropagationAllowed(false);

        clientResponse();

        assertFalse("Allow default was not set to false",
                registration.isBrowserDefaultAllowed());
        assertFalse("Allow propagation was not set to false",
                registration.isEventPropagationAllowed());
    }

    @Test
    public void listenOnChangesTheComponentThatOwnsTheListener() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {}, Key.KEY_A);

        // No response, no listenOn component
        assertNull(registration.getOwner());

        clientResponse();

        // listenOn component should be set after client response
        assertEquals(listenOn, registration.getOwner());

        // Change the listenOn component
        Component newListenOn = mock(Component.class);
        when(newListenOn.getUI()).thenReturn(Optional.empty());
        registration.listenOn(newListenOn);

        clientResponse(newListenOn);

        // listenOn component should be set to the new component
        assertEquals(newListenOn, registration.getOwner());
    }

    @Test
    public void shortcutRegistrationReturnedByClickNotifierHasCorrectDefault() {
        FakeComponent fakeComponent = new FakeComponent();

        ShortcutRegistration registration =
                fakeComponent.addClickShortcut(Key.KEY_A);

        assertTrue("Allows default was not true",
                registration.isBrowserDefaultAllowed());

        assertFalse("Allows propagation was not false",
                registration.isEventPropagationAllowed());
    }

    @Test
    public void shortcutRegistrationReturnedByFocusableHasCorrectDefaults() {
        FakeComponent fakeComponent = new FakeComponent();

        ShortcutRegistration registration =
                fakeComponent.addFocusShortcut(Key.KEY_A);

        assertFalse("Allows default was not false",
                registration.isBrowserDefaultAllowed());

        assertFalse("Allows propagation was not false",
                registration.isEventPropagationAllowed());
    }

    /**
     * Works only with the {@code registration} member variable.
     *
     * Simulates a "beforeClientResponse" callback for the given
     * {@link ShortcutRegistration}
     */
    private void clientResponse() {
        /*
            In all honesty, this should be an integration test and it relies
            too heavily on the internals of ShortcutRegistration and other
            components, but it did help catch a bug so here it is!
         */

        when(listenOn.getElement()).thenReturn(new Element("tag"));
        when(listenOn.getEventBus()).thenReturn(new ComponentEventBus(
                listenOn));

        ArgumentCaptor<SerializableConsumer> captor =
                ArgumentCaptor.forClass(SerializableConsumer.class);

        verify(ui, atLeastOnce()).beforeClientResponse(eq(lifecycleOwner),
                captor.capture());

        SerializableConsumer consumer = captor.getValue();

        // Fake beforeClientExecution call.
        consumer.accept(mock(ExecutionContext.class));
    }

    /**
     * Works only with the {@code registration} member variable, but allows
     * configuring the {@code listenOn} component
     *
     * Simulates a "beforeClientResponse" callback for the given
     * {@link ShortcutRegistration}
     */
    private void clientResponse(Component listenOnMock) {
        when(listenOnMock.getElement()).thenReturn(new Element("tag"));
        when(listenOnMock.getEventBus()).thenReturn(new ComponentEventBus(
                listenOnMock));

        ArgumentCaptor<SerializableConsumer> captor =
                ArgumentCaptor.forClass(SerializableConsumer.class);

        verify(ui, atLeastOnce()).beforeClientResponse(
                eq(lifecycleOwner), captor.capture());

        SerializableConsumer consumer = captor.getValue();

        // Fake beforeClientExecution call.
        consumer.accept(mock(ExecutionContext.class));
    }

    @Tag("imaginary-tag")
    private class FakeComponent extends Component implements
            ClickNotifier<FakeComponent>, Focusable<FakeComponent> {}
}
