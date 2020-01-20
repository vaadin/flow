/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import com.vaadin.flow.internal.LocaleUtil;
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
        Optional<Locale> exactLocaleMatch = LocaleUtil.getExactLocaleMatch(request,
                Arrays.asList(Locale.ENGLISH, LOCALE_EN));

        Assert.assertEquals(
                "Found wrong locale event though an exact match should have been available.",
                LOCALE_EN, exactLocaleMatch.get());
    }

    @Test
    public void no_exact_match_returns_null() {
        Optional<Locale> exactLocaleMatch = LocaleUtil.getExactLocaleMatch(request,
                Arrays.asList(Locale.ENGLISH));

        Assert.assertFalse(
                "Found locale event though none should have been available.", exactLocaleMatch.isPresent());
    }

    @Test
    public void language_match_gets_correct_target_by_request_priority() {
        Optional<Locale> exactLocaleMatch = LocaleUtil.getLocaleMatchByLanguage(request,
                Arrays.asList(Locale.US, LOCALE_FI));


        Assert.assertEquals(
                "Found wrong locale event though an language match should have been available.",
                LOCALE_FI, exactLocaleMatch.get());
    }

    @Test
    public void language_match_returns_null_when_no_match() {
        Optional<Locale> exactLocaleMatch = LocaleUtil.getLocaleMatchByLanguage(request,
                Arrays.asList(Locale.FRENCH, Locale.KOREA));

        Assert.assertFalse(
                "Found locale event though none should have been available.", exactLocaleMatch.isPresent());
    }

}
