/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Vector;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.server.VaadinRequest;

/**
 * Test class for the locale util methods.
 */
public class LocaleUtilTest {

    public static final Locale LOCALE_FI = new Locale("fi", "FI");
    public static final Locale LOCALE_EN = new Locale("en", "GB");

    @Mock
    VaadinRequest request;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        // request locales are returned as Enumeration
        Mockito.when(request.getLocales()).thenReturn(
                new Vector<>(Arrays.asList(new Locale("fi"), LOCALE_EN))
                        .elements());
    }

    @Test
    public void exact_match_provided_matches() {
        Optional<Locale> exactLocaleMatch = LocaleUtil.getExactLocaleMatch(
                request, Arrays.asList(Locale.ENGLISH, LOCALE_EN));

        Assert.assertEquals(
                "Found wrong locale event though an exact match should have been available.",
                LOCALE_EN, exactLocaleMatch.get());
    }

    @Test
    public void no_exact_match_returns_null() {
        Optional<Locale> exactLocaleMatch = LocaleUtil
                .getExactLocaleMatch(request, Arrays.asList(Locale.ENGLISH));

        Assert.assertFalse(
                "Found locale event though none should have been available.",
                exactLocaleMatch.isPresent());
    }

    @Test
    public void language_match_gets_correct_target_by_request_priority() {
        Optional<Locale> exactLocaleMatch = LocaleUtil.getLocaleMatchByLanguage(
                request, Arrays.asList(Locale.US, LOCALE_FI));

        Assert.assertEquals(
                "Found wrong locale event though an language match should have been available.",
                LOCALE_FI, exactLocaleMatch.get());
    }

    @Test
    public void language_match_returns_null_when_no_match() {
        Optional<Locale> exactLocaleMatch = LocaleUtil.getLocaleMatchByLanguage(
                request, Arrays.asList(Locale.FRENCH, Locale.KOREA));

        Assert.assertFalse(
                "Found locale event though none should have been available.",
                exactLocaleMatch.isPresent());
    }

}
