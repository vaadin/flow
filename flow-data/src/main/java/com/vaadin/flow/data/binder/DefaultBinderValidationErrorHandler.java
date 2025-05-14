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
package com.vaadin.flow.data.binder;

import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasTheme;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.dom.ThemeList;

/**
 * Default implementation of {@link BinderValidationErrorHandler}.
 * <p>
 * This handler applies visual effects for the field if its type allows this:
 * <ul>
 * <li>If the field's class implements {@link HasValidation} interface then its
 * validity and error message is set based on a {@link ValidationResult}
 * <li>If the field's class may have a theme (e.g. implements {@link HasTheme}
 * or {@link HasElement}) then theme name derived from the {@link ErrorLevel} of
 * the {@link ValidationResult} instance is applied. E.g., for
 * {@link ErrorLevel#WARNING} the element will get the "theme"="warning"
 * attribute and value in HTML.
 * </ul>
 *
 *
 * @author Vaadin Ltd
 * @since
 *
 */
public class DefaultBinderValidationErrorHandler
        implements BinderValidationErrorHandler {

    /**
     * Handles a validation error emitted when trying to write the value of the
     * given field.
     *
     * @param field
     *            the field with the invalid value
     * @param result
     *            the validation error result
     */
    @Override
    public void handleError(HasValue<?, ?> field, ValidationResult result) {
        if (field instanceof HasValidation) {
            HasValidation fieldWithValidation = (HasValidation) field;
            fieldWithValidation.setInvalid(true);
            fieldWithValidation.setErrorMessage(result.getErrorMessage());
        }
        setErrorTheme(field, result);
    }

    /**
     * Clears the error condition of the given field, if any.
     *
     * @param field
     *            the field with an invalid value
     */
    @Override
    public void clearError(HasValue<?, ?> field) {
        if (field instanceof HasValidation) {
            HasValidation fieldWithValidation = (HasValidation) field;
            fieldWithValidation.setInvalid(false);
        }
        clearErrorTheme(field);
    }

    /**
     * Gets the theme name for the {@code ErrorLevel}.
     *
     * @param errorLevel
     *            the error level
     * @return a theme name for the error level
     */
    protected String getErrorThemeName(ErrorLevel errorLevel) {
        return errorLevel.name().toLowerCase();
    }

    /**
     * Gets themes for the {@code field}.
     *
     * @param field
     *            a field
     * @return an optional theme list, or an empty optional if the {@code field}
     *         doesn't have it
     */
    protected Optional<ThemeList> getThemes(HasValue<?, ?> field) {
        if (field instanceof HasTheme) {
            return Optional.of(((HasTheme) field).getThemeNames());
        } else if (field instanceof HasElement) {
            return Optional
                    .of(((HasElement) field).getElement().getThemeList());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Clears error theme for the {@code field}.
     *
     * @param field
     *            a feild
     */
    protected void clearErrorTheme(HasValue<?, ?> field) {
        getThemes(field).ifPresent(themes -> Stream.of(ErrorLevel.values())
                .map(this::getErrorThemeName).forEach(themes::remove));
    }

    /**
     * Sets error theme for the {@code field} based on {@code result}.
     *
     * @param field
     *            a field
     * @param result
     *            a validation result
     */
    protected void setErrorTheme(HasValue<?, ?> field,
            ValidationResult result) {
        result.getErrorLevel().map(this::getErrorThemeName)
                .ifPresent(errorTheme -> getThemes(field)
                        .ifPresent(themes -> themes.add(errorTheme)));
    }

}
