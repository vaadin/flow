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
package com.vaadin.flow.component.html;

import java.util.Objects;
import java.util.Optional;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasOrderedComponents;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.SignalPropertySupport;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.streams.AbstractDownloadHandler;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.signals.Signal;

/**
 * Component representing a <code>&lt;object&gt;</code> element.
 *
 * @author Vaadin Ltd
 *
 */
@Tag(Tag.OBJECT)
public class HtmlObject extends HtmlContainer implements
        ClickNotifier<HtmlObject>, HasOrderedComponents, Focusable<HtmlObject> {

    private static final PropertyDescriptor<String, String> dataDescriptor = PropertyDescriptors
            .attributeWithDefault("data", "");

    private static final PropertyDescriptor<String, Optional<String>> typeDescriptor = PropertyDescriptors
            .optionalAttributeWithDefault("type", "");

    private final SignalPropertySupport<String> dataProperty = SignalPropertySupport
            .create(this, value -> set(dataDescriptor, value));

    /**
     * Creates a new <code>&lt;object&gt;</code> component.
     */
    public HtmlObject() {
    }

    /**
     * Creates a new <code>&lt;object&gt;</code> component with given data and
     * type attribute values.
     *
     * @see #setData(String)
     * @see #setType(String)
     *
     *
     * @param data
     *            a data attribute value
     * @param type
     *            a type attribute value
     */
    public HtmlObject(String data, String type) {
        setData(data);
        setType(type);
    }

    /**
     * Creates a new <code>&lt;object&gt;</code> component with given data, type
     * attribute values and "param" components.
     *
     * @see #setData(String)
     * @see #setType(String)
     * @see #add(Component...)
     *
     *
     * @param data
     *            a data attribute value
     * @param type
     *            a type attribute value
     * @param params
     *            parameter components
     */
    public HtmlObject(String data, String type, Param... params) {
        setData(data);
        setType(type);
        add(params);
    }

    /**
     * Creates a new <code>&lt;object&gt;</code> component with its data
     * attribute bound to the given signal and with the given type attribute.
     *
     * @param dataSignal
     *            the signal to bind, not {@code null}
     * @param type
     *            a type attribute value
     * @see #bindData(Signal)
     * @see #setType(String)
     */
    public HtmlObject(Signal<String> dataSignal, String type) {
        Objects.requireNonNull(dataSignal, "dataSignal must not be null");
        dataProperty.bind(dataSignal);
        setType(type);
    }

    /**
     * Creates a new <code>&lt;object&gt;</code> component with its data
     * attribute bound to the given signal, the given type attribute and the
     * provided "param" children.
     *
     * @param dataSignal
     *            the signal to bind, not {@code null}
     * @param type
     *            a type attribute value
     * @param params
     *            parameter components
     * @see #bindData(Signal)
     * @see #setType(String)
     * @see #add(Component...)
     */
    public HtmlObject(Signal<String> dataSignal, String type, Param... params) {
        Objects.requireNonNull(dataSignal, "dataSignal must not be null");
        dataProperty.bind(dataSignal);
        setType(type);
        add(params);
    }

    /**
     * Creates a new <code>&lt;object&gt;</code> component with given data
     * resource and type value.
     *
     * @see #setData(AbstractStreamResource)
     * @see #setType(String)
     *
     * @param data
     *            the resource value, not null
     * @param type
     *            a type attribute value
     * @deprecated use {@link #HtmlObject(DownloadHandler,String)} instead
     */
    @Deprecated(since = "24.8", forRemoval = true)
    public HtmlObject(AbstractStreamResource data, String type) {
        setData(data);
        setType(type);
    }

    /**
     * Creates a new <code>&lt;object&gt;</code> component with given data
     * resource, type value and "param" components.
     *
     * @see #setData(String)
     * @see #setType(String)
     * @see #add(Component...)
     *
     *
     * @param data
     *            a data attribute value
     * @param type
     *            a type attribute value
     * @param params
     *            parameter components
     * @deprecated use {@link #HtmlObject(DownloadHandler,String, Param...)}
     *             instead
     */
    @Deprecated(since = "24.8", forRemoval = true)
    public HtmlObject(AbstractStreamResource data, String type,
            Param... params) {
        setData(data);
        setType(type);
        add(params);
    }

    /**
     * Creates a new <code>&lt;object&gt;</code> component with given
     * {@link DownloadHandler} callback for providing an object data and type
     * value.
     * <p>
     * Sets the <code>Content-Disposition</code> header to <code>inline</code>
     * for pre-defined download handlers, created by factory methods in
     * {@link DownloadHandler}, as well as for other
     * {@link AbstractDownloadHandler} implementations.
     *
     * @see #setData(DownloadHandler)
     * @see #setType(String)
     *
     * @param data
     *            the callback for providing resource data, not null
     * @param type
     *            a type attribute value
     */
    public HtmlObject(DownloadHandler data, String type) {
        setData(data);
        setType(type);
    }

    /**
     * Creates a new <code>&lt;object&gt;</code> component with given data
     * resource, type value and "param" components.
     * <p>
     * Sets the <code>Content-Disposition</code> header to <code>inline</code>
     * for pre-defined download handlers, created by factory methods in
     * {@link DownloadHandler}, as well as for other
     * {@link AbstractDownloadHandler} implementations.
     *
     * @see #setData(String)
     * @see #setType(String)
     * @see #add(Component...)
     *
     *
     * @param data
     *            a data attribute value
     * @param type
     *            a type attribute value
     * @param params
     *            parameter components
     */
    public HtmlObject(DownloadHandler data, String type, Param... params) {
        setData(data);
        setType(type);
        add(params);
    }

    /**
     * Creates a new <code>&lt;object&gt;</code> component with given data
     * resource, type value and "param" components.
     * <p>
     * Sets the <code>Content-Disposition</code> header to <code>inline</code>
     * for pre-defined download handlers, created by factory methods in
     * {@link DownloadHandler}, as well as for other
     * {@link AbstractDownloadHandler} implementations.
     *
     * @see #setData(String)
     * @see #setType(String)
     * @see #add(Component...)
     *
     *
     * @param data
     *            a handler that defines the data to be set to this object
     *            component
     * @param params
     *            parameter components
     */
    public HtmlObject(DownloadHandler data, Param... params) {
        setData(data);
        add(params);
    }

    /**
     * Creates a new <code>&lt;object&gt;</code> component with given data
     * resource, type value and "param" components.
     * <p>
     * Sets the <code>Content-Disposition</code> header to <code>inline</code>
     * for pre-defined download handlers, created by factory methods in
     * {@link DownloadHandler}, as well as for other
     * {@link AbstractDownloadHandler} implementations.
     *
     * @see #setData(String)
     * @see #setType(String)
     * @see #add(Component...)
     *
     *
     * @param data
     *            a handler that defines the data to be set to this object
     *            component
     */
    public HtmlObject(DownloadHandler data) {
        setData(data);
    }

    /**
     * Creates a new <code>&lt;object&gt;</code> component with given data and
     * "param" components.
     *
     * @see #setData(String)
     * @see #add(Component...)
     *
     *
     * @param data
     *            a data attribute value
     * @param params
     *            parameter components
     */
    public HtmlObject(String data, Param... params) {
        setData(data);
        add(params);
    }

    /**
     * Creates a new <code>&lt;object&gt;</code> component with its data
     * attribute bound to the given signal.
     *
     * @param dataSignal
     *            the signal to bind, not {@code null}
     * @see #bindData(Signal)
     */
    public HtmlObject(Signal<String> dataSignal) {
        Objects.requireNonNull(dataSignal, "dataSignal must not be null");
        dataProperty.bind(dataSignal);
    }

    /**
     * Creates a new <code>&lt;object&gt;</code> component with its data
     * attribute bound to the given signal and the provided "param" children.
     *
     * @param dataSignal
     *            the signal to bind, not {@code null}
     * @param params
     *            parameter components
     * @see #bindData(Signal)
     * @see #add(Component...)
     */
    public HtmlObject(Signal<String> dataSignal, Param... params) {
        Objects.requireNonNull(dataSignal, "dataSignal must not be null");
        dataProperty.bind(dataSignal);
        add(params);
    }

    /**
     * Creates a new <code>&lt;object&gt;</code> component with given data
     * resource value.
     *
     * @see #setData(AbstractStreamResource)
     *
     * @param data
     *            the resource value, not {@code null}
     * @deprecated use {@link #HtmlObject(DownloadHandler)} instead
     */
    @Deprecated(since = "24.8", forRemoval = true)
    public HtmlObject(AbstractStreamResource data) {
        setData(data);
    }

    /**
     * Creates a new <code>&lt;object&gt;</code> component with given data
     * resource value and "param" components.
     *
     * @see #setData(AbstractStreamResource)
     * @see #add(Component...)
     *
     * @param data
     *            the resource value, not {@code null}
     * @param params
     *            parameter components
     * @deprecated use {@link #HtmlObject(DownloadHandler,Param...)} instead
     */
    @Deprecated(since = "24.8", forRemoval = true)
    public HtmlObject(AbstractStreamResource data, Param... params) {
        setData(data);
        add(params);
    }

    /**
     * Sets the "data" attribute value.
     *
     * @param data
     *            a "data" attribute value
     */
    public void setData(String data) {
        set(dataDescriptor, data);
    }

    /**
     * Sets the {@link StreamResource} URL as "data" attribute value .
     *
     * @param data
     *            a "data" attribute value,, not {@code null}
     * @deprecated use {@link #setData(DownloadHandler)} instead
     */
    @Deprecated(since = "24.8", forRemoval = true)
    public void setData(AbstractStreamResource data) {
        getElement().setAttribute("data", data);
    }

    /**
     * Sets the URL for {@link DownloadHandler} callback as "data" attribute
     * value.
     * <p>
     * Sets the <code>Content-Disposition</code> header to <code>inline</code>
     * for pre-defined download handlers, created by factory methods in
     * {@link DownloadHandler}, as well as for other
     * {@link AbstractDownloadHandler} implementations.
     *
     * @param data
     *            a "data" attribute value, not {@code null}
     */
    public void setData(DownloadHandler data) {
        if (data instanceof AbstractDownloadHandler<?> handler) {
            // change disposition to inline in pre-defined handlers,
            // where it is 'attachment' by default
            handler.inline();
        }
        getElement().setAttribute("data", data);
    }

    /**
     * Gets the "data" attribute value.
     *
     * @return the "data" attribute value
     *
     * @see #setData(String)
     * @see #setData(DownloadHandler)
     */
    public String getData() {
        return get(dataDescriptor);
    }

    /**
     * Binds a {@link com.vaadin.signals.Signal}'s value to the {@code data}
     * attribute of this component and keeps the attribute synchronized with the
     * signal value while the component is in the attached state. When the
     * component is in the detached state, signal value changes have no effect.
     * Passing {@code null} as the {@code signal} unbinds any existing binding.
     * <p>
     * Trying to bind a new signal while one is already bound throws
     * {@link com.vaadin.signals.BindingActiveException}.
     * <p>
     * The semantics follow
     * {@link com.vaadin.flow.component.HasText#bindText(Signal)} regarding
     * lifecycle and unbinding.
     * <p>
     * Example of usage:
     * 
     * <pre>
     * ValueSignal&lt;String&gt; signal = new ValueSignal&lt;&gt;("");
     * HtmlObject component = new HtmlObject();
     * add(component);
     * component.bindData(signal);
     * signal.value("/path/to/data"); // The component "data" attribute is set
     *                                // accordingly
     * </pre>
     *
     * @param dataSignal
     *            the signal to bind or <code>null</code> to unbind any existing
     *            binding
     * @throws com.vaadin.signals.BindingActiveException
     *             thrown when there is already an existing binding
     * @see #setData(String)
     */
    public void bindData(Signal<String> dataSignal) {
        dataProperty.bind(dataSignal);
    }

    /**
     * Sets the "type" attribute value.
     *
     * @param type
     *            a "type" attribute value
     */
    public void setType(String type) {
        set(typeDescriptor, type);
    }

    /**
     * Gets the "type" attribute value.
     *
     * @see #setType(String)
     *
     * @return the "type" attribute value
     */
    public Optional<String> getType() {
        return get(typeDescriptor);
    }

}
