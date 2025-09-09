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
package com.vaadin.flow.component.login;

import org.slf4j.LoggerFactory;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.BaseJsonNode;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.PropertyChangeListener;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.shared.Registration;

/**
 * Abstract component for the {@code <vaadin-login-overlay>} and
 * {@code <vaadin-login-form>} components. On {@link LoginForm.LoginEvent}
 * component becomes disabled. Disabled component stops to process login events,
 * however the {@link LoginForm.ForgotPasswordEvent} event is processed anyway.
 * To enable use the
 * {@link com.vaadin.flow.component.HasEnabled#setEnabled(boolean)} method.
 * Setting error {@link #setError(boolean)} true makes component automatically
 * enabled for the next login attempt.
 * <p>
 * </p>
 * Server side login listener do not work in combination with HTML form
 * submission configured by setting the {@code action} attribute. The reason is
 * that form submission, depending on the authentication process result, will
 * cause a redirection to a different page or to current login view. In both
 * cases a new Flow UI will be created and the event will potentially be
 * forwarded to a dismissed UI. In addition, if the HTTP session ID is changed
 * as a consequence of the authentication process, the server may respond to the
 * login event with a session expiration error, cause a client resynchronization
 * that can in turn cancel a potential redirect issued by the form submission.
 *
 * @author Vaadin Ltd
 */
public abstract class AbstractLogin extends Component implements HasEnabled {

    private static final String LOGIN_EVENT = "com/vaadin/flow/component/login";

    private static final String PROP_ACTION = "action";
    private static final String PROP_DISABLED = "disabled";
    private static final String PROP_ERROR = "error";
    private static final String PROP_NO_FORGOT_PASSWORD = "noForgotPassword";

    private static final PropertyChangeListener NO_OP = event -> {
    };
    private Registration registration;

    /**
     * Initializes a new AbstractLogin with a default localization.
     */
    public AbstractLogin() {
        this(LoginI18n.createDefault());
        getElement().setProperty("_preventAutoEnable", true);
        registerDefaultLoginListener();
    }

    /**
     * Initializes a new AbstractLogin.
     *
     * @param i18n
     *            internationalized messages to be used by this instance.
     */
    public AbstractLogin(LoginI18n i18n) {
        setI18n(i18n);
    }

    private void registerDefaultLoginListener() {
        DomListenerRegistration disabledPropertyRegistration = getElement()
                .addPropertyChangeListener(PROP_DISABLED, LOGIN_EVENT, NO_OP);
        Registration loginListenerRegistration = addLoginListener(e -> {
            setEnabled(false);
            setError(false);
        });
        this.registration = Registration.combine(disabledPropertyRegistration,
                loginListenerRegistration);
    }

    /**
     * Sets the path where to send the form-data when a form is submitted. Once
     * action is defined a {@link AbstractLogin.LoginEvent} is not fired
     * anymore.
     * <p>
     * The {@code action} attribute should not be used together with login
     * listeners added with {@link #addLoginListener(ComponentEventListener)}.
     * See class Javadoc for more information.
     *
     * @see #getAction()
     * @see #addLoginListener(ComponentEventListener)
     */
    public void setAction(String action) {
        if (action == null) {
            getElement().removeProperty(PROP_ACTION);
            if (registration == null) {
                registerDefaultLoginListener();
            }
        } else {
            getElement().setProperty(PROP_ACTION, action);
            if (registration != null) {
                registration.remove();
                registration = null;
            }
            warnIfActionAndLoginListenerUsedTogether();
        }
    }

    /**
     * Returns the action defined for a login form.
     *
     * @return the value of action property
     */
    public String getAction() {
        return getElement().getProperty(PROP_ACTION, "");
    }

    /**
     * Sets whether to show or hide the error message. The message can be set
     * via {@link #setI18n(LoginI18n)}
     *
     * Calling this method with {@code true} will also enable the component.
     *
     * @param error
     *            {@code true} to show the error message and enable component
     *            for next login attempt, {@code false} to hide an error
     * @see #isError()
     */
    public void setError(boolean error) {
        if (error) {
            setEnabled(true);
        }
        getElement().setProperty(PROP_ERROR, error);
    }

    /**
     * Returns whether the error message is displayed or not
     *
     * @return the value of error property
     */
    @Synchronize(property = PROP_ERROR, value = "error-changed")
    public boolean isError() {
        return getElement().getProperty(PROP_ERROR, false);
    }

