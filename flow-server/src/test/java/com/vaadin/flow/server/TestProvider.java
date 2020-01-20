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
package com.vaadin.flow.server;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.i18n.I18NProvider;

/**
 * Translation provider test class.
 */
public class TestProvider implements I18NProvider {

    @Override
    public List<Locale> getProvidedLocales() {
        return Arrays.asList(Locale.ENGLISH);
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        return "!" + key + "!";
    }
}
