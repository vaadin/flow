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
package com.vaadin.flow.uitest.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@Route("com.vaadin.flow.uitest.ui.LocaleChangeView")
public class LocaleChangeView extends Div {

    public static final String SAME_UI_RESULT_ID = "same-ui-result-id";
    public static final String CHANGE_LOCALE_BUTTON_ID = "change-locale-button-id";
    public static final String SHOW_RESULTS_BUTTON_ID = "show-results-button-id";

    public LocaleChangeView() {
        Locale defaultLocale = UI.getCurrent().getLocale();
        NativeButton changeLocaleButton = new NativeButton("Change Locale",
                click -> {
                    if (defaultLocale.equals(Locale.ENGLISH)) {
                        changeSessionLocale(Locale.FRANCE);
                    } else {
                        changeSessionLocale(Locale.ENGLISH);
                    }
                });
        changeLocaleButton.setId(CHANGE_LOCALE_BUTTON_ID);

        LocaleObserverComponent localeObserverComponent = new LocaleObserverComponent(
                defaultLocale);

        NativeButton showLocaleUpdates = new NativeButton("Show locale updates",
                click -> localeObserverComponent.showLocaleUpdates());
        showLocaleUpdates.setId(SHOW_RESULTS_BUTTON_ID);

        add(changeLocaleButton, showLocaleUpdates, new HtmlComponent(Tag.BR),
                localeObserverComponent);
    }

    private void changeSessionLocale(Locale locale) {
        UI.getCurrent().getSession().setLocale(locale);
    }

    @Tag("Div")
    private static class LocaleObserverComponent extends Component
            implements HasComponents, LocaleChangeObserver {

        private final Locale defaultLocale;

        public LocaleObserverComponent(Locale defaultLocale) {
            this.defaultLocale = defaultLocale;
        }

        @Override
        public void localeChange(LocaleChangeEvent event) {
            if (event.getLocale().equals(defaultLocale)) {
                return;
            }
            final boolean sameUI = getUI().isPresent()
                    && getUI().get() == UI.getCurrent();

            registerLocaleUpdate(sameUI);
        }

        public void showLocaleUpdates() {
            LocaleUpdates localeUpdates = getLocaleUpdates();
            renderResults(localeUpdates);
        }

        private void renderResults(LocaleUpdates localeUpdates) {
            for (int i = 0; i < localeUpdates.getUiCheckResults().size(); i++) {
                Boolean checkResult = localeUpdates.getUiCheckResults().get(i);
                Span sameUIResult = new Span();
                sameUIResult
                        .setId(String.format("%s-%d", SAME_UI_RESULT_ID, i));
                sameUIResult.setText(Boolean.toString(checkResult));
                Span caption = new Span(String
                        .format("Component %d uses current UI instance = ", i));
                add(caption, sameUIResult, new HtmlComponent(Tag.BR));
            }
        }

        private void registerLocaleUpdate(boolean sameUIcheckUp) {
            VaadinSession session = VaadinSession.getCurrent();
            // no lock needed, since flow listeners are called under lock
            // already
            LocaleUpdates localeUpdates = session
                    .getAttribute(LocaleUpdates.class);
            if (localeUpdates == null) {
                localeUpdates = new LocaleUpdates();
            }
            localeUpdates.addUICheckResult(sameUIcheckUp);
            session.setAttribute(LocaleUpdates.class, localeUpdates);
        }

        private LocaleUpdates getLocaleUpdates() {
            VaadinSession session = VaadinSession.getCurrent();
            // no lock needed, since flow listeners are called under lock
            // already
            return session.getAttribute(LocaleUpdates.class);
        }
    }

    static class LocaleUpdates {
        private final List<Boolean> uiCheckResults = new ArrayList<>(3);

        public void addUICheckResult(boolean uiCheckResult) {
            uiCheckResults.add(uiCheckResult);
        }

        public List<Boolean> getUiCheckResults() {
            return uiCheckResults;
        }
    }
}
