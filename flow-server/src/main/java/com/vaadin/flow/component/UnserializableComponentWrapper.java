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
    private SerializableFunction<S, T> generator;
    private SerializableFunction<T, S> saver;

    public UnserializableComponentWrapper(T component) {
        getElement().appendChild(component.getElement());
        this.component = component;
    }

    public static <S extends Serializable, T extends Component> UnserializableComponentWrapper<S, T> of(
            T component) {
        return new UnserializableComponentWrapper<>(component);
    }

    public UnserializableComponentWrapper<S, T> withGenerator(
            SerializableFunction<S, T> generator) {
        this.generator = generator;
        return this;
    }

    public UnserializableComponentWrapper<S, T> withSaver(
            SerializableFunction<T, S> saver) {
        this.saver = saver;
        return this;
    }

    @Serial
    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        state = saver.apply(component);
        if (!component.isAttached()) {
            out.defaultWriteObject();
        } else {
            throw new IllegalStateException("Component still attached");
        }
    }

    static void beforeSerialization(UI ui) {
        doWithWrapper(ui, wrapper -> {
            wrapper.component.removeFromParent();
            flush(wrapper);
        });
    }

    static void afterSerialization(UI ui) {
        doWithWrapper(ui, wrapper -> {
            wrapper.state = null;
            wrapper.getElement().appendChild(wrapper.component.getElement());
            flush(wrapper);
        });
    }

    static void afterDeserialization(UI ui) {
        doWithWrapper(ui, wrapper -> {
            wrapper.restoreComponent();
            // flush(wrapper);
        });
    }

    private void restoreComponent() {
        component = generator.apply(state);
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
