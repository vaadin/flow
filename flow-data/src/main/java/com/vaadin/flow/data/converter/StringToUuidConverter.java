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

package com.vaadin.flow.data.converter;

import com.vaadin.flow.data.binder.ErrorMessageProvider;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * A converter that converts from {@link String} to {@link UUID} and back.
 * <p>
 * Leading and trailing white spaces are ignored when converting from a String.
 * </p>
 * <p>
 * The String representation uses the canonical format of 32-characters with a hyphen
 * to separate each of five groups of hexadecimal digits as defined in:
 * RFC 4122: A Universally Unique IDentifier (UUID) URN Namespace
 * http://www.ietf.org/rfc/rfc4122.txt
 * </p>
 *
 * @author Vaadin Ltd
 */
public class StringToUuidConverter implements Converter<String, UUID> {

    private ErrorMessageProvider errorMessageProvider;

    /**
     * Constructs a converter for String to UUID and back.
     *
     * @param errorMessage the error message to use if conversion fails
     */
    public StringToUuidConverter(String errorMessage) {
        this(ctx -> errorMessage);
    }

    /**
     * Constructs a new converter instance with the given error message provider.
     * Empty strings are converted to <code>null</code>.
     *
     * @param errorMessageProvider the error message provider to use if conversion fails
     */
    public StringToUuidConverter(ErrorMessageProvider errorMessageProvider) {
        this.errorMessageProvider = errorMessageProvider;
    }

    @Override
    public Result<UUID> convertToModel(String value, ValueContext context) {
        if (value == null) {
            return Result.ok(null);
        }

        // Remove leading and trailing white space
        value = value.trim();

        // Parse string as UUID.
        UUID uuid = null;
        try {
            uuid = UUID.fromString(value);
        } catch (java.lang.IllegalArgumentException e) {
            LoggerFactory.getLogger(StringToUuidConverter.class.getName()).warn(
                    "Unable to convert String to UUID: " + value, e);
            return Result.error(this.errorMessageProvider.apply(context));
        }
        return Result.ok(uuid); // Return the UUID object, converted from String.
    }

    @Override
    public String convertToPresentation(UUID value, ValueContext context) {
        if (value == null) {
            return null;
        }
        // `java.util.UUID::toString` generates a textual representation of a
        // UUID’s 128-bits as in a  hexadecimal `String` in 32-character canonical
        // format with four hyphens separating groups of digits.
        // https://docs.oracle.com/javase/10/docs/api/java/util/UUID.html#toString()
        return value.toString();
    }

}
