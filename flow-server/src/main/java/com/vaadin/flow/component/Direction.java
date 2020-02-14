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

package com.vaadin.flow.component;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Class for setting the direction on the document element.
 *
 * @author Vaadin Ltd
 * @since 2.1.4
 */
class Direction implements Serializable {

    private static final Set<String> rtlSet;

    static {
        Set<String> lang = new HashSet<>();
        lang.add("ar"); // Arabic
        lang.add("dv"); // Divehi
        lang.add("fa"); // Persian
        lang.add("ha"); // Hausa
        lang.add("he"); // Hebrew
        lang.add("iw"); // Hebrew
        lang.add("ji"); // Yiddish
        lang.add("ps"); // Pushto
        lang.add("sd"); // Sindhi
        lang.add("ug"); // Uighur
        lang.add("ur"); // Urdu
        lang.add("yi"); // Yiddish

        rtlSet = Collections.unmodifiableSet(lang);
    }

    static void set(UI ui, Locale locale) {
        ui.getElement().executeJs("document.querySelector('html').dir=$0",
                getDirectionForLocale(locale));
    }

    /**
     * Returns direction string based on the locale provided.
     *
     * @param locale
     *            the locale set for the UI
     *
     * @return the direction string
     */
    public static String getDirectionForLocale(Locale locale) {
        return rtlSet.contains(locale.getLanguage()) ? "rtl" : "ltr";
    }
}
