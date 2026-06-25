/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.flowsecurity.views;

import java.util.Locale;
import java.util.Optional;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.internal.LocaleUtil;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "translations")
@AnonymousAllowed
public class TranslationView extends Div {

    public static final String TEST_VIEW_ID = "TranslationView";
    public static final String LOCALES_ID = "available-locales";

    public TranslationView() {
        setId(TEST_VIEW_ID);

        Span defaultLang = new Span(getTranslation("label", Locale.ENGLISH));
        defaultLang.setId("english");
        Span french = new Span(getTranslation("label", Locale.FRANCE));
        french.setId("french");
        Span japanese = new Span(getTranslation("label", Locale.JAPAN));
        japanese.setId("japanese");

        Optional<I18NProvider> i18NProvider = LocaleUtil.getI18NProvider();
        if (i18NProvider.isPresent()) {
            add(new Span("Available translation locales:"));
            StringBuilder locales = new StringBuilder();
            for (Locale locale : i18NProvider.get().getProvidedLocales()) {
                if (locales.length() > 0) {
                    locales.append(", ");
                }
                locales.append(locale.toString());
            }
            Span localeSpan = new Span(locales.toString());
            localeSpan.setId(LOCALES_ID);
            add(localeSpan, new Div());
        }
        add(defaultLang, new Div(), french, new Div(), japanese);
    }
}
