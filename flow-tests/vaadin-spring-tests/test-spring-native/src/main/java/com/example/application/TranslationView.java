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
package com.example.application;

import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Collectors;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.internal.LocaleUtil;
import com.vaadin.flow.router.Route;

/**
 * Shows the locales found in the vaadin-i18n folder and a translation read for
 * each of them, one line each.
 * <p>
 * The locales are found by listing the folder, while a translation is read from
 * a file of a known name, so the two can fail independently.
 */
@Route("translations")
public class TranslationView extends Div {

    public static final String LOCALES_ID = "available-locales";
    public static final String DEFAULT_ID = "default";
    public static final String GERMAN_ID = "german";
    public static final String FINNISH_ID = "finnish";

    public TranslationView() {
        I18NProvider provider = LocaleUtil.getI18NProvider().orElseThrow();
        Div locales = new Div("Available translation locales: "
                + provider.getProvidedLocales().stream().map(Locale::toString)
                        .sorted(Comparator.naturalOrder())
                        .collect(Collectors.joining(", ")));
        locales.setId(LOCALES_ID);

        add(locales, createTranslation(DEFAULT_ID, Locale.ENGLISH),
                createTranslation(GERMAN_ID, Locale.GERMAN),
                createTranslation(FINNISH_ID, Locale.of("fi", "FI")));
    }

    private Div createTranslation(String id, Locale locale) {
        Div translation = new Div(
                locale + ": " + getTranslation(locale, "greeting"));
        translation.setId(id);
        return translation;
    }
}
