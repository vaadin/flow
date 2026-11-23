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

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class TranslationIT extends ChromeBrowserTest {

    @Test
    public void open_localesListedFromTranslationFolder() {
        open();

        Assert.assertEquals("Available translation locales: de, fi_FI",
                getLine(TranslationView.LOCALES_ID));
    }

    @Test
    public void open_translationReadForEachLocale() {
        open();

        Assert.assertEquals("en: Hello from a translation",
                getLine(TranslationView.DEFAULT_ID));
        Assert.assertEquals("de: Hallo aus einer Übersetzung",
                getLine(TranslationView.GERMAN_ID));
        Assert.assertEquals("fi_FI: Terveisiä käännöksestä",
                getLine(TranslationView.FINNISH_ID));
    }

    private String getLine(String id) {
        return $(DivElement.class).id(id).getText();
    }

    @Override
    protected String getTestPath() {
        return "/translations";
    }
}
