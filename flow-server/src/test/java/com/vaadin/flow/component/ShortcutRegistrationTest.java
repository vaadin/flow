package com.vaadin.flow.component;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.ExecutionContext;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ShortcutRegistrationTest {

    private UI ui;
    private Component lifeOwner;
    private Component handlerOwner;

    @Before
    public void initTests() {
        ui = mock(UI.class);
        lifeOwner = mock(Component.class);
        handlerOwner = mock(Component.class);

        when(lifeOwner.getUI()).thenReturn(Optional.of(ui));
        when(handlerOwner.getUI()).thenReturn(Optional.of(ui));
    }

    @Test
    public void registrationWillBeCompletedBeforeClientResponse() {
        ShortcutRegistration registration = new ShortcutRegistration(lifeOwner,
                () -> handlerOwner, () -> {}, Key.KEY_A);

        clientResponse();

        // If everything went according to plan, the shortcut should be active
        // on our fake UI
        assertTrue(registration.isShortcutActive());

        // There should be no need to update the client
        assertFalse(registration.isDirty());
    }

    @Test
    public void constructedRegistrationIsDirty() {
        ShortcutRegistration registration = new ShortcutRegistration(lifeOwner,
                () -> handlerOwner, () -> {}, Key.KEY_A);

        assertTrue(registration.isDirty());
    }

    @Test
    public void lateUpdateOfModifiersDirtiesRegistration() {
        ShortcutRegistration registration = new ShortcutRegistration(lifeOwner,
                () -> handlerOwner, () -> {}, Key.KEY_A);

        clientResponse();

        assertFalse(registration.isDirty());

        registration.withModifiers(KeyModifier.ALT);

        assertTrue(registration.isDirty());
        assertEquals(1, registration.getModifiers().size());
    }

    @Test
    public void fluentModifiersAreAddedCorrectly() {
        ShortcutRegistration registration = new ShortcutRegistration(lifeOwner,
                () -> handlerOwner, () -> {}, Key.KEY_A);

        registration.withAlt().withCtrl().withMeta().withShift();

        assertEquals(4, registration.getModifiers().size());
    }

    @Test
    public void preventDefaultAndStopPropagationValuesDefaultToTrue() {
        ShortcutRegistration registration = new ShortcutRegistration(lifeOwner,
                () -> handlerOwner, () -> {}, Key.KEY_A);

        assertTrue(registration.preventsDefault());
        assertTrue(registration.stopsPropagation());

        registration.allowBrowserDefault().allowEventPropagation();

        assertFalse(registration.preventsDefault());
        assertFalse(registration.stopsPropagation());
    }

    /**
     * Works only with the {@code registration} member variable.
     *
     * Simulates a "beforeClientResponse" callback for the given
     * {@link ShortcutRegistration}
     */
    public void clientResponse() {
        /*
            In all honesty, this should be an integration test and it relies
            too heavily on the internals of ShortcutRegistration and other
            components, but it did help catch a bug so here it is!
         */

        when(handlerOwner.getElement()).thenReturn(new Element("tag"));
        when(handlerOwner.getEventBus()).thenReturn(new ComponentEventBus(handlerOwner));

        ArgumentCaptor<SerializableConsumer> captor =
                ArgumentCaptor.forClass(SerializableConsumer.class);

        verify(ui, times(1)).beforeClientResponse(
                eq(lifeOwner), captor.capture());

        SerializableConsumer consumer = captor.getValue();

        // Fake beforeClientExecution call.
        consumer.accept(mock(ExecutionContext.class));
    }
}