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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.shared.Registration;

/**
 * @author Vaadin Ltd
 *
 */
public class DragSourceComponent<T extends Component> extends Composite<T> {

    private final T origin;

    private Registration dragStartListenerHandle;
    private Registration dragEndListenerHandle;

    private EffectAllowed effectAllowed;

    private final Map<String, String> transferData = new LinkedHashMap<>();

    private final Map<String, Payload> payloads = new HashMap<>();

    /**
     * Stores the server side drag data that is available for the drop target if
     * it is in the same UI.
     */
    private Object dragData;

    public DragSourceComponent(T component) {
        origin = component;
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        // XXX: do it better in a detach listener
        dragStartListenerHandle.remove();
        dragEndListenerHandle.remove();
    }

    /**
     * Method invoked when a <code>dragstart</code> has been sent from client
     * side. Fires the {@link DragStartEvent}.
     */
    protected void onDragStart() {
        DragStartEvent<T> event = new DragStartEvent<>(getContent(),
                effectAllowed);
        fireEvent(event);
    }

    /**
     * Method invoked when a <code>dragend</code> has been sent from client
     * side. Fires the {@link DragEndEvent}.
     *
     * @param dropEffect
     *            the drop effect on the dragend
     */
    protected void onDragEnd(DropEffect dropEffect) {
        DragEndEvent<T> event = new DragEndEvent<>(getContent(), dropEffect);
        fireEvent(event);
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
     *         null}.
     */
    public void setEffectAllowed(EffectAllowed effect) {
        if (effect == null) {
            throw new IllegalArgumentException("Allowed effect cannot be null");
        }
        if (!Objects.equals(effectAllowed, effect)) {
            effectAllowed = effect;
        }
    }

    /**
     * Returns the allowed effects for the current drag source element. Used to
     * set client side {@code DataTransfer.effectAllowed} parameter for the drag
     * event.
     * <p>
     * You can use different types of data to support dragging to different
     * targets. Accepted types depend on the drop target and those can be
     * platform specific. See
     * https://developer.mozilla.org/en-US/docs/Web/API/HTML_Drag_and_Drop_API/Recommended_drag_types
     * for examples on different types.
     * <p>
     * <em>NOTE: IE11 only supports type ' text', which can be set using
     * {@link #setDataTransferText(String data)}</em>
     *
     * @return Effects that are allowed for this draggable element.
     */
    public EffectAllowed getEffectAllowed() {
        return effectAllowed;
    }

    /**
     * Sets data for this drag source element with the given type. The data is
     * set for the client side draggable element using {@code
     * DataTransfer.setData(type, data)} method.
     * <p>
     * Note that {@code "text"} is the only cross browser supported data type.
     * Use {@link #setDataTransferText(String)} method instead if your
     * application supports IE11.
     *
     * @param type
     *            Type of the data to be set for the client side draggable
     *            element, e.g. {@code text/plain}. Cannot be {@code null}.
     * @param data
     *            Data to be set for the client side draggable element. Cannot
     *            be {@code null}.
     */
    public void setDataTransferData(String type, String data) {
        if (type == null) {
            throw new IllegalArgumentException("Data type cannot be null");
        }

        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }

