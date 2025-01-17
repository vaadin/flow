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
package com.vaadin.flow.component.dnd;

import java.util.Locale;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dnd.internal.DndUtil;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.internal.nodefeature.VirtualChildrenList;
import com.vaadin.flow.shared.Registration;

/**
 * Mixin interface that provides basic drag source API for any component.
 * <p>
 * This can be used by either implementing this interface, or with the static
 * API {@link #create(Component)}, {@link #configure(Component)} or
 * {@link #configure(Component, boolean)}.
 *
 * @param <T>
 *            the type of the drag source component
 * @see DropTarget
 * @author Vaadin Ltd
 * @since 2.0
 */
@JsModule(DndUtil.DND_CONNECTOR)
public interface DragSource<T extends Component> extends HasElement {

    /**
     * Makes the given component draggable and gives access to the generic drag
     * source API for the component.
     * <p>
     * The given component will be always set as draggable, if this is not
     * desired, use either method {@link #configure(Component, boolean)} or
     * {@link #setDraggable(boolean)}.
     *
     * @param component
     *            the component to make draggable
     * @param <T>
     *            the type of the component
     * @return drag source API mapping to the component
     * @see #configure(Component)
     * @see #configure(Component, boolean)
     */
    static <T extends Component> DragSource<T> create(T component) {
        return configure(component, true);
    }

    /**
     * Gives access to the generic drag source API for the given component.
     * <p>
     * Unlike {@link #create(Component)} and
     * {@link #configure(Component, boolean)}, this method does not change the
     * active drop target status of the given component.
     *
     * @param component
     *            the component to make draggable
     * @param <T>
     *            the type of the component
     * @return drag source API mapping to the component
     * @see #create(Component)
     * @see #configure(Component, boolean)
     */
    static <T extends Component> DragSource<T> configure(T component) {
        return new DragSource<T>() {
            @Override
            public T getDragSourceComponent() {
                return component;
            }
        };
    }

    /**
     * Gives access to the generic drag source API for the given component and
     * applies the given draggable status to it.
     * <p>
     * This method is a shorthand for calling {@link #configure(Component)} and
     * {@link #setDraggable(boolean)}.
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
     * @see #create(Component)
     * @see #configure(Component, boolean)
     */
    static <T extends Component> DragSource<T> configure(T component,
            boolean draggable) {
        DragSource<T> dragSource = configure(component);
        dragSource.setDraggable(draggable);
        return dragSource;
    }

