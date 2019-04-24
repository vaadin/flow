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
package com.vaadin.flow.component.dnd;

import java.util.Locale;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.shared.Registration;

/**
 * Mixin interface that provides basic drag source API for any component.
 * <p>
 * This can be used by either implementing this interface, or with the static
 * API {@link #of(Component)}.
 *
 * @param <T>
 *            the type of the drag source component
 * @see DropTarget
 * @author Vaadin Ltd
 * @since 2.0
 */
public interface DragSource<T extends Component> extends HasElement {

    /**
     * Makes the given component draggable and gives access to the generic drag
     * source API for the component.
     * <p>
     * The given component will be always set as draggable, if this is not
     * desired, use either method {@link #of(Component, boolean)} or
     * {@link #setDraggable(boolean)}.
     *
     * @param component
     *            the component to make draggable
     * @param <T>
     *            the type of the component
     * @return drag source API mapping to the component
     * @see #of(Component, boolean)
     */
    static <T extends Component> DragSource<T> of(T component) {
        return of(component, true);
    }

    /**
     * Gives access to the generic drag source API for the given component and
     * optionally makes it draggable.
     * <p>
     * The component draggable state can be changed later on with
     * {@link #setDraggable(boolean)}.
     * 
     * @param component
     *            the component to make draggable
     * @param draggable
     *            {@code true} to make draggable, {@code false} to not
     * @param <T>
     *            the type of the component
     * @return drag source API mapping to the component
     * @see #of(Component)
     */
    static <T extends Component> DragSource<T> of(T component,
            boolean draggable) {
        DragSource<T> dragSource = new DragSource<T>() {
            @Override
            public T getDragSourceComponent() {
                return component;
            }
        };
        dragSource.setDraggable(draggable);
        return dragSource;
    }

    /**
     * Returns the drag source component. This component is used in the drag
     * start and end events, and set as active drag source for the UI when
     * dragged.
     * 
     * @return the drag source component
     */
    /*
     * Implementation note: This could be mayhaps removed and replaced with
     * magic that digs the component from an element. But keeping this method
     * adds some convenience to the static usage. And it enables using an
     * element that is not the root element of this component as the draggable
     * element.
     */
    T getDragSourceComponent();

    /**
     * Returns the element where the {@code draggable} attribute is applied,
     * making it draggable by the user. By default it is the element of the
     * component returned by {@link #getDragSourceComponent()}.
     * 
     * @return the element made draggable
     */
    @Override
    default Element getElement() {
        return getDragSourceComponent().getElement();
    }

    /**
     * Sets this component as draggable. By default it is not.
     * 
     * @param draggable
     *            {@code true} for enable dragging, {@code false} to prevent
     */
    default void setDraggable(boolean draggable) {
        if (draggable == isDraggable()) {
            return;
        }
        if (draggable) {
            // The attribute is an enumerated one and not a Boolean one.
            getElement().setProperty("draggable", Boolean.TRUE.toString());
            getElement().executeJavaScript("window.Vaadin.Flow.dndConnector"
                    + ".activateDragSource($0)", getElement());
            // store & clear the component as active drag source for the UI
            Registration startListenerRegistration = addDragStartListener(
                    event -> getDragSourceComponent().getUI()
                            .orElseThrow(() -> new IllegalStateException(
                                    "DragSource not attached to an UI but received a drag start event."))
                            .getInternals().setActiveDragSourceComponent(
                                    getDragSourceComponent()));
            Registration endListenerRegistration = addDragEndListener(
                    event -> getDragSourceComponent().getUI()
                            .orElse(UI.getCurrent()).getInternals()
                            .setActiveDragSourceComponent(null));
            ComponentUtil.setData(getDragSourceComponent(),
                    Constants.START_LISTENER_REGISTRATION_KEY,
                    startListenerRegistration);
            ComponentUtil.setData(getDragSourceComponent(),
                    Constants.END_LISTENER_REGISTRATION_KEY,
                    endListenerRegistration);
        } else {
            getElement().removeProperty("draggable");
            getElement().executeJavaScript("window.Vaadin.Flow.dndConnector"
                    + ".deactivateDragSource($0)", getElement());
            // clear listeners for setting active data source
            Object startListenerRegistration = ComponentUtil.getData(
                    getDragSourceComponent(),
                    Constants.START_LISTENER_REGISTRATION_KEY);
            if (startListenerRegistration instanceof Registration) {
                ((Registration) startListenerRegistration).remove();
            }
            Object endListenerRegistration = ComponentUtil.getData(
                    getDragSourceComponent(),
                    Constants.END_LISTENER_REGISTRATION_KEY);
            if (endListenerRegistration instanceof Registration) {
                ((Registration) endListenerRegistration).remove();
            }
        }
    }

