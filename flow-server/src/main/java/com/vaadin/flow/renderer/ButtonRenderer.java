/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.renderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.util.HtmlUtils;

/**
 * 
 * A template renderer to create a clickable button.
 * <p>
 * {@link ItemClickListener}s are notified when the rendered buttons are either
 * clicked or tapped (in touch devices).
 * 
 * @author Vaadin Ltd.
 *
 * @param <SOURCE>
 *            the type of the item to be received in the click listeners
 */
public class ButtonRenderer<SOURCE> extends TemplateRenderer<SOURCE>
        implements ClickableRenderer<SOURCE> {

    private static AtomicInteger RENDERER_ID_GENERATOR = new AtomicInteger();
    private String template;
    private List<ItemClickListener<SOURCE>> listeners = new ArrayList<>(1);

    /**
     * Creates a new button renderer with the specified label. The label is the
     * same for all the items.
     * <p>
     * Item click listeners can be added via
     * {@link #addItemClickListener(com.vaadin.ui.renderers.ClickableRenderer.ItemClickListener)}.
     * 
     * @param label
     *            the label of the rendered button, not <code>null</code>
     */
    public ButtonRenderer(String label) {
        if (label == null) {
            throw new IllegalArgumentException("label may not be null");
        }

        int id = RENDERER_ID_GENERATOR.incrementAndGet();
        String eventName = "_" + getClass().getSimpleName() + "_" + id
                + "_event";

        template = "<button on-click='" + eventName + "'>"
                + HtmlUtils.escape(label) + "</button>";
        withEventHandler(eventName, this::onClick);
    }

    /**
     * Creates a new button renderer with the specified label and registers a
     * {@link ItemClickListener} to receive events. The label is the same for
     * all the items.
     * <p>
     * More click listeners can be added via
     * {@link #addItemClickListener(com.vaadin.ui.renderers.ClickableRenderer.ItemClickListener)}.
     * 
     * @param label
     *            the label for the rendered button, not <code>null</code>
     * @param clickListener
     *            a listener to receive click events
     */
    public ButtonRenderer(String label,
            ItemClickListener<SOURCE> clickListener) {
        this(label);
        addItemClickListener(clickListener);
    }

    /**
     * Creates a new button renderer with a dynamic label.
     * <p>
     * Item click listeners can be added via
     * {@link #addItemClickListener(com.vaadin.ui.renderers.ClickableRenderer.ItemClickListener)}.
     * 
     * @param labelProvider
     *            the provider for the labels of the rendered buttons, not
     *            <code>null</code>
     */
    public ButtonRenderer(ValueProvider<SOURCE, String> labelProvider) {
        if (labelProvider == null) {
            throw new IllegalArgumentException("labelProvider may not be null");
        }

        int id = RENDERER_ID_GENERATOR.incrementAndGet();
        String propertyName = "_" + getClass().getSimpleName() + "_" + id
                + "_label";
        String eventName = "_" + getClass().getSimpleName() + "_" + id
                + "_event";

        template = "<button on-click='" + eventName + "'>[[item." + propertyName
                + "]]</button>";
        withProperty(propertyName, labelProvider);
        withEventHandler(eventName, this::onClick);
    }

    /**
     * Creates a new button renderer with a dynamic label and registers a
     * {@link ItemClickListener} to receive events.
     * <p>
     * More click listeners can be added via
     * {@link #addItemClickListener(com.vaadin.ui.renderers.ClickableRenderer.ItemClickListener)}.
     * 
     * @param labelProvider
     *            the provider for the labels of the rendered buttons, not
     *            <code>null</code>
     * @param clickListener
     *            a listener to receive click events
     */
    public ButtonRenderer(ValueProvider<SOURCE, String> labelProvider,
            ItemClickListener<SOURCE> clickListener) {
        this(labelProvider);
        addItemClickListener(clickListener);
    }

    @Override
    public String getTemplate() {
        return template;
    }

    @Override
    public Registration addItemClickListener(
            ItemClickListener<SOURCE> listener) {

        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    @Override
    public List<ItemClickListener<SOURCE>> getItemClickListeners() {
        return Collections.unmodifiableList(listeners);
    }

}