    /**
     * Returns the drag source component. This component is used in the drag
     * start and end events, and set as active drag source for the UI when
     * dragged.
     * <p>
     * The default implementation of this method returns {@code this}. This
     * method exists for type safe access for the drag source component.
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
    default T getDragSourceComponent() {
        return (T) this;
    };

    @Override
    default Element getElement() {
        return getDragSourceComponent().getElement();
    }

    /**
     * Returns the element where the {@code draggable} attribute is applied,
     * making it draggable by the user. By default it is the element of the
     * component returned by {@link #getDragSourceComponent()}.
     * <p>
     * Override this method to provide another element to be draggable instead
     * of the root element of the component.
     *
     * @return the element made draggable
     * @since 2.1
     */
    /*
     * Implementation note: this is added so that user can change the draggable
     * element to be something else than the root element of the component.
     */
    default Element getDraggableElement() {
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
            getDraggableElement().setProperty("draggable",
                    Boolean.TRUE.toString());
            DndUtil.updateDragSourceActivation(this);

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
                    DndUtil.START_LISTENER_REGISTRATION_KEY,
                    startListenerRegistration);
            ComponentUtil.setData(getDragSourceComponent(),
                    DndUtil.END_LISTENER_REGISTRATION_KEY,
                    endListenerRegistration);
        } else {
            getDraggableElement().removeProperty("draggable");
            DndUtil.updateDragSourceActivation(this);
            // clear listeners for setting active data source
            Object startListenerRegistration = ComponentUtil.getData(
                    getDragSourceComponent(),
                    DndUtil.START_LISTENER_REGISTRATION_KEY);
            if (startListenerRegistration instanceof Registration) {
                ((Registration) startListenerRegistration).remove();
            }
            Object endListenerRegistration = ComponentUtil.getData(
                    getDragSourceComponent(),
                    DndUtil.END_LISTENER_REGISTRATION_KEY);
            if (endListenerRegistration instanceof Registration) {
                ((Registration) endListenerRegistration).remove();
            }
        }
        // only onetime thing when in development mode
        DndUtil.reportUsage();
    }

    /**
     * Is this component currently draggable. By default it is not.
     *
     * @return {@code true} draggable, {@code false} if not
     */
    default boolean isDraggable() {
        return getDraggableElement().hasProperty("draggable");
    }

    /**
     * Set server side drag data. This data is available in the drop event and
     * can be used to transfer data between drag source and {@link DropTarget}
     * if they are in the same UI.
     * <p>
     * The drag data can be set also in the drag start event listener added with
     * {@link #addDragStartListener(ComponentEventListener)} using
     * {@link DragStartEvent#setDragData(Object)}.
     *
     * @param data
     *            Data to transfer to drop event.
     * @see DropEvent#getDragData()
     * @see DragStartEvent#setDragData(Object)
     * @see DragEndEvent#clearDragData()
     */
    default void setDragData(Object data) {
        ComponentUtil.setData(getDragSourceComponent(),
                DndUtil.DRAG_SOURCE_DATA_KEY, data);
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
                DndUtil.DRAG_SOURCE_DATA_KEY);
    }

    /**
     * Sets the allowed effects for the current drag source element. Used for
     * setting client side {@code DataTransfer.effectAllowed} parameter for the
     * drag event.
     * <p>
     * By default the value is {@link EffectAllowed#UNINITIALIZED} which is
     * equivalent to {@link EffectAllowed#ALL}.
     * <p>
     * <em>NOTE:</em> The effect should be set in advance, setting it after the
     * user has started dragging and the {@link DragStartEvent} has been fired
     * is too late - it will take effect only for next drag operation.
     * <p>
     * <em>NOTE 2: Edge, Safari and IE11 will allow the drop to occur even when
     * the effect allowed does not match the drop effect set on the drop target.
     * Chrome and Firefox prevent the drop if those do not match.</em>
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
        getDraggableElement().setProperty(
                DndUtil.EFFECT_ALLOWED_ELEMENT_PROPERTY,
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
        return EffectAllowed.valueOf(getDraggableElement().getProperty(
                DndUtil.EFFECT_ALLOWED_ELEMENT_PROPERTY,
                EffectAllowed.UNINITIALIZED.getClientPropertyValue()
                        .toUpperCase(Locale.ENGLISH)));
    }

    /**
     * Sets the drag image for the current drag source element. The image is
     * applied automatically in the next drag start event in the browser. Drag
     * image is shown by default with zero offset which means that pointer
     * location is in the top left corner of the image.
     * <p>
     * {@code com.vaadin.flow.component.html.Image} is fully supported as a drag
     * image component. Other components can be used as well, but the support
     * may vary between browsers. If given component is visible element in the
     * viewport, browser can show it as a drag image.
     *
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/API/DataTransfer/setDragImage">
     *      MDN web docs</a> for more information.
     * @param dragImage
     *            the image to be used as drag image or null to remove it
     */
    default void setDragImage(Component dragImage) {
        setDragImage(dragImage, 0, 0);
    }

    /**
     * Sets the drag image for the current drag source element. The image is
     * applied automatically in the next drag start event in the browser.
     * Coordinates define the offset of the pointer location from the top left
     * corner of the image.
     * <p>
     * {@code com.vaadin.flow.component.html.Image} is fully supported as a drag
     * image component. Other components can be used as well, but the support
     * may vary between browsers. If given component is visible element in the
     * viewport, browser can show it as a drag image.
     *
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/API/DataTransfer/setDragImage">
     *      MDN web docs</a> for more information.
     * @param dragImage
     *            the image to be used as drag image or null to remove it
     * @param offsetX
     *            the x-offset of the drag image
     * @param offsetY
     *            the y-offset of the drag image
     */
    default void setDragImage(Component dragImage, int offsetX, int offsetY) {
        if (dragImage != null && !dragImage.isVisible()) {
            throw new IllegalStateException(
                    "Drag image element is not visible and will not show.\nMake element visible to use as drag image!");
        }
        if (getDragImage() != null && getDragImage() != dragImage) {
            // Remove drag image from the virtual children list if it's there.
            if (getDraggableElement().getNode()
                    .hasFeature(VirtualChildrenList.class)) {
                VirtualChildrenList childrenList = getDraggableElement()
                        .getNode().getFeature(VirtualChildrenList.class);
                // dodging exception with empty list
                if (childrenList.size() > 0) {
                    getDraggableElement()
                            .removeVirtualChild(getDragImage().getElement());
                }
            }
        }
        if (dragImage != null && !dragImage.isAttached()) {
            if (!getDragSourceComponent().isAttached()) {
                getDragSourceComponent().addAttachListener(event -> {
                    if (!dragImage.isAttached()
                            && dragImage.getParent().isEmpty()) {
                        appendDragElement(dragImage.getElement());
                    }
                    event.unregisterListener();
                });
            } else {
                appendDragElement(dragImage.getElement());
            }
        }
        ComponentUtil.setData(getDragSourceComponent(),
                DndUtil.DRAG_SOURCE_IMAGE, dragImage);
        getDraggableElement().executeJs(
                "window.Vaadin.Flow.dndConnector.setDragImage($0, $1, $2, $3)",
                dragImage, (dragImage == null ? 0 : offsetX),
                (dragImage == null ? 0 : offsetY), getDraggableElement());
    }

    private void appendDragElement(Element dragElement) {
        if (dragElement.getTag().equals("img")) {
            getDraggableElement().appendVirtualChild(dragElement);
        } else {
            LoggerFactory.getLogger(DragSource.class).debug(
                    "Attaching child to dom in position -100,-100. Consider adding the component manually to not get overlapping components on drag for element.");
            getDraggableElement().appendChild(dragElement);
            Style style = dragElement.getStyle();
            style.set("position", "absolute");
            style.set("top", "-100px");
            style.set("left", "-100px");
            style.set("display", "none");
        }
    }

    /**
     * Get server side drag image. This image is applied automatically in the
     * next drag start event in the browser.
     *
     * @return Server side drag image if set, otherwise {@literal null}.
     */
    default Component getDragImage() {
        return (Component) ComponentUtil.getData(getDragSourceComponent(),
                DndUtil.DRAG_SOURCE_IMAGE);
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