        transferData.put(type, data);
    }

    /**
     * Returns the data stored with type {@code type} in this drag source
     * element.
     *
     * @param type
     *            Type of the requested data, e.g. {@code text/plain}.
     * @return Data of type {@code type} stored in this drag source element.
     */
    public String getDataTransferData(String type) {
        return transferData.get(type);
    }

    /**
     * Returns the map of data stored in this drag source element. The returned
     * map preserves the order of storage and is unmodifiable.
     *
     * @return Unmodifiable copy of the map of data in the order the data was
     *         stored.
     */
    public Map<String, String> getDataTransferData() {
        return Collections.unmodifiableMap(transferData);
    }

    /**
     * Sets data of type {@code "text"} for this drag source element. The data
     * is set for the client side draggable element using the {@code
     * DataTransfer.setData("text", data)} method.
     * <p>
     * Note that {@code "text"} is the only cross browser supported data type.
     * Use this method if your application supports IE11.
     *
     * @param data
     *            Data to be set for the client side draggable element.
     * @see #setDataTransferData(String, String)
     */
    public void setDataTransferText(String data) {
        setDataTransferData(DnDConstants.DATA_TYPE_TEXT, data);
    }

    /**
     * Returns the data stored with type {@code "text"} in this drag source
     * element.
     *
     * @return Data of type {@code "text"} stored in this drag source element.
     */
    public String getDataTransferText() {
        return getDataTransferData(DnDConstants.DATA_TYPE_TEXT);
    }

    /**
     * Clears data with the given type for this drag source element when
     * present.
     *
     * @param type
     *            Type of data to be cleared. Cannot be {@code null}.
     */
    public void clearDataTransferData(String type) {
        if (type == null) {
            throw new IllegalArgumentException("Data type cannot be null");
        }

        transferData.remove(type);
    }

    /**
     * Clears all data for this drag source element.
     */
    public void clearDataTransferData() {
        transferData.clear();
    }

    /**
     * Sets payload for this drag source to use with acceptance criterion. The
     * payload is transferred as data type in the data transfer object in the
     * following format: {@code "v-item:string:key:value"}. The given value is
     * compared to the criterion value when the drag source is dragged on top of
     * a drop target that has the suitable criterion.
     * <p>
     * Note that setting payload in Internet Explorer 11 is not possible due to
     * the browser's limitations.
     *
     * @param key
     *            key of the payload to be transferred
     * @param value
     *            value of the payload to be transferred
     * @see DropTargetExtension#setDropCriterion(String, String)
     */
    public void setPayload(String key, String value) {
        setPayload(key, String.valueOf(value), Payload.ValueType.STRING);
    }

    /**
     * Sets payload for this drag source to use with acceptance criterion. The
     * payload is transferred as data type in the data transfer object in the
     * following format: {@code "v-item:integer:key:value"}. The given value is
     * compared to the criterion value when the drag source is dragged on top of
     * a drop target that has the suitable criterion.
     * <p>
     * Note that setting payload in Internet Explorer 11 is not possible due to
     * the browser's limitations.
     *
     * @param key
     *            key of the payload to be transferred
     * @param value
     *            value of the payload to be transferred
     * @see DropTargetExtension#setDropCriterion(String,
     *      com.vaadin.shared.ui.dnd.criteria.ComparisonOperator, int)
     *      DropTargetExtension#setDropCriterion(String, ComparisonOperator,
     *      int)
     */
    public void setPayload(String key, int value) {
        setPayload(key, String.valueOf(value), Payload.ValueType.INTEGER);
    }

    /**
     * Sets payload for this drag source to use with acceptance criterion. The
     * payload is transferred as data type in the data transfer object in the
     * following format: {@code "v-item:double:key:value"}. The given value is
     * compared to the criterion value when the drag source is dragged on top of
     * a drop target that has the suitable criterion.
     * <p>
     * Note that setting payload in Internet Explorer 11 is not possible due to
     * the browser's limitations.
     *
     * @param key
     *            key of the payload to be transferred
     * @param value
     *            value of the payload to be transferred
     * @see DropTargetExtension#setDropCriterion(String,
     *      com.vaadin.shared.ui.dnd.criteria.ComparisonOperator, double)
     *      DropTargetExtension#setDropCriterion(String, ComparisonOperator,
     *      double)
     */
    public void setPayload(String key, double value) {
        setPayload(key, String.valueOf(value), Payload.ValueType.DOUBLE);
    }

    private void setPayload(String key, String value,
            Payload.ValueType valueType) {
        payloads.put(key, new Payload(key, value, valueType));
    }

    /**
     * Set server side drag data. This data is available in the drop event and
     * can be used to transfer data between drag source and drop target if they
     * are in the same UI.
     *
     * @param data
     *            Data to transfer to drop event.
     */
    public void setDragData(Object data) {
        dragData = data;
    }

    /**
     * Get server side drag data. This data is available in the drop event and
     * can be used to transfer data between drag source and drop target if they
     * are in the same UI.
     *
     * @return Server side drag data if set, otherwise {@literal null}.
     */
    public Object getDragData() {
        return dragData;
    }

    /**
     * Attaches dragstart listener for the current drag source.
     * {@link DragStartListener#dragStart(DragStartEvent)} is called when
     * dragstart event happens on the client side.
     *
     * @param listener
     *            Listener to handle dragstart event.
     * @return Handle to be used to remove this listener.
     */
    public Registration addDragStartListener(DragStartListener<T> listener) {
        return addListener(DragStartEvent.class,
                (ComponentEventListener) listener);
    }

    /**
     * Attaches dragend listener for the current drag source.
     * {@link DragEndListener#dragEnd(DragEndEvent)} is called when dragend
     * event happens on the client side.
     *
     * @param listener
     *            Listener to handle dragend event.
     * @return Handle to be used to remove this listener.
     */
    public Registration addDragEndListener(DragEndListener<T> listener) {
        return addListener(DragEndEvent.class,
                (ComponentEventListener) listener);
    }

    /**
     * Set a custom drag image for the current drag source.
     *
     * XXX : what type should be {@code imageResource} ? In FW8 it's a
     * {@code Resource} which doesn't exist in Flow. MAy be it should be just a
     * {@code String} ?
     *
     * @param imageResource
     *            Resource of the image to be displayed as drag image.
     */
    public void setDragImage(Component imageResource) {
    }

    @Override
    protected T initContent() {
        return origin;
    }

    /**
     * Initializes dragstart and -end event listeners for this drag source to
     * capture the active drag source for the UI.
     */
    private void initListeners() {
        // XXX: Do we need this at all?
        // Set current extension as active drag source in the UI
        /*
         * dragStartListenerHandle = addDragStartListener( event ->
         * getUI().setActiveDragSource(this));
         */
        // Remove current extension as active drag source from the UI
        /*
         * dragEndListenerHandle = addDragEndListener( event ->
         * getUI().setActiveDragSource(null));
         */
    }
}
