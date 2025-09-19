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

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.JsonNode;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Internationalization object for customizing the component UI texts. An
 * instance with the default messages can be obtained using
 * {@link LoginI18n#createDefault()}
 *
 * @see LoginForm#setI18n(LoginI18n)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginI18n implements Serializable {

    private static final JsonNode DEFAULT_I18N;

    static {
        try {
            String jsonString = IOUtils.toString(
                    LoginI18n.class.getResource("i18n.json"),
                    StandardCharsets.UTF_8);
            DEFAULT_I18N = JacksonUtils.readTree(jsonString);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot find the default i18n configuration. "
                            + "Please make sure the i18n.json does exist.");
        }
    }

    private Header header;
    private Form form;
    private ErrorMessage errorMessage;
    private String additionalInformation;

    /**
     * @return a new instance with the default messages
     */
    public static LoginI18n createDefault() {
        return JacksonUtils.readToObject(DEFAULT_I18N, LoginI18n.class);
    }

    /**
     * @return {@link Header} object.
     */
    public Header getHeader() {
        return header;
    }

    /**
     * @param header
     *            {@link Header} object.
     */
    public void setHeader(Header header) {
        this.header = header;
    }

    /**
     * @return {@link Form} object.
     */
    public Form getForm() {
        return form;
    }

    /**
     * @param form
     *            {@link Form} object.
     */
    public void setForm(Form form) {
        this.form = form;
    }

    /**
     * @return {@link ErrorMessage} object.
     */
    public ErrorMessage getErrorMessage() {
        return errorMessage;
    }

    /**
     * @param errorMessage
     *            {@link ErrorMessage} object.
     */
    public void setErrorMessage(ErrorMessage errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * @return current value for the additional information property
     */
    public String getAdditionalInformation() {
        return additionalInformation;
    }

    /**
     * @param additionalInformation
     *            new value for the additional information property.
     */
    public void setAdditionalInformation(String additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    /**
     * I18n properties for the header.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Header implements Serializable {
        private String title;
        private String description;

        /**
         * @return current value for the title property
         */
        public String getTitle() {
            return title;
        }

        /**
         * @param title
         *            new value for the title property.
         */
        public void setTitle(String title) {
            this.title = title;
        }

        /**
         * @return current value for the description property
         */
        public String getDescription() {
            return description;
        }

        /**
         * @param description
         *            new value for the description property.
         */
        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * I18n properties for the form.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Form implements Serializable {
        private String title;
        private String username;
        private String password;
        private String submit;
        private String forgotPassword;

        /**
         * @return current value for the title property
         */
        public String getTitle() {
            return title;
        }

        /**
         * @param title
         *            new value for the title property.
         */
        public void setTitle(String title) {
            this.title = title;
        }

        /**
         * @return current value for the username property
         */
        public String getUsername() {
            return username;
        }

        /**
         * @param username
         *            new value for the username property.
         */
        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * @return current value for the password property
         */
        public String getPassword() {
            return password;
        }

        /**
         * @param password
         *            new value for the password property.
         */
        public void setPassword(String password) {
            this.password = password;
        }

        /**
         * @return current value for the submit property
         */
        public String getSubmit() {
            return submit;
        }

        /**
         * @param submit
         *            new value for the submit property.
         */
        public void setSubmit(String submit) {
            this.submit = submit;
        }

        /**
         * @return current value for the forgotPassword property
         */
        public String getForgotPassword() {
            return forgotPassword;
        }

        /**
         * @param forgotPassword
         *            new value for the forgotPassword property.
         */
        public void setForgotPassword(String forgotPassword) {
            this.forgotPassword = forgotPassword;
        }
    }

    /**
     * I18n properties for the error message.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorMessage implements Serializable {
        private String title;
        private String message;

        private String username;
        private String password;

        /**
         * @return current value for the title property
         */
        public String getTitle() {
            return title;
        }

        /**
         * @param title
         *            new value for the title property.
         */
        public void setTitle(String title) {
            this.title = title;
        }

        /**
         * @return current value for the message property
         */
        public String getMessage() {
            return message;
        }

        /**
         * @param message
         *            new value for the message property.
         */
        public void setMessage(String message) {
            this.message = message;
        }

        /**
         * @return current value for the username property
         */
        public String getUsername() {
            return username;
        }

        /**
         * Sets the error message for the username field
         *
         * @param username
         *            new value for the username property
         */
        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * @return current value for the password property
         */
        public String getPassword() {
            return password;
        }

        /**
         * Sets the error message for the password field
         *
         * @param password
         *            new value for the password property
         */
        public void setPassword(String password) {
            this.password = password;
        }
    }

}
