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
package com.vaadin.flow.data.validator;

/**
 * A string validator for e-mail addresses. The e-mail address syntax is not
 * complete according to RFC 822 but handles the vast majority of valid e-mail
 * addresses correctly.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
@SuppressWarnings("serial")
public class EmailValidator extends RegexpValidator {

    public static final String PATTERN = "^" + "([a-zA-Z0-9_\\.\\-+])+" // local
            + "@" + "([a-zA-Z0-9-]+\\.)+" // domain with the dot separator
            + "[a-zA-Z0-9-]{2,}" // before tld
            + "$";

    private final boolean allowEmptyValue;

    /**
     * Creates a validator for checking that a string is a syntactically valid
     * e-mail address.
     * <p>
     * This constructor creates a validator which doesn't accept an empty string
     * as a valid e-mail address. Use {@link #EmailValidator(String, boolean)}
     * constructor with {@code true} as a value for the second argument to
     * create a validator which accepts an empty string.
     *
     * @param errorMessage
     *            the message to display in case the value does not validate.
     * @see #EmailValidator(String, boolean)
     */
    public EmailValidator(String errorMessage) {
        this(errorMessage, false);
    }

    /**
     * Creates a validator for checking that a string is a syntactically valid
     * e-mail address.
     *
     * @param errorMessage
     *            the message to display in case the value does not validate.
     * @param allowEmpty
     *            if {@code true} then an empty string passes the validation,
     *            otherwise the validation fails
     */
    public EmailValidator(String errorMessage, boolean allowEmpty) {
        super(errorMessage, PATTERN, true);
        allowEmptyValue = allowEmpty;
    }

    @Override
    protected boolean isValid(String value) {
        if (allowEmptyValue && value != null && value.isEmpty()) {
            return true;
        }
        return super.isValid(value);
    }
}
