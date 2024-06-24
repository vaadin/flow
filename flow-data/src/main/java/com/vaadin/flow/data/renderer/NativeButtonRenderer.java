/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.renderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.shared.Registration;

/**
 *
 * A template renderer to create a clickable button.
 * <p>
 * {@link ItemClickListener}s are notified when the rendered buttons are either
 * clicked or tapped (in touch devices).
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <SOURCE>
 *            the type of the item to be received in the click listeners
 */
public class NativeButtonRenderer<SOURCE> extends BasicRenderer<SOURCE, String>
        implements ClickableRenderer<SOURCE> {

    private List<ItemClickListener<SOURCE>> listeners = new ArrayList<>(1);

    /**
     * Creates a new button renderer with the specified label. The label is the
     * same for all the items.
     * <p>
     * Item click listeners can be added via
     * {@link #addItemClickListener(ItemClickListener)}.
     *
     * @param label
     *            the label of the rendered button, not <code>null</code>
     */
    public NativeButtonRenderer(String label) {
        this(value -> label);
    }

    /**
     * Creates a new button renderer with the specified label and registers a
     * {@link ItemClickListener} to receive events. The label is the same for
     * all the items.
     * <p>
     * More click listeners can be added via
     * {@link #addItemClickListener(ItemClickListener)}.
     *
     * @param label
     *            the label for the rendered button, not <code>null</code>
     * @param clickListener
     *            a listener to receive click events
     */
    public NativeButtonRenderer(String label,
            ItemClickListener<SOURCE> clickListener) {
        this(label);
        addItemClickListener(clickListener);
    }

    /**
     * Creates a new button renderer with a dynamic label.
     * <p>
     * Item click listeners can be added via
     * {@link #addItemClickListener(ItemClickListener)}.
     *
     * @param labelProvider
     *            the provider for the labels of the rendered buttons, not
     *            <code>null</code>
     */
    public NativeButtonRenderer(ValueProvider<SOURCE, String> labelProvider) {
        super(labelProvider);
    }

    /**
     * Creates a new button renderer with a dynamic label and registers a
     * {@link ItemClickListener} to receive events.
     * <p>
     * More click listeners can be added via
     * {@link #addItemClickListener(ItemClickListener)}.
     *
     * @param labelProvider
     *            the provider for the labels of the rendered buttons, not
     *            <code>null</code>
     * @param clickListener
     *            a listener to receive click events
     */
    public NativeButtonRenderer(ValueProvider<SOURCE, String> labelProvider,
            ItemClickListener<SOURCE> clickListener) {
        this(labelProvider);
        addItemClickListener(clickListener);
    }

    @Override
    public Registration addItemClickListener(
            ItemClickListener<SOURCE> listener) {
        return Registration.addAndRemove(listeners, listener);
    }

    @Override
    public List<ItemClickListener<SOURCE>> getItemClickListeners() {
        return Collections.unmodifiableList(listeners);
    }

    @Override
    protected String getTemplateForProperty(String property,
            Rendering<SOURCE> context) {
        String templatePropertyName = getTemplatePropertyName(context);
        String eventName = templatePropertyName + "_event";
        String disabledName = templatePropertyName + "_disabled";
        setEventHandler(eventName, this::onClick);
        return String.format(
                "<button on-click=\"%s\" disabled=\"[[item.%s]]\">%s</button>",
                eventName, disabledName, property);
    }

    @Override
    public Component createComponent(SOURCE item) {
        Element button = ElementFactory
                .createButton(getValueProvider().apply(item));
        button.addEventListener("click", event -> getItemClickListeners()
                .forEach(listeners -> listeners.onItemClicked(item)));
        return ComponentUtil.componentFromElement(button, Component.class,
                true);
    }

}