    /**
     * Sets whether to show or hide the forgot password button. The button is
     * visible by default
     *
     * @param forgotPasswordButtonVisible
     *            whether to display or hide the button
     * @see #isForgotPasswordButtonVisible()
     */
    public void setForgotPasswordButtonVisible(
            boolean forgotPasswordButtonVisible) {
        getElement().setProperty(PROP_NO_FORGOT_PASSWORD,
                !forgotPasswordButtonVisible);
    }

    /**
     * Returns whether the forgot password button is visible or not
     *
     * @return {@code true} if the forgot password button is visible
     *         {@code false} otherwise
     */
    public boolean isForgotPasswordButtonVisible() {
        return !getElement().getProperty(PROP_NO_FORGOT_PASSWORD, false);
    }

    /**
     * Sets the internationalized messages to be used by this instance.
     *
     * @param i18n
     *            the internationalized messages
     * @see LoginI18n#createDefault()
     */
    public void setI18n(LoginI18n i18n) {
        BaseJsonNode jsonNode = i18n != null ? JacksonUtils.beanToJson(i18n)
                : JacksonUtils.nullNode();
        getElement().setPropertyJson("i18n", jsonNode);
    }

    /**
     * Returns {@link LoginI18n} set earlier via {@link #setI18n(LoginI18n)}.
     * <p>
     * </p>
     * Note that a copy of the original object is returned: changes done to the
     * copy will not be reflected back until the object is set via
     * {@link #setI18n(LoginI18n)}.
     *
     * @return currently set {@link LoginI18n} or null if none was set.
     */
    LoginI18n getI18n() {
        final JsonNode json = (JsonNode) getElement().getPropertyRaw("i18n");
        if (json == null || json.isNull()) {
            return null;
        }
        return JacksonUtils.readToObject(json, LoginI18n.class);
    }

    /**
     * Shows given error message and sets {@link #setError(boolean)} to true.
     *
     * @param title
     *            the {@link LoginI18n.ErrorMessage#getTitle() error message
     *            title}, may be null.
     * @param message
     *            the {@link LoginI18n.ErrorMessage#getMessage() error message},
     *            may be null.
     */
    public void showErrorMessage(String title, String message) {
        var loginI18n = getI18n();
        if (loginI18n == null) {
            loginI18n = LoginI18n.createDefault();
        }
        if (loginI18n.getErrorMessage() == null) {
            loginI18n.setErrorMessage(new LoginI18n.ErrorMessage());
        }
        loginI18n.getErrorMessage().setTitle(title);
        loginI18n.getErrorMessage().setMessage(message);
        setI18n(loginI18n);
        setError(true);
    }

    /**
     * Adds `login` event listener.
     * <p>
     * Login listeners should not be used together with the {@code action}
     * attribute. See class Javadoc for more information.
     *
     * @see #setAction(String)
     */
    public Registration addLoginListener(
            ComponentEventListener<LoginEvent> listener) {
        Registration registration = ComponentUtil.addListener(this,
                LoginEvent.class, listener);
        warnIfActionAndLoginListenerUsedTogether();
        return registration;
    }

    /**
     * Adds `forgotPassword` event listener. Event continues being process even
     * if the component is not {@link #isEnabled()}.
     */
    public Registration addForgotPasswordListener(
            ComponentEventListener<ForgotPasswordEvent> listener) {
        return ComponentUtil.addListener(this, ForgotPasswordEvent.class,
                listener, domReg -> domReg
                        .setDisabledUpdateMode(DisabledUpdateMode.ALWAYS));
    }

    /**
     * `login` is fired when the user either clicks Submit button or presses an
     * Enter key. Event is fired only if client-side validation passed.
     */
    @DomEvent(LOGIN_EVENT)
    public static class LoginEvent extends ComponentEvent<AbstractLogin> {

        private String username;
        private String password;

        public LoginEvent(AbstractLogin source, boolean fromClient,
                @EventData("event.detail.username") String username,
                @EventData("event.detail.password") String password) {
            super(source, fromClient);
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }

    /**
     * `forgot-password` is fired when the user clicks Forgot password button
     */
    @DomEvent("forgot-password")
    public static class ForgotPasswordEvent
            extends ComponentEvent<AbstractLogin> {
        public ForgotPasswordEvent(AbstractLogin source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    @Override
    public void onEnabledStateChanged(boolean enabled) {
        getElement().setProperty(PROP_DISABLED, !enabled);
    }

    private void warnIfActionAndLoginListenerUsedTogether() {
        if (getElement().hasProperty(PROP_ACTION)
                && !getListeners(LoginEvent.class).isEmpty()) {
            LoggerFactory.getLogger(getClass()).warn(
                    "Using the action attribute together with login listeners is discouraged. See the AbstractLogin JavaDoc for more information. This may throw an exception in the future.");
        }
    }
}
