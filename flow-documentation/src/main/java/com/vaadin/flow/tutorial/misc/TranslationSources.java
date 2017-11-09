/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.tutorial.misc;

import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.html.Div;
import com.vaadin.ui.i18n.I18NProvider;
import com.vaadin.ui.i18n.LocaleChangeEvent;
import com.vaadin.ui.i18n.LocaleChangeObserver;

@CodeFor("miscellaneous/tutorial-i18n-localization.asciidoc")
public class TranslationSources {
    public class LocaleObserver extends Div implements LocaleChangeObserver {

        @Override
        public void localeChange(LocaleChangeEvent event) {
            setText(getProvider().getTranslation("my.translation",
                    "My parameter!"));
        }
    }

    public class MyLocale extends Div {

        public MyLocale() {
            I18NProvider provider = VaadinService.getCurrent().getInstantiator()
                    .getI18NProvider();

            setText(provider.getTranslation("my.translation", "My parameter!"));
        }
    }
}
