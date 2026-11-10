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
package com.vaadin.flow.component;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.ShortcutFilterJs;
import com.vaadin.flow.component.internal.ShortcutJs;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.ConstantPoolKey;
import com.vaadin.flow.internal.ExecutionContext;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.internal.change.MapPutChange;
import com.vaadin.flow.internal.change.NodeChange;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.flow.shared.Registration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShortcutRegistrationTest {

    private UI ui;
    private Component lifecycleOwner;
    private Component[] listenOn = new Component[3];

    @BeforeEach
    void initTests() {
        ui = mock(UI.class);
        lifecycleOwner = mock(Component.class);
        Arrays.setAll(listenOn, i -> mock(Component.class));

        when(lifecycleOwner.getUI()).thenReturn(Optional.of(ui));
        when(lifecycleOwner.getUIOrThrow()).thenCallRealMethod();
        when(lifecycleOwner.addAttachListener(any()))
                .thenReturn(mock(Registration.class));
        when(lifecycleOwner.addDetachListener(any()))
                .thenReturn(mock(Registration.class));

        for (Component component : listenOn) {
            when(component.getUI()).thenReturn(Optional.of(ui));
        }
    }

    @Test
    void registrationWillBeCompletedBeforeClientResponse() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {
                }, Key.KEY_A);

        clientResponse();

        // If everything went according to plan, the shortcut should be
        // active
        // on our fake UI
        assertTrue(registration.isShortcutActive());

        // There should be no need to update the client
        assertFalse(registration.isDirty());
    }

    @Test
    void constructedRegistrationIsDirty() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {
                }, Key.KEY_A);

        assertTrue(registration.isDirty());
    }

    @Test
    void lateUpdateOfModifiersDirtiesRegistration() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {
                }, Key.KEY_A);

        clientResponse();

        assertFalse(registration.isDirty());

        registration.withModifiers(KeyModifier.ALT);

        assertTrue(registration.isDirty());
        assertEquals(1, registration.getModifiers().size());
    }

    @Test
    void fluentModifiersAreAddedCorrectly() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {
                }, Key.KEY_A);

        registration.withAlt().withCtrl().withMeta().withShift();

        assertEquals(4, registration.getModifiers().size());
    }

    @Test
    void preventDefaultAndStopPropagationValuesDefaultToTrue() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {
                }, Key.KEY_A);

        assertFalse(registration.isBrowserDefaultAllowed());
        assertFalse(registration.isEventPropagationAllowed());

        registration.allowBrowserDefault().allowEventPropagation();

        assertTrue(registration.isBrowserDefaultAllowed());
        assertTrue(registration.isEventPropagationAllowed());
    }

    @Test
    void resetFocusOnActiveElementValuesDefaultToTrue() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {
                }, Key.KEY_A);

        assertFalse(registration.isResetFocusOnActiveElement());
        registration.resetFocusOnActiveElement();
        assertTrue(registration.isResetFocusOnActiveElement());
    }

    @Test
    void bindLifecycleToChangesLifecycleOwner() {
        Component newOwner = mock(Component.class);

        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {
                }, Key.KEY_A);

        assertEquals(lifecycleOwner, registration.getLifecycleOwner());

        registration.bindLifecycleTo(newOwner);

        assertEquals(newOwner, registration.getLifecycleOwner());

    }

    @Test
    void settersAndGettersChangeValuesCorrectly() {

        // Component listenOn = mock(Component.class);
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {
                }, Key.KEY_A);

        registration.setBrowserDefaultAllowed(true);
        registration.setEventPropagationAllowed(true);
        registration.setResetFocusOnActiveElement(true);
        registration.setEventsFromNestedModalsAllowed(true);

        clientResponse();

        assertTrue(registration.isBrowserDefaultAllowed(),
                "Allow default was not set to true");
        assertTrue(registration.isBrowserDefaultAllowed(),
                "Allow propagation was not set to true");
        assertTrue(registration.isResetFocusOnActiveElement(),
                "Reset focus on active element was not set to true");
        assertTrue(registration.isEventsFromNestedModalsAllowed(),
                "Allow events from nested modals was not set to true");

        registration.setBrowserDefaultAllowed(false);
        registration.setEventPropagationAllowed(false);
        registration.setResetFocusOnActiveElement(false);
        registration.setEventsFromNestedModalsAllowed(false);

        clientResponse();

        assertFalse(registration.isBrowserDefaultAllowed(),
                "Allow default was not set to false");
        assertFalse(registration.isEventPropagationAllowed(),
                "Allow propagation was not set to false");
        assertFalse(registration.isResetFocusOnActiveElement(),
                "Reset focus on active element was not set to false");
        assertFalse(registration.isEventsFromNestedModalsAllowed(),
                "Allow events from nested modals was not set to false");
    }

    @Test
    void listenOnChangesTheComponentThatOwnsTheListener() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {
                }, Key.KEY_A);

        for (Component component : listenOn) {
            when(component.addDetachListener(any()))
                    .thenReturn(mock(Registration.class));
        }
        clientResponse();

        // listenOn component should be set after client response
        assertArrayEquals(listenOn, registration.getOwners());

        // Change the listenOn component
        Component[] newListenOn = new Component[2];
        Arrays.setAll(newListenOn, i -> mock(Component.class));
        for (Component component : newListenOn)
            when(component.getUI()).thenReturn(Optional.of(ui));
        registration.listenOn(newListenOn);

        clientResponse(newListenOn);

        // listenOn component should be set to the new component
        assertArrayEquals(newListenOn, registration.getOwners());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    void listenOnComponentIsChanged_eventIsPopulatedForANewListenOnComponent() {
        UI ui = spyUiWithSession();
        Component owner = new FakeComponent();
        Component initialComponentToListenOn = new FakeComponent();

        Component[] components = new Component[] { initialComponentToListenOn };

        ui.add(owner);
        ui.add(initialComponentToListenOn);
        new ShortcutRegistration(owner, () -> components, event -> {
        }, Key.KEY_A);

        ArgumentCaptor<SerializableConsumer> captor = ArgumentCaptor
                .forClass(SerializableConsumer.class);

        verify(ui, atLeastOnce()).beforeClientResponse(eq(owner),
                captor.capture());

        SerializableConsumer consumer = captor.getValue();

        // Fake beforeClientExecution call.
        consumer.accept(mock(ExecutionContext.class));

        // Once the shortcut listener is registered the expression should
        // contain KeyA
        assertTrue(hasKeyAInKeyDownExpression(initialComponentToListenOn));

        Component replacementComponentToListenOn = new FakeComponent();
        components[0] = replacementComponentToListenOn;
        ui.add(components[0]);
        // detach the original "listen on" component: the new one replaces the
        // old one
        ui.remove(initialComponentToListenOn);

        // now re-attach the owner
        ui.remove(owner);

        ui.add(owner);

        consumer.accept(mock(ExecutionContext.class));
        // the new component should now also have expression with KeyA
        assertTrue(hasKeyAInKeyDownExpression(replacementComponentToListenOn));
    }

    @Test
    void listenOnUIIsClosing_eventIsPopulatedForANewUI() {
        UI ui = spyUiWithSession();
        Component owner = new FakeComponent();

        Component[] components = new Component[] { ui };

        ui.add(owner);
        new ShortcutRegistration(owner, () -> components, event -> {
        }, Key.KEY_A);

        UI newUI = spyUiWithSession();
        // close the previous UI
        ui.close();
        components[0] = newUI;

        owner.getElement().removeFromTree(false);
        newUI.add(owner);

        ArgumentCaptor<SerializableConsumer> captor = ArgumentCaptor
                .forClass(SerializableConsumer.class);

        verify(ui, atLeastOnce()).beforeClientResponse(eq(owner),
                captor.capture());

        SerializableConsumer consumer = captor.getValue();

        // Fake beforeClientExecution call.
        consumer.accept(mock(ExecutionContext.class));
        // the new UI should now also have expression with KeyA
        assertTrue(hasKeyAInKeyDownExpression(newUI));
    }

    @Test
    void shortcutRegistrationReturnedByClickNotifierHasCorrectDefault() {
        FakeComponent fakeComponent = new FakeComponent();

        ShortcutRegistration registration = fakeComponent
                .addClickShortcut(Key.KEY_A);

        assertTrue(registration.isBrowserDefaultAllowed(),
                "Allows default was not true");

        assertFalse(registration.isEventPropagationAllowed(),
                "Allows propagation was not false");

        assertFalse(registration.isResetFocusOnActiveElement(),
                "Reset focus on active element was not set to false");
    }

    @Test
    void shortcutRegistrationReturnedByFocusableHasCorrectDefaults() {
        FakeComponent fakeComponent = new FakeComponent();

        ShortcutRegistration registration = fakeComponent
                .addFocusShortcut(Key.KEY_A);

        assertFalse(registration.isBrowserDefaultAllowed(),
                "Allows default was not false");

        assertFalse(registration.isEventPropagationAllowed(),
                "Allows propagation was not false");

        assertFalse(registration.isResetFocusOnActiveElement(),
                "Reset focus on active element was not set to false");
    }

    @Test
    void listenOnWithDuplicateShouldThrowException() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {
                }, Key.KEY_A);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> registration
                        .listenOn(listenOn[0], listenOn[1], listenOn[1]));
        assertTrue(ex.getMessage().contains(
                ShortcutRegistration.LISTEN_ON_COMPONENTS_SHOULD_NOT_HAVE_DUPLICATE_ENTRIES));
    }

    @Test
    void listenOnWithNullEntriesShouldThrowException() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {
                }, Key.KEY_A);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> registration.listenOn(listenOn[0], null, listenOn[1]));
        assertTrue(ex.getMessage().contains(
                ShortcutRegistration.LISTEN_ON_COMPONENTS_SHOULD_NOT_CONTAIN_NULL));
    }

    @Test
    void listenOnItemsAreChangedAfterCallingListenOnShouldNotHaveAnyEffect() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {
                }, Key.KEY_A);

        Component[] newListenOn = new Component[] { listenOn[0], listenOn[1] };
        registration.listenOn(newListenOn);
        newListenOn[0] = null;
        newListenOn[1] = null;

        clientResponse();

        assertTrue(registration.isShortcutActive());
    }

    @Test
    @SuppressWarnings("removal")
    void listenOnComponentHasElementLocatorJs_jsExecutionScheduled() {
        final ElementLocatorTestFixture fixture = new ElementLocatorTestFixture();
        final Key key = Key.KEY_A;
        fixture.createNewShortcut(key).withCtrl();

        final JsCall call = delegateCall(fixture.writeResponse());
        assertEquals("registerKeydownDelegate", call.methodName());
        // The string locator is compiled by the browser, with the listen-on
        // element as this
        final JsFunction locator = (JsFunction) call.arguments().get(0);
        assertEquals("return (" + fixture.elementLocatorJs + ");",
                locator.getBody());
        assertEquals("Control", call.arguments().get(1));
        assertEquals("Shift Alt AltGraph Meta", call.arguments().get(2));
        // resetFocus=false, allowDefault=false by default
        assertEquals(List.of(true, false, false),
                call.arguments().subList(3, 6));
        assertArrayEquals(key.getKeys().toArray(),
                (Object[]) call.arguments().get(6));

        fixture.registration.remove();

        fixture.createNewShortcut(Key.KEY_X);

        // Locator removed and client already initialized: no more JS scheduled.
        List<PendingJavaScriptInvocation> pendingJavaScriptInvocations = fixture
                .writeResponse();
        assertEquals(0, pendingJavaScriptInvocations.size());
    }

    @Test
    void listenOnComponentHasDeclaredElementLocator_locatorPassedToDelegate() {
        final JsFunction locator = JsSnippets.of(LocatorJs.class)
                .locateOverlay();
        final ElementLocatorTestFixture fixture = new ElementLocatorTestFixture(
                locator);
        fixture.createNewShortcut(Key.KEY_A);

        final JsCall call = delegateCall(fixture.writeResponse());
        assertEquals(locator, call.arguments().get(0));
    }

    @Test
    void listenOnComponentHasDeclaredElementLocator_delegateRegisteredOncePerSettings() {
        final ElementLocatorTestFixture fixture = new ElementLocatorTestFixture(
                JsSnippets.of(LocatorJs.class).locateOverlay());
        fixture.createNewShortcut(Key.KEY_A);
        fixture.createNewShortcut(Key.KEY_A);
        assertEquals(1, delegateCalls(fixture.writeResponse()).size());

        // Setting an equal locator again does not relay keydowns twice
        Shortcuts.setShortcutListenOnElementLocator(
                JsSnippets.of(LocatorJs.class).locateOverlay(),
                fixture.components[0]);
        fixture.createNewShortcut(Key.KEY_A);
        assertEquals(0, delegateCalls(fixture.writeResponse()).size());

        fixture.createNewShortcut(Key.KEY_B);
        final List<JsCall> calls = delegateCalls(fixture.writeResponse());
        assertEquals(1, calls.size());
        assertArrayEquals(Key.KEY_B.getKeys().toArray(),
                (Object[]) calls.get(0).arguments().get(6));
    }

    @Test
    void listenOnComponentHasElementLocatorJs_allowBrowserDefault_JsExecutionDoesNotPreventDefault() {
        final ElementLocatorTestFixture fixture = new ElementLocatorTestFixture();
        final Key key = Key.KEY_A;
        fixture.createNewShortcut(key).allowBrowserDefault();

        final JsCall call = delegateCall(fixture.writeResponse());
        assertEquals(true, call.arguments().get(5),
                "delegate should allow browser default");
    }

    @Test
    void listenOnComponentHasElementLocatorJs_resetFocusOnActiveElement_JsExecutionResetFocusOnActiveElement() {
        final ElementLocatorTestFixture fixture = new ElementLocatorTestFixture();
        final Key key = Key.KEY_A;
        fixture.createNewShortcut(key).resetFocusOnActiveElement();

        final JsCall call = delegateCall(fixture.writeResponse());
        assertEquals(true, call.arguments().get(4),
                "delegate should reset focus on active element");
    }

    @Test
    void listenOnComponentHasElementLocatorJs_allowEventsFromNestedModals_noGuard() {
        final ElementLocatorTestFixture fixture = new ElementLocatorTestFixture();
        fixture.createNewShortcut(Key.KEY_A).allowEventsFromNestedModals();

        final JsCall call = delegateCall(fixture.writeResponse());
        assertEquals(false, call.arguments().get(3),
                "delegate should not guard against nested modal events when "
                        + "explicitly allowed");
    }

    @Test
    void eventsFromNestedModalsAllowedDefaultsToFalse() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {
                }, Key.KEY_A);

        assertFalse(registration.isEventsFromNestedModalsAllowed());
        registration.allowEventsFromNestedModals();
        assertTrue(registration.isEventsFromNestedModalsAllowed());
        // Calling again is a no-op and must keep the value unchanged.
        registration.allowEventsFromNestedModals();
        assertTrue(registration.isEventsFromNestedModalsAllowed());
        // Setting the same value via the bean setter is also a no-op.
        registration.setEventsFromNestedModalsAllowed(true);
        assertTrue(registration.isEventsFromNestedModalsAllowed());
    }

    @Test
    void ownerScopeGuard_lifecycleOwnerMarkedByDefault() {
        final ElementLocatorTestFixture fixture = new ElementLocatorTestFixture();
        fixture.createNewShortcut(Key.KEY_A);

        fixture.writeResponse();

        assertNotNull(
                fixture.owner.getElement().getAttribute(
                        ShortcutRegistration.SHORTCUT_OWNER_ATTRIBUTE),
                "lifecycle owner should be marked so the origin guard can locate it");
    }

    @Test
    void ownerScopeGuard_lifecycleOwnerNotMarkedWhenAllowed() {
        final ElementLocatorTestFixture fixture = new ElementLocatorTestFixture();
        final ShortcutRegistration registration = fixture
                .createNewShortcut(Key.KEY_A);

        fixture.writeResponse();
        assertNotNull(fixture.owner.getElement()
                .getAttribute(ShortcutRegistration.SHORTCUT_OWNER_ATTRIBUTE));

        registration.setEventsFromNestedModalsAllowed(true);
        fixture.writeResponse();

        assertNull(
                fixture.owner.getElement().getAttribute(
                        ShortcutRegistration.SHORTCUT_OWNER_ATTRIBUTE),
                "owner marker should be removed when nested-modal events are allowed");
    }

    @Test
    void ownerScopeGuard_componentLifecycleOwner_filterLocatesOwnerByToken() {
        UI realUi = spyUiWithSession();
        Component owner = new FakeComponent();
        realUi.add(owner);
        new ShortcutRegistration(owner, () -> new Component[] { realUi },
                event -> {
                }, Key.KEY_A);

        realUi.getInternals().getStateTree()
                .runExecutionsBeforeClientResponse();

        final String token = owner.getElement()
                .getAttribute(ShortcutRegistration.SHORTCUT_OWNER_ATTRIBUTE);
        assertNotNull(token, "component owner should be marked");
        final ArrayNode captures = shortcutFilterCaptures(realUi);
        assertTrue(captures.get(2).booleanValue(),
                "filter should check the owner scope");
        assertEquals(
                "[" + ShortcutRegistration.SHORTCUT_OWNER_ATTRIBUTE + "~=\""
                        + token + "\"]",
                captures.get(3).asString(),
                "filter should locate the owner element by its marker token");
    }

    @Test
    void ownerScopeGuard_uiLifecycleOwner_filterHasNoTokenAndUiIsNotMarked() {
        UI realUi = spyUiWithSession();
        realUi.addShortcutListener(() -> {
        }, Key.KEY_A);

        realUi.getInternals().getStateTree()
                .runExecutionsBeforeClientResponse();

        assertNull(
                realUi.getElement().getAttribute(
                        ShortcutRegistration.SHORTCUT_OWNER_ATTRIBUTE),
                "the UI element can never be inside a popover, so it needs no marker");
        final ArrayNode captures = shortcutFilterCaptures(realUi);
        assertTrue(captures.get(2).booleanValue(),
                "filter should check the owner scope");
        assertTrue(captures.get(3).isNull(),
                "UI-owned shortcut should use the token-free origin guard "
                        + captures);
    }

    @Test
    void listenerFilter_capturesKeysModifiersAndSideEffects() {
        UI realUi = spyUiWithSession();
        realUi.addShortcutListener(() -> {
        }, Key.of("Digit1", "1"), KeyModifier.ALT, KeyModifier.CONTROL)
                .allowEventPropagation().resetFocusOnActiveElement();

        realUi.getInternals().getStateTree()
                .runExecutionsBeforeClientResponse();

        final ArrayNode captures = shortcutFilterCaptures(realUi);
        // Modifiers that are not part of the shortcut have to be released,
        // except AltGraph when Alt is part of it (#15906)
        assertEquals("Control Alt", captures.get(0).asString());
        assertEquals("Shift Meta", captures.get(1).asString());
        // preventDefault, stopPropagation, resetFocus
        assertEquals(List.of(true, false, true),
                List.of(captures.get(4).booleanValue(),
                        captures.get(5).booleanValue(),
                        captures.get(6).booleanValue()));
        // Every key of the primary key, matched as key code or key value
        assertEquals(List.of("Digit1", "1"), List.of(captures.get(7).asString(),
                captures.get(8).asString()));
        assertEquals(9, captures.size());
    }

    @Test
    void ownerScopeGuard_uiLifecycleOwner_repeatedRegistrationsShareFilter() {
        // #25624: filter settings that differ per registration are retained
        // forever by both constant pools, so re-registering the same shortcut
        // must not produce new settings.
        UI realUi = spyUiWithSession();
        final StateTree tree = realUi.getInternals().getStateTree();

        ShortcutRegistration first = realUi.addShortcutListener(() -> {
        }, Key.KEY_A);
        tree.runExecutionsBeforeClientResponse();
        // The whole settings object is what keys the constant pool entry.
        final ObjectNode firstSettings = keydownSettings(realUi);

        first.remove();
        realUi.addShortcutListener(() -> {
        }, Key.KEY_A);
        tree.runExecutionsBeforeClientResponse();

        assertEquals(firstSettings, keydownSettings(realUi),
                "re-registering the same UI shortcut should reuse the very "
                        + "same settings");
    }

    @Test
    void constructedRegistration_lifecycleIsVisibleAndEnabled_shorcutEventIsFired() {
        AtomicReference<ShortcutEvent> event = new AtomicReference<>();

        new ShortcutRegistration(lifecycleOwner, () -> listenOn, event::set,
                Key.KEY_A);

        mockLifecycle(true);

        clientResponse();

        listenOn[0].getEventBus()
                .fireEvent(new KeyDownEvent(listenOn[0], Key.KEY_A.toString()));

        assertNotNull(event.get());
    }

    @Test
    void constructedRegistration_lifecycleOnwerIsDisabled_shorcutEventIsNotFired() {
        AtomicReference<ShortcutEvent> event = new AtomicReference<>();

        new ShortcutRegistration(lifecycleOwner, () -> listenOn, event::set,
                Key.KEY_A);

        Element element = mockLifecycle(true);
        element.setEnabled(false);

        clientResponse();

        listenOn[0].getEventBus()
                .fireEvent(new KeyDownEvent(listenOn[0], Key.KEY_A.toString()));

        assertNull(event.get());
    }

    @Test
    void constructedRegistration_lifecycleOwnerIsDisabledWithDisabledUpdateModeAlways_shortcutEventIsFired() {
        AtomicReference<ShortcutEvent> event = new AtomicReference<>();

        new ShortcutRegistration(lifecycleOwner, () -> listenOn, event::set,
                Key.KEY_A).setDisabledUpdateMode(DisabledUpdateMode.ALWAYS);

        Element element = mockLifecycle(true);
        element.setEnabled(false);

        clientResponse();

        listenOn[0].getEventBus()
                .fireEvent(new KeyDownEvent(listenOn[0], Key.KEY_A.toString()));

        assertNotNull(event.get());
    }

    @Test
    void constructedRegistration_lifecycleOnwerIsInvisible_shorcutEventIsNotFired() {
        AtomicReference<ShortcutEvent> event = new AtomicReference<>();

        new ShortcutRegistration(lifecycleOwner, () -> listenOn, event::set,
                Key.KEY_A);

        mockLifecycle(false);

        clientResponse();

        listenOn[0].getEventBus()
                .fireEvent(new KeyDownEvent(listenOn[0], Key.KEY_A.toString()));

        assertNull(event.get());
    }

    @Test
    void constructedRegistration_lifecycleOnwerAncestorsAreVisible_shorcutEventIsFired() {
        AtomicReference<ShortcutEvent> event = new AtomicReference<>();

        new ShortcutRegistration(lifecycleOwner, () -> listenOn, event::set,
                Key.KEY_A);

        mockLifecycle(true);
        when(lifecycleOwner.getParent())
                .thenReturn(Optional.of(new FakeComponent()));

        clientResponse();

        listenOn[0].getEventBus()
                .fireEvent(new KeyDownEvent(listenOn[0], Key.KEY_A.toString()));

        assertNotNull(event.get());
    }

    @Test
    void uiRegistration_uiHasModalComponent_eventIsSentFromModalComponentInsteadOfUi() {
        AtomicReference<ShortcutEvent> eventRef = new AtomicReference<>();

        Component modal = mock(Component.class);
        when(modal.getUI()).thenReturn(Optional.of(ui));
        when(modal.getUIOrThrow()).thenCallRealMethod();
        when(modal.getEventBus()).thenReturn(new ComponentEventBus(modal));
        when(modal.getElement()).thenReturn(new Element("tag"));
        when(modal.isVisible()).thenReturn(true);

        UIInternals uiInternals = mock(UIInternals.class);
        when(uiInternals.hasModalComponent()).thenReturn(true);
        when(uiInternals.getActiveModalComponent()).thenReturn(modal);
        when(ui.getInternals()).thenReturn(uiInternals);

        listenOn = new Component[] { ui };
        when(ui.getUI()).thenReturn(Optional.of(ui));
        when(ui.getUIOrThrow()).thenCallRealMethod();

        new ShortcutRegistration(lifecycleOwner, () -> listenOn, eventRef::set,
                Key.KEY_A);

        mockLifecycle(true);
        when(lifecycleOwner.getParent()).thenReturn(Optional.of(modal));

        clientResponse(listenOn);

        modal.getEventBus()
                .fireEvent(new KeyDownEvent(listenOn[0], Key.KEY_A.toString()));

        ShortcutEvent event = eventRef.get();
        assertNotNull(event);
        assertEquals(modal, event.getSource());
    }

    @Test
    void constructedRegistration_lifecycleOnwerHasInvisibleParent_shorcutEventIsNotFired() {
        AtomicReference<ShortcutEvent> event = new AtomicReference<>();

        new ShortcutRegistration(lifecycleOwner, () -> listenOn, event::set,
                Key.KEY_A);

        Element ownerElement = mockLifecycle(true);

        FakeComponent component = new FakeComponent();
        component.setVisible(false);
        component.getElement().appendChild(ownerElement);
        when(lifecycleOwner.getParent()).thenReturn(Optional.of(component));

        clientResponse();

        listenOn[0].getEventBus()
                .fireEvent(new KeyDownEvent(listenOn[0], Key.KEY_A.toString()));

        assertNull(event.get());
    }

    @Test
    void constructedRegistration_lifecycleOwnerNulledBeforeKeyEvent_noNPE()
            throws Exception {
        AtomicReference<ShortcutEvent> event = new AtomicReference<>();

        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event::set, Key.KEY_A);

        mockLifecycle(true);
        clientResponse();

        // Simulate the race condition: null out lifecycleOwner while
        // the KeyDown listener is still registered
        Field field = ShortcutRegistration.class
                .getDeclaredField("lifecycleOwner");
        field.setAccessible(true);
        field.set(registration, null);

        // Fire KeyDown event — should not throw NPE
        listenOn[0].getEventBus()
                .fireEvent(new KeyDownEvent(listenOn[0], Key.KEY_A.toString()));

        // Shortcut event should not have been fired
        assertNull(event.get());
    }

    @Test
    void constructedRegistration_lifeCycleOwnerIsDetached_detachListenerIsDeregisteredFromListenOnComponents() {
        AtomicReference<ComponentEventListener> detachListener = new AtomicReference<>();
        doAnswer(invocaation -> {
            detachListener.set(
                    invocaation.getArgument(0, ComponentEventListener.class));
            return mock(Registration.class);
        }).when(lifecycleOwner).addDetachListener(any());

        new ShortcutRegistration(lifecycleOwner, () -> listenOn, event -> {
        }, Key.KEY_A);

        Registration registration = mock(Registration.class);
        for (Component component : listenOn) {
            when(component.addDetachListener(any())).thenReturn(registration);
        }

        clientResponse();

        detachListener.get().onComponentEvent(new DetachEvent(lifecycleOwner));

        verify(registration, times(3)).remove();
    }

    @Test
    void reattachComponent_detachListenerIsAddedOnEveryAttach_listenOnUIIsClosing_eventIsPopulatedForANewUI() {
        UI ui = spyUiWithSession();
        Component owner = new FakeComponent();

        Registration registration = mock(Registration.class);
        AtomicInteger count = new AtomicInteger();
        doAnswer(invocation -> {
            count.incrementAndGet();
            return registration;
        }).when(ui).addDetachListener(any());

        Component[] components = new Component[] { ui };

        ui.add(owner);
        new ShortcutRegistration(owner, () -> components, event -> {
        }, Key.KEY_A);

        ArgumentCaptor<SerializableConsumer> captor = ArgumentCaptor
                .forClass(SerializableConsumer.class);
        verify(ui, atLeastOnce()).beforeClientResponse(eq(owner),
                captor.capture());

        SerializableConsumer consumer = captor.getValue();

        // Fake beforeClientExecution call.
        consumer.accept(mock(ExecutionContext.class));
        assertEquals(1, count.get());

        ui.remove(owner);

        // reattach
        ui.add(owner);

        // Fake beforeClientExecution call.
        consumer.accept(mock(ExecutionContext.class));
        assertEquals(2, count.get());

        UI newUI = spyUiWithSession();
        // close the previous UI
        ui.close();
        components[0] = newUI;

        owner.getElement().removeFromTree(false);
        newUI.add(owner);

        verify(newUI, atLeastOnce()).beforeClientResponse(eq(owner),
                captor.capture());
        // Fake beforeClientExecution call.
        captor.getValue().accept(mock(ExecutionContext.class));

        // the new UI should now also have expression with KeyA
        assertTrue(hasKeyAInKeyDownExpression(newUI));
    }

    @Test
    void attachAndDetachComponent_sameRoundTrip_beforeClientResponseListenerRemoved() {
        UI ui = spyUiWithSession();
        Component owner = new FakeComponent();

        List<StateTree.ExecutionRegistration> beforeClientRegistrations = new ArrayList<>();
        doAnswer(i -> {
            StateTree.ExecutionRegistration registration = new StateTree.ExecutionRegistration() {
                @Override
                public void remove() {
                    beforeClientRegistrations.remove(this);
                }
            };

            beforeClientRegistrations.add(registration);
            return registration;
        }).when(ui).beforeClientResponse(eq(owner), any());

        ui.add(owner);
        Component[] components = new Component[] { ui };
        new ShortcutRegistration(owner, () -> components, event -> {
        }, Key.KEY_A);
        assertEquals(1, beforeClientRegistrations.size());

        ui.remove(owner);
        assertEquals(0, beforeClientRegistrations.size());

        ui.add(owner);
        assertEquals(1, beforeClientRegistrations.size());
        ui.remove(owner);
        assertEquals(0, beforeClientRegistrations.size());
    }

    @Test
    void toString_listenOnComponentsNotInitialized_doesNotFail() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {
                }, Key.KEY_A);
        assertTrue(registration.toString().contains("listenOn = []"));

        clientResponse();
        assertTrue(
                registration.toString().matches(".*listenOn = \\[[^]]+],.*"));
    }

    private Element mockLifecycle(boolean visible) {
        when(lifecycleOwner.isVisible()).thenReturn(visible);
        Element element = ElementFactory.createAnchor();
        element.setVisible(visible);
        when(lifecycleOwner.getElement()).thenReturn(element);
        return element;
    }

    class ElementLocatorTestFixture {

        final Registration registration;
        final Component owner;
        private final String elementLocatorJs;
        private final Component[] components;
        private final UI ui;

        ElementLocatorTestFixture() {
            this(null);
        }

        @SuppressWarnings("removal")
        ElementLocatorTestFixture(JsFunction declaredLocator) {
            VaadinSession session = mock(VaadinSession.class);
            when(session.hasLock()).thenReturn(true);
            ui = spy(UI.class);
            ui.getInternals().setSession(session);

            owner = new FakeComponent();
            Component initialComponentToListenOn = new FakeComponent();
            components = new Component[] { initialComponentToListenOn };

            ui.add(owner);
            ui.add(initialComponentToListenOn);

            elementLocatorJs = "foobar";
            registration = declaredLocator == null
                    ? Shortcuts.setShortcutListenOnElement(elementLocatorJs,
                            initialComponentToListenOn)
                    : Shortcuts.setShortcutListenOnElementLocator(
                            declaredLocator, initialComponentToListenOn);
        }

        List<PendingJavaScriptInvocation> writeResponse() {
            ui.getInternals().getStateTree()
                    .runExecutionsBeforeClientResponse();

            return ui.getInternals().dumpPendingJavaScriptInvocations();
        }

        ShortcutRegistration createNewShortcut(Key key) {
            return new ShortcutRegistration(owner, () -> components, event -> {
            }, key);
        }

    }

    /**
     * Works only with the {@code registration} member variable.
     *
     * Simulates a "beforeClientResponse" callback for the given
     * {@link ShortcutRegistration}
     */
    private void clientResponse() {
        clientResponse(listenOn);
    }

    /**
     * Works only with the {@code registration} member variable, but allows
     * configuring the {@code listenOn} component
     *
     * Simulates a "beforeClientResponse" callback for the given
     * {@link ShortcutRegistration}
     */
    private void clientResponse(Component[] listenOnMock) {
        for (Component component : listenOnMock) {
            when(component.getElement()).thenReturn(new Element("tag"));
            when(component.getEventBus())
                    .thenReturn(new ComponentEventBus(component));
        }

        ArgumentCaptor<SerializableConsumer> captor = ArgumentCaptor
                .forClass(SerializableConsumer.class);

        verify(ui, atLeastOnce()).beforeClientResponse(eq(lifecycleOwner),
                captor.capture());

        SerializableConsumer consumer = captor.getValue();

        // Fake beforeClientExecution call.
        consumer.accept(mock(ExecutionContext.class));
    }

    private static UI spyUiWithSession() {
        UI spyUi = spy(UI.class);
        // A locked session lets the delegate's executeJs run.
        VaadinSession session = mock(VaadinSession.class);
        when(session.hasLock()).thenReturn(true);
        spyUi.getInternals().setSession(session);
        return spyUi;
    }

    private static JsCall delegateCall(
            List<PendingJavaScriptInvocation> invocations) {
        return delegateCalls(invocations).stream().findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No registerKeydownDelegate call scheduled"));
    }

    private static List<JsCall> delegateCalls(
            List<PendingJavaScriptInvocation> invocations) {
        return invocations.stream().map(i -> i.getInvocation().getJsCall())
                .filter(call -> call != null
                        && call.definitionType() == ShortcutJs.class)
                .toList();
    }

    /**
     * The keydown listener settings the client gets for the component.
     */
    private static ObjectNode keydownSettings(Component component) {
        List<NodeChange> changes = new ArrayList<>();
        component.getElement().getNode().getFeature(ElementListenerMap.class)
                .collectChanges(changes::add);
        ConstantPoolKey key = changes.stream()
                .filter(MapPutChange.class::isInstance)
                .map(MapPutChange.class::cast)
                .filter(change -> "keydown".equals(change.getKey()))
                .map(change -> (ConstantPoolKey) change.getValue())
                .reduce((first, second) -> second)
                .orElseThrow(() -> new AssertionError(
                        "No keydown listener registered"));
        ObjectNode constantPoolUpdate = JacksonUtils.createObjectNode();
        key.export(constantPoolUpdate);
        return (ObjectNode) constantPoolUpdate.get(key.getId());
    }

    /**
     * The captures of the single keydown listener filter of a shortcut:
     * required modifiers, forbidden modifiers, check owner scope, owner
     * selector, prevent default, stop propagation, reset focus and the keys.
     */
    private static ArrayNode shortcutFilterCaptures(Component component) {
        final String functionId = JsCall.functionId(Arrays
                .stream(ShortcutFilterJs.class.getMethods())
                .filter(method -> method.getName().equals("matchKeydown"))
                .findFirst().orElseThrow());
        final ObjectNode settings = keydownSettings(component);
        return JacksonUtils.getKeys(settings).stream().map(settings::get)
                .filter(entry -> functionId.equals(
                        entry.path(JsonConstants.EVENT_SETTINGS_FUNCTION)
                                .asString(null)))
                .map(entry -> (ArrayNode) entry
                        .get(JsonConstants.EVENT_SETTINGS_CAPTURES))
                .reduce((a, b) -> {
                    throw new AssertionError(
                            "More than one shortcut filter: " + a + " / " + b);
                }).orElseThrow(() -> new AssertionError(
                        "No shortcut filter registered for keydown"));
    }

    private boolean hasKeyAInKeyDownExpression(Component component) {
        // Once the shortcut listener is registered the filter should match
        // KeyA, and only it
        ArrayNode captures = shortcutFilterCaptures(component);
        return captures.size() == 8 && captures.get(7).asString()
                .equals(Key.KEY_A.getKeys().get(0));
    }

    @JsDefinition
    public interface LocatorJs extends Serializable {

        @JsExpression("return this.overlay;")
        JsFunction locateOverlay();
    }

    @Tag("imaginary-tag")
    private class FakeComponent extends Component
            implements ClickNotifier<FakeComponent>, Focusable<FakeComponent> {
    }
}
