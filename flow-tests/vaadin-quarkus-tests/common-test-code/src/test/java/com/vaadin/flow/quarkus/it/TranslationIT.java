/*
 * Copyright 2000-2024 Vaadin Ltd.
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
package com.vaadin.flow.quarkus.it;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.quarkus.it.i18n.TranslationView;
import com.vaadin.flow.test.AbstractChromeIT;

@QuarkusIntegrationTest
public class TranslationIT extends AbstractChromeIT {
    @Override
    protected String getTestPath() {
        return "/translations";
    }

    @Test
    public void translationFilesExist_defaultI18NInstantiated_languagesWork() {
        open();

        String locales = $(SpanElement.class).id(TranslationView.LOCALES_ID)
                .getText();
        Assertions.assertTrue(locales.contains("de"),
                "Couldn't verify German locale");
        Assertions.assertTrue(locales.contains("fi_FI"),
                "Couldn't verify Finnish locale");
        Assertions.assertTrue(locales.contains("fr_FR"),
                "Couldn't verify French locale");
        Assertions.assertTrue(locales.contains("ja_JP"),
                "Couldn't verify Japanese locale");

        Assertions.assertEquals("Default",
                $(SpanElement.class).id("english").getText());
        Assertions.assertEquals("Deutsch",
                $(SpanElement.class).id("german").getText());
        Assertions.assertEquals("Deutsch",
                $(SpanElement.class).id("germany").getText());
        Assertions.assertEquals("Suomi",
                $(SpanElement.class).id("finnish").getText());
        Assertions.assertEquals("français",
                $(SpanElement.class).id("french").getText());
        Assertions.assertEquals("日本語",
                $(SpanElement.class).id("japanese").getText());
    }

    @Test
    public void translationFilesExist_defaultI18NInstantiated_updateFromExternalThreadWorks() {
        open();

        waitUntilNot(driver -> $(SpanElement.class).id("dynamic").getText()
                .equals("waiting"));

        Assertions.assertEquals("français",
                $(SpanElement.class).id("dynamic").getText(),
                "Dynamic update from thread should have used correct bundle.");
    }
}
