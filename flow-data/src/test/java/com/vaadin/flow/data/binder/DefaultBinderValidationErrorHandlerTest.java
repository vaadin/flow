/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.HasTheme;
import com.vaadin.flow.data.binder.testcomponents.TestTextField;
import com.vaadin.flow.dom.ThemeList;

public class DefaultBinderValidationErrorHandlerTest {

    private DefaultBinderValidationErrorHandler handler = new DefaultBinderValidationErrorHandler();

    private TestTextField field = new TestTextField();

    private ThemeList themes = Mockito.mock(ThemeList.class);

    private class TestHasTheme extends TestTextField implements HasTheme {

        @Override
        public ThemeList getThemeNames() {
            return themes;
        }
    }

    @Test
    public void handleError_setValidationStatus_setErrorTheme() {
        handler.handleError(field, ValidationResult.error(""));

        Assert.assertTrue(field.isInvalid());

        Assert.assertTrue(field.getElement().getThemeList()
                .contains(ErrorLevel.ERROR.name().toLowerCase(Locale.ENGLISH)));
    }

    @Test
    public void handleError_fieldHasTheme_setErrorTheme() {
        TestHasTheme field = new TestHasTheme();
        handler.handleError(field, ValidationResult.error(""));

        Mockito.verify(themes)
                .add(ErrorLevel.ERROR.name().toLowerCase(Locale.ENGLISH));
    }

    @Test
    public void clearError_setValidationStatus_clearErrorTheme() {
        field.setInvalid(true);
        field.getElement().getThemeList()
                .add(ErrorLevel.CRITICAL.name().toLowerCase(Locale.ENGLISH));
        handler.clearError(field);

        Assert.assertFalse(field.isInvalid());

        Assert.assertFalse(field.getElement().getThemeList().contains(
                ErrorLevel.CRITICAL.name().toLowerCase(Locale.ENGLISH)));
    }

    @Test
    public void clearError_fieldHasTheme_clearErrorTheme() {
        TestHasTheme field = new TestHasTheme();
        handler.clearError(field);

        Mockito.verify(themes)
                .remove(ErrorLevel.ERROR.name().toLowerCase(Locale.ENGLISH));
    }
}
