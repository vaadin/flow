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
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.internal.UrlUtil;
import com.vaadin.flow.server.InitParameters;
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
 * <p>
 * The browser submits only native form controls, such as
 * <code>&lt;input&gt;</code>, and form-associated custom elements with a form,
 * so use {@link Input} for the fields and {@link NativeButton} for the submit
 * button. Vaadin field components are not form-associated custom elements:
 * depending on the component they submit nothing, a formatted or internal value
 * instead of the field value, or only work by chance because of their internal
 * markup, and a Vaadin {@code Button} does not submit the form. None of this is
 * reported as an error.
 * <p>
 * Submitting the form makes the browser leave the current page, unless
 * {@link #setTarget(AnchorTargetValue) a target} sends the response to another
 * window or frame. The state of the Vaadin UI on the page is then lost. Keep in
 * mind what triggers a submit:
 * <ul>
 * <li>A {@link NativeButton} submits the form unless its type is set to
 * {@link NativeButton.Type#BUTTON}, because {@code submit} is the HTML default.
 * Set that type on every button in the form that should only run its click
 * listeners.
 * <li>Pressing Enter in an {@link Input} submits the form, even when the form
 * has no submit button but only a single text field.
 * <li>A click listener on a submit button races with the navigation: the
 * browser may leave the page before the server has handled the click, so do not
 * rely on it.
 * </ul>
 * <p>
 * The form is submitted with {@link Method#POST} unless another method is set,
 * so that field values such as passwords do not end up in the URL, the browser
 * history and server logs. A POST to an endpoint protected by Spring Security
 * needs a CSRF token as a hidden {@link Input} named after the token parameter.
 * The login processing URL of a view configured with
 * {@code VaadinSecurityConfigurer#loginView} is exempt from this.
 *
 * <pre>
 * Input username = new Input();
 * username.setName("username");
 * Input password = new Input();
 * password.setType("password");
 * password.setName("password");
 * // Keep the password out of the Vaadin session, see below
 * password.setValueChangeMode(null);
 * NativeButton submit = new NativeButton("Log in");
 * submit.setType(NativeButton.Type.SUBMIT);
 *
 * NativeForm form = new NativeForm(username, password, submit);
 * form.setAction("login");
 * </pre>
 * <p>
 * An {@link Input} sends its value to the server on every change by default, so
 * the value is also kept in the component state of the Vaadin session, which
 * may be serialized. Call {@code setValueChangeMode(null)} on an input for a
 * password or another secret that only the action URL should receive.
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
    private static final String ATTRIBUTE_TARGET = "target";

    /**
     * The HTTP method the browser uses to submit the form.
     */
    public enum Method {
        /**
         * Appends the form data to the action URL as a query string. This is
         * what the browser does when the method attribute is removed. Do not
         * use it for sensitive values such as passwords, which then end up in
         * the URL.
         */
        GET("get"),
        /**
         * Sends the form data in the request body. This is the method of a new
         * {@code NativeForm}.
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

        /**
         * Resolves an attribute value to its constant. The comparison ignores
         * case, because {@code method} is an HTML enumerated attribute and its
         * keywords are matched ASCII case-insensitively, so an element that
         * arrived with {@code method="POST"} has to read back as {@link #POST}.
         */
        private static @Nullable Method fromValue(@Nullable String value) {
            return Stream.of(values())
                    .filter(method -> method.value.equalsIgnoreCase(value))
                    .findFirst().orElse(null);
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

        /**
         * Resolves an attribute value to its constant. The comparison ignores
         * case, because {@code enctype} is an HTML enumerated attribute and its
         * keywords are matched ASCII case-insensitively, so an element that
         * arrived with {@code enctype="MULTIPART/FORM-DATA"} has to read back
         * as {@link #MULTIPART}.
         */
        private static @Nullable Enctype fromValue(@Nullable String value) {
            return Stream.of(values())
                    .filter(enctype -> enctype.value.equalsIgnoreCase(value))
                    .findFirst().orElse(null);
        }
    }

    /**
     * Creates a new empty form that is submitted with {@link Method#POST}.
     */
    public NativeForm() {
        super();
        setMethod(Method.POST);
    }

    /**
     * Creates a new form with the given children that is submitted with
     * {@link Method#POST}.
     *
     * @param components
     *            the children components
     * @throws IllegalArgumentException
     *             if one of the components is a {@code NativeForm}
     */
    public NativeForm(Component... components) {
        super(components);
        setMethod(Method.POST);
    }

    /**
     * Sets the URL the browser submits the form to. A relative URL is resolved
     * against the {@code <base href>} that Vaadin sets to the root of the
     * application, so {@code "login"} targets the {@code login} path of the
     * application, while {@code "/login"} ignores its context path.
     *
     * @param action
     *            the URL to submit to, or {@code null} to remove the attribute,
     *            in which case the browser submits to the URL of the current
     *            page
     * @throws IllegalArgumentException
     *             if the URL uses a scheme that is not considered safe, such as
     *             {@code javascript:}. The
     *             {@value InitParameters#URL_SAFE_SCHEMES} configuration is
     *             read from the application that this form is attached to, so
     *             for a form that isn't attached yet the exception is instead
     *             thrown when it is attached. See
     *             {@link #setUnsafeAction(String)} for setting a URL that
     *             shouldn't be checked at all.
     */
    public void setAction(@Nullable String action) {
        if (action == null) {
            UrlUtil.cancelUrlValidation(this, ATTRIBUTE_ACTION);
        } else {
            UrlUtil.validateUrl(this, ATTRIBUTE_ACTION, action,
                    "setUnsafeAction(String)",
                    () -> getElement().removeAttribute(ATTRIBUTE_ACTION));
        }
        getElement().setAttribute(ATTRIBUTE_ACTION, action);
    }

    /**
     * Sets the URL the browser submits the form to without validating its
     * scheme.
     * <p>
     * Unlike {@link #setAction(String)}, this method does not reject URLs based
     * on the {@value InitParameters#URL_SAFE_SCHEMES} configuration. Use it
     * only for URLs that are fully under your control and known to be safe.
     * Passing untrusted input here can expose the application to cross-site
     * scripting (XSS) attacks, because a {@code javascript:} action runs when
     * the form is submitted.
     *
     * @param action
     *            the URL to submit to, or {@code null} to remove the attribute
     * @see #setAction(String)
     */
    public void setUnsafeAction(@Nullable String action) {
        UrlUtil.cancelUrlValidation(this, ATTRIBUTE_ACTION);
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
     * Sets the HTTP method the browser uses to submit the form. A new form uses
     * {@link Method#POST}.
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
        return Method.fromValue(getElement().getAttribute(ATTRIBUTE_METHOD));
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
        return Enctype.fromValue(getElement().getAttribute(ATTRIBUTE_ENCTYPE));
    }

    /**
     * Sets the window, tab or frame that shows the response when the form is
     * submitted. Use {@link AnchorTarget#BLANK} to keep the Vaadin UI open
     * while the response opens in a new tab, or
     * {@link AnchorTargetValue#forString(String)} with the name of an
     * {@link IFrame} to show it there.
     *
     * @param target
     *            the target, or {@code null} or {@link AnchorTarget#DEFAULT} to
     *            remove the attribute, in which case the response replaces the
     *            current page
     */
    public void setTarget(@Nullable AnchorTargetValue target) {
        String value = target == null ? null : target.getValue();
        getElement().setAttribute(ATTRIBUTE_TARGET,
                value == null || value.isEmpty() ? null : value);
    }

    /**
     * Gets the window, tab or frame that shows the response when the form is
     * submitted.
     *
     * @return the target, or {@code null} if not set
     * @see #setTarget(AnchorTargetValue)
     */
    public @Nullable AnchorTargetValue getTarget() {
        String value = getElement().getAttribute(ATTRIBUTE_TARGET);
        return value == null ? null : AnchorTargetValue.forString(value);
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
     * <p>
     * A {@code NativeForm} created by the factory is rejected with an
     * {@link IllegalArgumentException}. While this form is attached, the
     * factory runs in an effect, so the exception is passed to the session
     * error handler instead of being thrown to the caller.
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
