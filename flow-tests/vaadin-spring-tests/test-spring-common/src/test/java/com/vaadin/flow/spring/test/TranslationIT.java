/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.SpanElement;

public class TranslationIT extends AbstractSpringTest {
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
    }
}
