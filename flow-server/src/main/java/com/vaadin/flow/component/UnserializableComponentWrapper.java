/*-
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.function.Consumer;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementUtil;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.internal.StateTree;

@Tag(Tag.DIV)
public class UnserializableComponentWrapper<S extends Serializable, T extends Component>
        extends Component {

    private transient T component;
    private S state;
    private SerializableFunction<S, T> deserializer;
    private SerializableFunction<T, S> serializer;

    public UnserializableComponentWrapper(T component) {
        this.component = Objects.requireNonNull(component);
        getElement().appendChild(component.getElement());
    }

    private UnserializableComponentWrapper(T component,
            SerializableFunction<T, S> serializer,
            SerializableFunction<S, T> deserializer) {
        this.component = Objects.requireNonNull(component);
        this.serializer = Objects.requireNonNull(serializer);
        this.deserializer = Objects.requireNonNull(deserializer);
        getElement().appendChild(component.getElement());
    }

    public UnserializableComponentWrapper<S, T> withComponentDeserializer(
            SerializableFunction<S, T> deserializer) {
        this.deserializer = Objects.requireNonNull(deserializer);
        return this;
    }

    public UnserializableComponentWrapper<S, T> withComponentSerializer(
            SerializableFunction<T, S> serializer) {
        this.serializer = Objects.requireNonNull(serializer);
        return this;
    }

    @Serial
    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        state = serializer.apply(component);
        out.defaultWriteObject();
    }

    /**
     * Prepares the UI for serialization, removing unserializable components
     * from the component tree.
     * <p>
     * </p>
     * The changes to the UI caused by the removal are silently ignored.
     * <p>
     * </p>
     * <b>IMPORTANT NOTE:</b> any detach listener registered on the wrapped
     * components will be executed.
     *
     * @param ui
     *            The ui to prepare for serialization.
     */
    static void beforeSerialization(UI ui) {
        doWithWrapper(ui, wrapper -> {
            wrapper.component.removeFromParent();
            flush(wrapper);
        });
    }

    /**
     * Restores the UI adding the unserializable components to the component
     * tree.
     * <p>
     * </p>
     * The changes to the UI caused by re-adding the components are silently
     * ignored.
     * <p>
     * </p>
     * <b>IMPORTANT NOTE:</b> any attach listener registered on the wrapped
     * components will be executed.
     *
     * @param ui
     *            The ui to prepare for serialization.
     */
    static void afterSerialization(UI ui) {
        doWithWrapper(ui, wrapper -> {
            wrapper.state = null;
            wrapper.getElement().appendChild(wrapper.component.getElement());
            flush(wrapper);
        });
    }

    /**
     * Regenerates and adds unserializable components to the UI tree.
     * <p>
     * </p>
     * The changes to the UI caused by the removal are silently ignored.
     * <p>
     * </p>
     * <b>IMPORTANT NOTE:</b> any attach listener registered on the wrapped
     * component will be executed.
     *
     * @param ui
     *            The ui to which unserializable components should be added.
     */
    public static void afterDeserialization(UI ui) {
        doWithWrapper(ui, wrapper -> {
            wrapper.restoreComponent();
            flush(wrapper);
        });
    }

    private void restoreComponent() {
        component = deserializer.apply(state);
        getElement().appendChild(component.getElement());
    }

    private static void flush(UnserializableComponentWrapper<?, ?> wrapper) {
        if (wrapper.getElement().getNode()
                .getOwner() instanceof StateTree owner) {
            owner.collectChanges(change -> {
            });
        }
    }

    @SuppressWarnings("rawtypes")
    private static void doWithWrapper(UI ui,
            Consumer<UnserializableComponentWrapper> action) {
        ui.getElement().getNode().visitNodeTree(node -> ElementUtil.from(node)
                .flatMap(Element::getComponent)
                .filter(UnserializableComponentWrapper.class::isInstance)
                .map(UnserializableComponentWrapper.class::cast)
                .ifPresent(action));
    }
}
