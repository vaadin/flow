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
package com.vaadin.flow.component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.stream.Stream;

import com.googlecode.gentyref.GenericTypeReflector;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementUtil;
import com.vaadin.flow.internal.ReflectTools;

/**
 * A composite encapsulates a {@link Component} tree to allow creation of new
 * components by composing existing components. By encapsulating the component,
 * its API can be hidden or presented in a different way for the user of the
 * composite.
 * <p>
 * The encapsulated component tree is available through {@link #getContent()}.
 * Composite will by default look at the generic type declaration of its
 * subclass to find the content type and create an instance if the type has a
 * no-args constructor. You can also override {@link #initContent()} to manually
 * create the component tree. The encapsulated component itself can contain more
 * components.
 * <p>
 * Composite is a way to hide API on the server side. It does not contribute any
 * element to the {@link Element} tree.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @param <T>
 *            the type of the content
 */
public abstract class Composite<T extends Component> extends Component {
    private T content;

    /**
     * Creates a new composite.
     * <p>
     * To define your own composite, extend this class and implement
     * {@link #initContent()}.
     */
    protected Composite() {
        super(null);
    }

    /**
     * Called when the content of this composite is requested for the first
     * time.
     * <p>
     * This method should initialize the component structure for the composite
     * and return the root component.
     * <p>
     * By default, this method uses reflection to instantiate the component
     * based on the generic type of the sub class.
     *
     * @return the root component which this composite wraps, never {@code null}
     */
    @SuppressWarnings("unchecked")
    protected T initContent() {
        return (T) ReflectTools.createInstance(
                findContentType((Class<? extends Composite<?>>) getClass()));
    }

    private static Class<? extends Component> findContentType(
            Class<? extends Composite<?>> compositeClass) {
        Type type = GenericTypeReflector.getTypeParameter(
                compositeClass.getGenericSuperclass(),
                Composite.class.getTypeParameters()[0]);
        if (type instanceof Class || type instanceof ParameterizedType) {
            return GenericTypeReflector.erase(type).asSubclass(Component.class);
        }
        throw new IllegalStateException(getExceptionMessage(type));
    }

    private static String getExceptionMessage(Type type) {
        if (type == null) {
            return "Composite is used as raw type: either add type information or override initContent().";
        }

        if (type instanceof TypeVariable) {
            return String.format(
                    "Could not determine the composite content type for TypeVariable '%s'. "
                            + "Either specify exact type or override initContent().",
                    type.getTypeName());
        }
        return String.format(
                "Could not determine the composite content type for %s. Override initContent().",
                type.getTypeName());
    }

    /**
     * Gets the content of the composite, i.e. the component the composite is
     * wrapping.
     *
     * @return the content for the composite, never {@code null}
     */
    public T getContent() {
        if (content == null) {
            T newContent = initContent();
            if (newContent == null) {
                throw new IllegalStateException(
                        "initContent returned null instead of a component");
            }
            setContent(newContent);
        }
        return content;
    }

    /**
     * Sets the content for this composite and attaches it to the element.
     * <p>
     * This method must only be called once.
     *
     * @param content
     *            the content for the composite
     */
    private void setContent(T content) {
        assert content.getElement().getComponent()
                .isPresent() : "Composite should never be attached to an element which is not attached to a component";
        assert this.content == null : "Content has already been initialized";
        this.content = content;
        Element element = content.getElement();
        // Always replace the composite reference as this will be called from
        // inside out, so the end result is that the element refers to the
        // outermost composite in the probably rare case that multiple
        // composites are nested
        ElementUtil.setComponent(element, this);
    }

    /**
     * Gets the root element of this composite.
     * <p>
     * For a composite, the root element is the same as the root element of the
     * content of the composite.
     *
     * @return the root element of this component
     */
    @Override
    public Element getElement() {
        return getContent().getElement();
    }

    /**
     * Gets the child components of this composite.
     * <p>
     * A composite always has one child component, returned by
     * {@link #initContent()}.
     *
     * @return the child component of this composite
     */
    @Override
    public Stream<Component> getChildren() {
        return Stream.of(getContent());
    }
}
