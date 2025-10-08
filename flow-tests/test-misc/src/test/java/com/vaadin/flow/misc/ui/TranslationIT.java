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
package com.vaadin.flow.misc.ui;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class TranslationIT extends ChromeBrowserTest {
    @Override
    protected String getTestPath() {
        return "/translations";
    }

    @Test
    public void translationFilesExist_defaultI18NInstantiated_languagesWork() {
        open();

        String locales = $(SpanElement.class).id(TranslationView.LOCALES_ID)
                .getText();
        Assert.assertTrue("Couldn't verify German locale",
                locales.contains("de"));
        Assert.assertTrue("Couldn't verify Finnish locale",
                locales.contains("fi_FI"));
        Assert.assertTrue("Couldn't verify French locale",
                locales.contains("fr_FR"));
        Assert.assertTrue("Couldn't verify Japanese locale",
                locales.contains("ja_JP"));

        Assert.assertEquals("Default",
                $(SpanElement.class).id("english").getText());
        Assert.assertEquals("Deutsch",
                $(SpanElement.class).id("german").getText());
        Assert.assertEquals("Deutsch",
                $(SpanElement.class).id("germany").getText());
        Assert.assertEquals("Suomi",
                $(SpanElement.class).id("finnish").getText());
        Assert.assertEquals("français",
                $(SpanElement.class).id("french").getText());
        Assert.assertEquals("日本語",
                $(SpanElement.class).id("japanese").getText());

        Assert.assertEquals("Default",
                $(SpanElement.class).id("static-method").getText());
    }

    @Test
    public void translationFilesExist_defaultI18NInstantiated_updateFromExternalThreadWorks() {
        open();

        waitUntilNot(driver -> $(SpanElement.class).id("dynamic").getText()
                .equals("waiting"));

        Assert.assertEquals(
                "Dynamic update from thread should have used correct bundle.",
                "français", $(SpanElement.class).id("dynamic").getText());
    }
}
