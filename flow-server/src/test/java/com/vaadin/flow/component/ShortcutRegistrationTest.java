/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.ExecutionContext;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ShortcutRegistrationTest {

    private UI ui;
    private Component lifecycleOwner;
    private Component[] listenOn = new Component[3];

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void initTests() {
        ui = mock(UI.class);
        lifecycleOwner = mock(Component.class);
        Arrays.setAll(listenOn, i -> mock(Component.class));

        when(lifecycleOwner.getUI()).thenReturn(Optional.of(ui));
        when(lifecycleOwner.addAttachListener(any()))
                .thenReturn(mock(Registration.class));
        when(lifecycleOwner.addDetachListener(any()))
                .thenReturn(mock(Registration.class));

        for (Component component : listenOn) {
            when(component.getUI()).thenReturn(Optional.of(ui));
        }
    }

    @Test
    public void registrationWillBeCompletedBeforeClientResponse() {
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
    public void constructedRegistrationIsDirty() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {
                }, Key.KEY_A);

        assertTrue(registration.isDirty());
    }

    @Test
    public void lateUpdateOfModifiersDirtiesRegistration() {
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
    public void fluentModifiersAreAddedCorrectly() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {
                }, Key.KEY_A);

        registration.withAlt().withCtrl().withMeta().withShift();

        assertEquals(4, registration.getModifiers().size());
    }

    @Test
    public void preventDefaultAndStopPropagationValuesDefaultToTrue() {
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
    public void resetFocusOnActiveElementValuesDefaultToTrue() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {
                }, Key.KEY_A);

        assertFalse(registration.isResetFocusOnActiveElement());
        registration.resetFocusOnActiveElement();
        assertTrue(registration.isResetFocusOnActiveElement());
    }

    @Test
    public void bindLifecycleToChangesLifecycleOwner() {
        Component newOwner = mock(Component.class);

        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {
                }, Key.KEY_A);

        assertEquals(lifecycleOwner, registration.getLifecycleOwner());

        registration.bindLifecycleTo(newOwner);

        assertEquals(newOwner, registration.getLifecycleOwner());

    }

    @Test
    public void settersAndGettersChangeValuesCorrectly() {

        // Component listenOn = mock(Component.class);
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {
                }, Key.KEY_A);

        registration.setBrowserDefaultAllowed(true);
        registration.setEventPropagationAllowed(true);
        registration.setResetFocusOnActiveElement(true);

        clientResponse();

        assertTrue("Allow default was not set to true",
                registration.isBrowserDefaultAllowed());
        assertTrue("Allow propagation was not set to true",
                registration.isBrowserDefaultAllowed());
        assertTrue("Reset focus on active element was not set to true",
                registration.isResetFocusOnActiveElement());

        registration.setBrowserDefaultAllowed(false);
        registration.setEventPropagationAllowed(false);
        registration.setResetFocusOnActiveElement(false);

        clientResponse();

        assertFalse("Allow default was not set to false",
                registration.isBrowserDefaultAllowed());
        assertFalse("Allow propagation was not set to false",
                registration.isEventPropagationAllowed());
        assertFalse("Reset focus on active element was not set to false",
                registration.isResetFocusOnActiveElement());
    }

    @Test
    public void listenOnChangesTheComponentThatOwnsTheListener() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {
                }, Key.KEY_A);

        for (Component component : listenOn) {
            when(component.addDetachListener(Mockito.any()))
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
    public void listenOnComponentIsChanged_eventIsPopulatedForANewListenOnComponent() {
        UI ui = Mockito.spy(UI.class);
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
        Assert.assertTrue(
                hasKeyAInKeyDownExpression(initialComponentToListenOn));

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
        Assert.assertTrue(
                hasKeyAInKeyDownExpression(replacementComponentToListenOn));
    }

    @Test
    public void listenOnUIIsClosing_eventIsPopulatedForANewUI() {
        UI ui = Mockito.spy(UI.class);
        Component owner = new FakeComponent();

        Component[] components = new Component[] { ui };

        ui.add(owner);
        new ShortcutRegistration(owner, () -> components, event -> {
        }, Key.KEY_A);

        UI newUI = Mockito.spy(UI.class);
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
        Assert.assertTrue(hasKeyAInKeyDownExpression(newUI));
    }

    @Test
    public void shortcutRegistrationReturnedByClickNotifierHasCorrectDefault() {
        FakeComponent fakeComponent = new FakeComponent();

        ShortcutRegistration registration = fakeComponent
                .addClickShortcut(Key.KEY_A);

        assertTrue("Allows default was not true",
                registration.isBrowserDefaultAllowed());

        assertFalse("Allows propagation was not false",
                registration.isEventPropagationAllowed());

        assertFalse("Reset focus on active element was not set to false",
                registration.isResetFocusOnActiveElement());
    }

    @Test
    public void shortcutRegistrationReturnedByFocusableHasCorrectDefaults() {
        FakeComponent fakeComponent = new FakeComponent();

        ShortcutRegistration registration = fakeComponent
                .addFocusShortcut(Key.KEY_A);

        assertFalse("Allows default was not false",
                registration.isBrowserDefaultAllowed());

        assertFalse("Allows propagation was not false",
                registration.isEventPropagationAllowed());

        assertFalse("Reset focus on active element was not set to false",
                registration.isResetFocusOnActiveElement());
    }

    @Test
    public void listenOnWithDuplicateShouldThrowException() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {
                }, Key.KEY_A);

        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage(
                ShortcutRegistration.LISTEN_ON_COMPONENTS_SHOULD_NOT_HAVE_DUPLICATE_ENTRIES);
        registration.listenOn(listenOn[0], listenOn[1], listenOn[1]);
    }

    @Test
    public void listenOnWithNullEntriesShouldThrowException() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {
                }, Key.KEY_A);

        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage(
                ShortcutRegistration.LISTEN_ON_COMPONENTS_SHOULD_NOT_CONTAIN_NULL);
        registration.listenOn(listenOn[0], null, listenOn[1]);
    }

    @Test
    public void listenOnItemsAreChangedAfterCallingListenOnShouldNotHaveAnyEffect() {
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
    public void listenOnComponentHasElementLocatorJs_jsExecutionScheduled() {
        final ElementLocatorTestFixture fixture = new ElementLocatorTestFixture();
        final Key key = Key.KEY_A;
        fixture.createNewShortcut(key);

        List<PendingJavaScriptInvocation> pendingJavaScriptInvocations = fixture
                .writeResponse();

        final PendingJavaScriptInvocation js = pendingJavaScriptInvocations
                .get(0);
        final String expression = js.getInvocation().getExpression();
        Assert.assertTrue(
                "element locator string " + fixture.elementLocatorJs
                        + " missing from JS execution string " + expression,
                expression.contains(
                        "const delegate=" + fixture.elementLocatorJs + ";"));
        Assert.assertTrue(
                "JS execution string should have event.preventDefault() in it"
                        + expression,
                expression.contains("event.preventDefault();"));
        Assert.assertTrue(
                "JS execution string should always have event.stopPropagation() in it"
                        + expression,
                expression.contains("event.stopPropagation();"));
        Assert.assertTrue("JS execution string missing the key" + key,
                expression.contains(key.getKeys().get(0)));
        Assert.assertFalse(
                "JS execution string should not have blur() and focus() on active element in it"
                        + expression,
                expression.contains("window.Vaadin.Flow.resetFocus()"));

        fixture.registration.remove();

        fixture.createNewShortcut(Key.KEY_X);

        pendingJavaScriptInvocations = fixture.writeResponse();
        Assert.assertEquals(0, pendingJavaScriptInvocations.size());
    }

    @Test
    public void listenOnComponentHasElementLocatorJs_allowBrowserDefault_JsExecutionDoesNotPreventDefault() {
        final ElementLocatorTestFixture fixture = new ElementLocatorTestFixture();
        final Key key = Key.KEY_A;
        fixture.createNewShortcut(key).allowBrowserDefault();

        List<PendingJavaScriptInvocation> pendingJavaScriptInvocations = fixture
                .writeResponse();

        final PendingJavaScriptInvocation js = pendingJavaScriptInvocations
                .get(0);
        final String expression = js.getInvocation().getExpression();
        Assert.assertFalse(
                "JS execution string should NOT have event.preventDefault() in it"
                        + expression,
                expression.contains("event.preventDefault();"));
    }

    @Test
    public void listenOnComponentHasElementLocatorJs_resetFocusOnActiveElement_JsExecutionResetFocusOnActiveElement() {
        final ElementLocatorTestFixture fixture = new ElementLocatorTestFixture();
        final Key key = Key.KEY_A;
        fixture.createNewShortcut(key).resetFocusOnActiveElement();

        List<PendingJavaScriptInvocation> pendingJavaScriptInvocations = fixture
                .writeResponse();

        final PendingJavaScriptInvocation js = pendingJavaScriptInvocations
                .get(0);
        final String expression = js.getInvocation().getExpression();
        Assert.assertTrue(
                "JS execution string should have blur() and focus() on active element in it"
                        + expression,
                expression.contains("window.Vaadin.Flow.resetFocus()"));
    }

    @Test
    public void constructedRegistration_lifecycleIsVisibleAndEnabled_shorcutEventIsFired() {
        AtomicReference<ShortcutEvent> event = new AtomicReference<>();

        new ShortcutRegistration(lifecycleOwner, () -> listenOn, event::set,
                Key.KEY_A);

        mockLifecycle(true);

        clientResponse();

        listenOn[0].getEventBus()
                .fireEvent(new KeyDownEvent(listenOn[0], Key.KEY_A.toString()));

        Assert.assertNotNull(event.get());
    }

    @Test
    public void constructedRegistration_lifecycleOnwerIsDisabled_shorcutEventIsNotFired() {
        AtomicReference<ShortcutEvent> event = new AtomicReference<>();

        new ShortcutRegistration(lifecycleOwner, () -> listenOn, event::set,
                Key.KEY_A);

        Element element = mockLifecycle(true);
        element.setEnabled(false);

        clientResponse();

        listenOn[0].getEventBus()
                .fireEvent(new KeyDownEvent(listenOn[0], Key.KEY_A.toString()));

        Assert.assertNull(event.get());
    }

    @Test
    public void constructedRegistration_lifecycleOwnerIsDisabledWithDisabledUpdateModeAlways_shortcutEventIsFired() {
        AtomicReference<ShortcutEvent> event = new AtomicReference<>();

        new ShortcutRegistration(lifecycleOwner, () -> listenOn, event::set,
                Key.KEY_A).setDisabledUpdateMode(DisabledUpdateMode.ALWAYS);

        Element element = mockLifecycle(true);
        element.setEnabled(false);

        clientResponse();

        listenOn[0].getEventBus()
                .fireEvent(new KeyDownEvent(listenOn[0], Key.KEY_A.toString()));

        Assert.assertNotNull(event.get());
    }

    @Test
    public void constructedRegistration_lifecycleOnwerIsInvisible_shorcutEventIsNotFired() {
        AtomicReference<ShortcutEvent> event = new AtomicReference<>();

        new ShortcutRegistration(lifecycleOwner, () -> listenOn, event::set,
                Key.KEY_A);

        mockLifecycle(false);

        clientResponse();

        listenOn[0].getEventBus()
                .fireEvent(new KeyDownEvent(listenOn[0], Key.KEY_A.toString()));

        Assert.assertNull(event.get());
    }

    @Test
    public void constructedRegistration_lifecycleOnwerAncestorsAreVisible_shorcutEventIsFired() {
        AtomicReference<ShortcutEvent> event = new AtomicReference<>();

        new ShortcutRegistration(lifecycleOwner, () -> listenOn, event::set,
                Key.KEY_A);

        mockLifecycle(true);
        Mockito.when(lifecycleOwner.getParent())
                .thenReturn(Optional.of(new FakeComponent()));

        clientResponse();

        listenOn[0].getEventBus()
                .fireEvent(new KeyDownEvent(listenOn[0], Key.KEY_A.toString()));

        Assert.assertNotNull(event.get());
    }

    @Test
    public void uiRegistration_uiHasModalComponent_eventIsSentFromModalComponentInsteadOfUi() {
        AtomicReference<ShortcutEvent> eventRef = new AtomicReference<>();

        Component modal = Mockito.mock(Component.class);
        when(modal.getUI()).thenReturn(Optional.of(ui));
        when(modal.getEventBus()).thenReturn(new ComponentEventBus(modal));
        when(modal.getElement()).thenReturn(new Element("tag"));
        when(modal.isVisible()).thenReturn(true);

        UIInternals uiInternals = Mockito.mock(UIInternals.class);
        Mockito.when(uiInternals.hasModalComponent()).thenReturn(true);
        Mockito.when(uiInternals.getActiveModalComponent()).thenReturn(modal);
        Mockito.when(ui.getInternals()).thenReturn(uiInternals);

        listenOn = new Component[] { ui };
        when(ui.getUI()).thenReturn(Optional.of(ui));

        new ShortcutRegistration(lifecycleOwner, () -> listenOn, eventRef::set,
                Key.KEY_A);

        mockLifecycle(true);
        Mockito.when(lifecycleOwner.getParent()).thenReturn(Optional.of(modal));

        clientResponse(listenOn);

        modal.getEventBus()
                .fireEvent(new KeyDownEvent(listenOn[0], Key.KEY_A.toString()));

        ShortcutEvent event = eventRef.get();
        Assert.assertNotNull(event);
        Assert.assertEquals(modal, event.getSource());
    }

    @Test
    public void constructedRegistration_lifecycleOnwerHasInvisibleParent_shorcutEventIsNotFired() {
        AtomicReference<ShortcutEvent> event = new AtomicReference<>();

        new ShortcutRegistration(lifecycleOwner, () -> listenOn, event::set,
                Key.KEY_A);

        mockLifecycle(true);

        FakeComponent component = new FakeComponent();
        component.setVisible(false);
        Mockito.when(lifecycleOwner.getParent())
                .thenReturn(Optional.of(component));

        clientResponse();

        listenOn[0].getEventBus()
                .fireEvent(new KeyDownEvent(listenOn[0], Key.KEY_A.toString()));

        Assert.assertNull(event.get());
    }

    @Test
    public void constructedRegistration_lifeCycleOwnerIsDetached_detachListenerIsDeregisteredFromListenOnComponents() {
        AtomicReference<ComponentEventListener> detachListener = new AtomicReference<>();
        Mockito.doAnswer(invocaation -> {
            detachListener.set(
                    invocaation.getArgument(0, ComponentEventListener.class));
            return mock(Registration.class);
        }).when(lifecycleOwner).addDetachListener(any());

        new ShortcutRegistration(lifecycleOwner, () -> listenOn, event -> {
        }, Key.KEY_A);

        Registration registration = Mockito.mock(Registration.class);
        for (Component component : listenOn) {
            Mockito.when(component.addDetachListener(Mockito.any()))
                    .thenReturn(registration);
        }

        clientResponse();

        detachListener.get().onComponentEvent(new DetachEvent(lifecycleOwner));

        Mockito.verify(registration, Mockito.times(3)).remove();
    }

    @Test
    public void reattachComponent_detachListenerIsAddedOnEveryAttach_listenOnUIIsClosing_eventIsPopulatedForANewUI() {
        UI ui = Mockito.spy(UI.class);
        Component owner = new FakeComponent();

        Registration registration = Mockito.mock(Registration.class);
        AtomicInteger count = new AtomicInteger();
        Mockito.doAnswer(invocation -> {
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
        Assert.assertEquals(1, count.get());

        ui.remove(owner);

        // reattach
        ui.add(owner);

        // Fake beforeClientExecution call.
        consumer.accept(mock(ExecutionContext.class));
        Assert.assertEquals(2, count.get());

        UI newUI = Mockito.spy(UI.class);
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
        Assert.assertTrue(hasKeyAInKeyDownExpression(newUI));
    }

    @Test
    public void attachAndDetachComponent_sameRoundTrip_beforeClientResponseListenerRemoved() {
        UI ui = Mockito.spy(UI.class);
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
        Assert.assertEquals(1, beforeClientRegistrations.size());

        ui.remove(owner);
        Assert.assertEquals(0, beforeClientRegistrations.size());

        ui.add(owner);
        Assert.assertEquals(1, beforeClientRegistrations.size());
        ui.remove(owner);
        Assert.assertEquals(0, beforeClientRegistrations.size());
    }

    @Test
    public void toString_listenOnComponentsNotInitialized_doesNotFail() {
        ShortcutRegistration registration = new ShortcutRegistration(
                lifecycleOwner, () -> listenOn, event -> {
                }, Key.KEY_A);
        Assert.assertTrue(registration.toString().contains("listenOn = []"));

        clientResponse();
        Assert.assertTrue(
                registration.toString().matches(".*listenOn = \\[[^]]+],.*"));
    }

    private Element mockLifecycle(boolean visible) {
        Mockito.when(lifecycleOwner.isVisible()).thenReturn(visible);
        Element element = ElementFactory.createAnchor();
        Mockito.when(lifecycleOwner.getElement()).thenReturn(element);
        return element;
    }

    class ElementLocatorTestFixture {

        final Registration registration;
        final Component owner;
        private final String elementLocatorJs;
        private final Component[] components;
        private final UI ui;

        ElementLocatorTestFixture() {
            VaadinSession session = Mockito.mock(VaadinSession.class);
            Mockito.when(session.hasLock()).thenReturn(true);
            ui = Mockito.spy(UI.class);
            ui.getInternals().setSession(session);

            owner = new FakeComponent();
            Component initialComponentToListenOn = new FakeComponent();
            components = new Component[] { initialComponentToListenOn };

            ui.add(owner);
            ui.add(initialComponentToListenOn);

            elementLocatorJs = "foobar";
            registration = Shortcuts.setShortcutListenOnElement(
                    elementLocatorJs, initialComponentToListenOn);
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

    private boolean hasKeyAInKeyDownExpression(Component component) {
        ElementListenerMap map = component.getElement().getNode()
                .getFeature(ElementListenerMap.class);

        // Once the shortcut listener is registered the expression should
        // contain KeyA
        boolean hasKeyA = false;
        for (String expression : map.getExpressions("keydown")) {
            if (expression.contains(Key.KEY_A.getKeys().get(0))) {
                hasKeyA = true;
            }
        }
        return hasKeyA;
    }

    @Tag("imaginary-tag")
    private class FakeComponent extends Component
            implements ClickNotifier<FakeComponent>, Focusable<FakeComponent> {
    }
}