    /**
     * Is this component currently draggable. By default it is not.
     * 
     * @return {@code true} draggable, {@code false} if not
     */
    default boolean isDraggable() {
        return getElement().hasProperty("draggable");
    }

    /**
     * Set server side drag data. This data is available in the drop event and
     * can be used to transfer data between drag source and {@link DropTarget}
     * if they are in the same UI.
     *
     * @param data
     *            Data to transfer to drop event.
     * @see DropEvent#getDragData()
     */
    default void setDragData(Object data) {
        ComponentUtil.setData(getDragSourceComponent(),
                Constants.DRAG_SOURCE_DATA_KEY, data);
    }

    /**
     * Get server side drag data. This data is available in the drop event and
     * can be used to transfer data between drag source and drop target if they
     * are in the same UI.
     *
     * @return Server side drag data if set, otherwise {@literal null}.
     */
    default Object getDragData() {
        return ComponentUtil.getData(getDragSourceComponent(),
                Constants.DRAG_SOURCE_DATA_KEY);
    }

    /**
     * Sets the allowed effects for the current drag source element. Used for
     * setting client side {@code DataTransfer.effectAllowed} parameter for the
     * drag event.
     * <p>
     * By default the value is {@link EffectAllowed#UNINITIALIZED} which is
     * equivalent to {@link EffectAllowed#ALL}.
     * 
     * @param effect
     *            Effects to allow for this draggable element. Cannot be {@code
     *               null}.
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/API/HTML_Drag_and_Drop_API/Drag_operations#drageffects">
     *      MDN web docs</a> for more information.
     */
    default void setEffectAllowed(EffectAllowed effect) {
        if (effect == null) {
            throw new IllegalArgumentException("Allowed effect cannot be null");
        }
        getElement().setProperty(Constants.EFFECT_ALLOWED_ELEMENT_PROPERTY,
                effect.getClientPropertyValue());
    }

    /**
     * Returns the allowed effects for the current drag source element. Used to
     * set client side {@code DataTransfer.effectAllowed} parameter for the drag
     * event.
     * 
     * @return effects that are allowed for this draggable element.
     */
    default EffectAllowed getEffectAllowed() {
        return EffectAllowed.valueOf(getElement().getProperty(
                Constants.EFFECT_ALLOWED_ELEMENT_PROPERTY,
                EffectAllowed.UNINITIALIZED.getClientPropertyValue()
                        .toUpperCase(Locale.ENGLISH)));
    }

    /**
     * Attaches dragstart listener for the current drag source. The listener is
     * triggered when dragstart event happens on the client side.
     *
     * @param listener
     *            Listener to handle dragstart event.
     * @return Handle to be used to remove this listener.
     */
    default Registration addDragStartListener(
            ComponentEventListener<DragStartEvent<T>> listener) {
        return ComponentUtil.addListener(getDragSourceComponent(),
                DragStartEvent.class, (ComponentEventListener) listener);
    }

    /**
     * Attaches dragend listener for the current drag source.The listener is
     * triggered when dragend event happens on the client side.
     *
     * @param listener
     *            Listener to handle dragend event.
     * @return Handle to be used to remove this listener.
     */
    default Registration addDragEndListener(
            ComponentEventListener<DragEndEvent<T>> listener) {
        return ComponentUtil.addListener(getDragSourceComponent(),
                DragEndEvent.class, (ComponentEventListener) listener);
    }

}
