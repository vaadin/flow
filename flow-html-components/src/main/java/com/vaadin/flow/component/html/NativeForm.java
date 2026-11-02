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

import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.signals.Signal;

/**
 * Component representing a <code>&lt;form&gt;</code> element.
 * <p>
 * A native form is submitted by the browser itself: when the user activates a
 * submit button inside it, the browser sends the values of the named fields
 * ({@link Input#setName(String)}) to {@link #setAction(String) the action URL}
 * with {@link #setMethod(Method) the given method} and navigates to the
 * response. The values do not go through the server-side component state, so
 * use this for posting to an endpoint outside of Vaadin, such as the login
 * processing URL of Spring Security, rather than for editing data with a
 * {@code Binder}.
 *
 * <pre>
 * Input username = new Input();
 * username.setName("username");
 * Input password = new Input();
 * password.setType("password");
 * password.setName("password");
 * NativeButton submit = new NativeButton("Log in");
 * submit.setType(NativeButton.Type.SUBMIT);
 *
 * NativeForm form = new NativeForm(username, password, submit);
 * form.setAction("login");
 * form.setMethod(NativeForm.Method.POST);
 * </pre>
 * <p>
 * Forms can not be nested in HTML; the browser drops the inner
 * <code>&lt;form&gt;</code> tag. Adding a {@code NativeForm} directly to
 * another {@code NativeForm} therefore throws an
 * {@link IllegalArgumentException}.
 *
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/form">MDN:
 *      &lt;form&gt;</a>
 * @author Vaadin Ltd
 */
@NullMarked
@Tag("form")
public class NativeForm extends HtmlContainer {

    private static final String ATTRIBUTE_ACTION = "action";
    private static final String ATTRIBUTE_METHOD = "method";
    private static final String ATTRIBUTE_ENCTYPE = "enctype";

    /**
     * The HTTP method the browser uses to submit the form.
     */
    public enum Method {
        /**
         * Appends the form data to the action URL as a query string. This is
         * what the browser does when no method is set.
         */
        GET("get"),
        /**
         * Sends the form data in the request body.
         */
        POST("post"),
        /**
         * Closes the <code>&lt;dialog&gt;</code> element the form is in instead
         * of sending a request.
         */
        DIALOG("dialog");

        private final String value;

        Method(String value) {
            this.value = value;
        }

        /**
         * Returns the attribute value as it appears in the rendered HTML.
         *
         * @return the {@code method} attribute value
         */
        public String getValue() {
            return value;
        }
    }

    /**
     * The encoding of the form data when the form is submitted with
     * {@link Method#POST}.
     */
    public enum Enctype {
        /**
         * Encodes the fields as {@code application/x-www-form-urlencoded}. This
         * is what the browser does when no encoding is set.
         */
        URLENCODED("application/x-www-form-urlencoded"),
        /**
         * Encodes the fields as {@code multipart/form-data}, which is needed to
         * submit the content of an {@code <input type="file">}.
         */
        MULTIPART("multipart/form-data"),
        /**
         * Encodes the fields as {@code text/plain}.
         */
        PLAIN("text/plain");

        private final String value;

        Enctype(String value) {
            this.value = value;
        }

        /**
         * Returns the attribute value as it appears in the rendered HTML.
         *
         * @return the {@code enctype} attribute value
         */
        public String getValue() {
            return value;
        }
    }

    /**
     * Creates a new empty form.
     */
    public NativeForm() {
        super();
    }

    /**
     * Creates a new form with the given children.
     *
     * @param components
     *            the children components
     * @throws IllegalArgumentException
     *             if one of the components is a {@code NativeForm}
     */
    public NativeForm(Component... components) {
        super(components);
    }

    /**
     * Sets the URL the browser submits the form to. A relative URL is resolved
     * against the base URL of the page, so {@code "login"} targets the
     * {@code login} path of the application.
     *
     * @param action
     *            the URL to submit to, or {@code null} to remove the attribute,
     *            in which case the browser submits to the URL of the current
     *            page
     */
    public void setAction(@Nullable String action) {
        getElement().setAttribute(ATTRIBUTE_ACTION, action);
    }

    /**
     * Gets the URL the browser submits the form to.
     *
     * @return the action URL, or {@code null} if not set
     * @see #setAction(String)
     */
    public @Nullable String getAction() {
        return getElement().getAttribute(ATTRIBUTE_ACTION);
    }

    /**
     * Sets the HTTP method the browser uses to submit the form.
     *
     * @param method
     *            the method, or {@code null} to remove the attribute, in which
     *            case the browser uses {@link Method#GET}
     */
    public void setMethod(@Nullable Method method) {
        getElement().setAttribute(ATTRIBUTE_METHOD,
                method == null ? null : method.getValue());
    }

    /**
     * Gets the HTTP method the browser uses to submit the form.
     *
     * @return the method, or {@code null} if not set or not recognized
     * @see #setMethod(Method)
     */
    public @Nullable Method getMethod() {
        String value = getElement().getAttribute(ATTRIBUTE_METHOD);
        for (Method method : Method.values()) {
            // HTML keywords are case-insensitive
            if (method.getValue().equalsIgnoreCase(value)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Sets how the browser encodes the form data when submitting it with
     * {@link Method#POST}.
     *
     * @param enctype
     *            the encoding, or {@code null} to remove the attribute, in
     *            which case the browser uses {@link Enctype#URLENCODED}
     */
    public void setEnctype(@Nullable Enctype enctype) {
        getElement().setAttribute(ATTRIBUTE_ENCTYPE,
                enctype == null ? null : enctype.getValue());
    }

    /**
     * Gets how the browser encodes the form data when submitting it.
     *
     * @return the encoding, or {@code null} if not set or not recognized
     * @see #setEnctype(Enctype)
     */
    public @Nullable Enctype getEnctype() {
        String value = getElement().getAttribute(ATTRIBUTE_ENCTYPE);
        for (Enctype enctype : Enctype.values()) {
            if (enctype.getValue().equalsIgnoreCase(value)) {
                return enctype;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException
     *             if one of the components is a {@code NativeForm}
     */
    @Override
    public void add(Collection<Component> components) {
        components.forEach(NativeForm::rejectNestedForm);
        super.add(components);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException
     *             if the component is a {@code NativeForm}
     */
    @Override
    public void addComponentAtIndex(int index, Component component) {
        rejectNestedForm(component);
        super.addComponentAtIndex(index, component);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException
     *             if the new component is a {@code NativeForm}
     */
    @Override
    public void replace(@Nullable Component oldComponent,
            @Nullable Component newComponent) {
        rejectNestedForm(newComponent);
        super.replace(oldComponent, newComponent);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException
     *             if the factory creates a {@code NativeForm}
     */
    @Override
    public <V extends @Nullable Object, S extends Signal<V>> void bindChildren(
            Signal<List<S>> list,
            SerializableFunction<S, Component> childFactory) {
        super.bindChildren(list, item -> {
            Component child = childFactory.apply(item);
            rejectNestedForm(child);
            return child;
        });
    }

    private static void rejectNestedForm(@Nullable Component component) {
        if (component instanceof NativeForm) {
            throw new IllegalArgumentException(
                    "A NativeForm can not be added to another NativeForm, "
                            + "because nested <form> elements are not "
                            + "allowed in HTML.");
        }
    }
}
