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
package com.vaadin.cdi.itest;

import java.io.File;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.cdi.itest.i18n.TranslationView;

public class TranslationTest extends AbstractCdiTest {

    @Deployment(testable = false)
    public static WebArchive createCdiServletEnabledDeployment() {
        return ArchiveProvider
                .createWebArchive("translations", TranslationView.class)
                .addAsResource(new File("src/main/resources/vaadin-i18n"));

    }

    @Override
    protected String getTestPath() {
        return "/translations";
    }

    @Test
    public void translationFilesExist_defaultI18NInstantiated_languagesWork() {
        open();

        String locales = $("span").id(TranslationView.LOCALES_ID).getText();
        Assert.assertTrue("Couldn't verify German locale",
                locales.contains("de"));
        Assert.assertTrue("Couldn't verify Finnish locale",
                locales.contains("fi_FI"));
        Assert.assertTrue("Couldn't verify French locale",
                locales.contains("fr_FR"));
        Assert.assertTrue("Couldn't verify Japanese locale",
                locales.contains("ja_JP"));

        Assert.assertEquals("Default", $("span").id("english").getText());
        Assert.assertEquals("Deutsch", $("span").id("german").getText());
        Assert.assertEquals("Deutsch", $("span").id("germany").getText());
        Assert.assertEquals("Suomi", $("span").id("finnish").getText());
        Assert.assertEquals("français", $("span").id("french").getText());
        Assert.assertEquals("日本語", $("span").id("japanese").getText());
    }

    @Test
    public void translationFilesExist_defaultI18NInstantiated_updateFromExternalThreadWorks() {
        open();

        waitUntilNot(
                driver -> $("span").id("dynamic").getText().equals("waiting"));

        Assert.assertEquals(
                "Dynamic update from thread should have used correct bundle.",
                "français", $("span").id("dynamic").getText());
    }
}
