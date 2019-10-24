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

import java.util.Locale;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dnd.internal.DndUtil;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.shared.Registration;

/**
 * Mixin interface that provides basic drop target API for any component.
 * <p>
 * This can be used by either implementing this interface with the static API
 * {@link #create(Component)}, {@link #configure(Component)} or
 * {@link #configure(Component, boolean)}.
 *
 * @param <T>
 *            the type of the drop target component
 * @see DragSource
 * @author Vaadin Ltd
 * @since 2.0
 */
@JavaScript(DndUtil.DND_CONNECTOR_COMPATIBILITY)
@JsModule(DndUtil.DND_CONNECTOR)
public interface DropTarget<T extends Component> extends HasElement {

    /**
     * Makes the given component a drop target and gives access to the generic
     * drop target API for the component.
     * <p>
     * The given component will be always set an active drop target, if this is
     * not desired, use either method {@link #configure(Component, boolean)} or
     * {@link #setActive(boolean)}.
     * 
     * @param component
     *            the component to make a drop target
     * @param <T>
     *            the type of the component
     * @return drop target API for the component
     * @see #configure(Component)
     * @see #configure(Component, boolean)
     */
    static <T extends Component> DropTarget<T> create(T component) {
        return configure(component, true);
    }

    /**
     * Gives access to the generic drop target API for the given component.
     * <p>
     * Unlike {@link #create(Component)} and
     * {@link #configure(Component, boolean)}, this method does not change the
     * active drop target status of the given component.
     * 
     * @param component
     *            the component to make a drop target
     * @param <T>
     *            the type of the component
     * @return drop target API for the component
     * @see #configure(Component, boolean)
     * @see #create(Component)
     */
    static <T extends Component> DropTarget<T> configure(T component) {
        DndUtil.addDndConnectorWhenComponentAttached(component);
        return new DropTarget<T>() {
            @Override
            public T getDropTargetComponent() {
                return component;
            }
        };
    }

    /**
     * Gives access to the generic drop target API for the given component and
     * either activates or deactivates the drop target.
     * <p>
     * This method is a shorthand for calling {@link #configure(Component)} and
     * {@link #setActive(boolean)}.
     * <p>
     * The drop target active state can be changed at any time with
     * {@link #setActive(boolean)}.
     * 
     * @param component
     *            the component to provide drop target API for
     * @param active
     *            {@code true} to set the component as an active drop target,
     *            {@code false} for not
     * @param <T>
     *            the type of the component
     * @return the drop target API for the component
     * @see #create(Component)
     * @see #configure(Component)
     */
    static <T extends Component> DropTarget<T> configure(T component,
            boolean active) {
        DropTarget<T> dropTarget = configure(component);
        dropTarget.setActive(active);
        return dropTarget;
    }

    /**
     * Returns the drop target component. This component is used in the drop
     * event as the source, and its element is made as a drop target by default.
     * <p>
     * The default implementation of this method returns {@code this}. This
     * method exists for type safe access for the drop target component and
     * being able to provide access to drop target API for any component.
     * 
     * @return the drop target component
     */
    default T getDropTargetComponent() {
        return (T) this;
    };

    /**
     * Returns the element which is made as a drop target in the UI. By default
     * it is the element of the component returned by
     * {@link #getDropTargetComponent()}.
     * 
     * @return the element that is a drop target
     */
    @Override
    default Element getElement() {
        return getDropTargetComponent().getElement();
    }

    /**
     * Activate or deactivate this drop target. By default, it is not active.
     * 
     * @param active
     *            {@code true} to allow drops, {@code false} to not
     */
    default void setActive(boolean active) {
        if (isActive() != active) {
            getElement().setProperty(DndUtil.DROP_TARGET_ACTIVE_PROPERTY,
                    active);
            DndUtil.updateDropTargetActivation(this);
            DndUtil.addMobileDndPolyfillIfNeeded(getDropTargetComponent());
            // only onetime thing when in development mode
            DndUtil.reportUsage();
        }
    }

    /**
     * Gets whether this drop target is activate or not. By default, it is not.
     * 
     * @return {@code true} to allow drops, {@code false} to not
     */
    default boolean isActive() {
        return getElement().getProperty(DndUtil.DROP_TARGET_ACTIVE_PROPERTY,
                false);
    }

    /**
     * Sets the drop effect for the current drop target. This is set to the
     * dropEffect on {@code dragenter}, {@code dragover} and {@code drop} events
     * and needs to match the {@code effectAllowed} property for the drag
     * operation.
     * <p>
     * <em>NOTE: If the drop effect that doesn't match the effectAllowed of the
     * drag source, it DOES NOT prevent drop on IE11 and Safari! For FireFox and
     * Chrome the drop is prevented if the properties don't match.</em>
     *
     * @param dropEffect
     *            the drop effect to be set or {@code null} to not modify
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/API/HTML_Drag_and_Drop_API/Drag_operations#drageffects">
     *      MDN web docs</a> for more information.
     */
    default void setDropEffect(DropEffect dropEffect) {
        if (!Objects.equals(getDropEffect(), dropEffect)) {
            if (dropEffect == null) {
                getElement()
                        .removeProperty(DndUtil.DROP_EFFECT_ELEMENT_PROPERTY);
            } else {
                getElement().setProperty(DndUtil.DROP_EFFECT_ELEMENT_PROPERTY,
                        dropEffect.toString().toLowerCase(Locale.ENGLISH));
            }
        }
    }

    /**
     * Returns the drop effect for the current drop target.
     *
     * @return The drop effect of this drop target or {@code null} if none set
     * @see #setDropEffect(DropEffect)
     */
    default DropEffect getDropEffect() {
        String dropEffect = getElement()
                .getProperty(DndUtil.DROP_EFFECT_ELEMENT_PROPERTY, null);
        return dropEffect == null ? null : DropEffect.fromString(dropEffect);
    }

    /**
     * Attaches drop listener for the component this maps to. The listener is
     * triggered when the user performs a drop operation on the client side, and
     * the criteria set with {@link #setDropEffect(DropEffect)} matches a one
     * set for the drag operation (see
     * {@link DragSource#setEffectAllowed(EffectAllowed)}).
     * <p>
     * <em>NOTE:</em> the drop listener might be triggered for a drop inside
     * another drop target that is inside this drop target component! For this,
     * the {@link #setActive(boolean)} does not have effect.
     *
     * @param listener
     *            Listener to handle drop event.
     * @return Handle to be used to remove this listener.
     */
    default Registration addDropListener(
            ComponentEventListener<DropEvent<T>> listener) {
        return ComponentUtil.addListener(getDropTargetComponent(),
                DropEvent.class, (ComponentEventListener) listener);
    }
}
