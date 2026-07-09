/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.flowsecurity;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.spring.flowsecurity.views.TranslationView;
import com.vaadin.flow.spring.test.AbstractSpringTest;

public class TranslationIT extends AbstractSpringTest {
    @Override
    protected String getTestPath() {
        return "/translations";
    }

    @Test
    public void translationFilesExist_customI18NInstantiated_languagesWork() {
        open();

        String locales = $(SpanElement.class).id(TranslationView.LOCALES_ID)
                .getText();
        Assert.assertTrue("Couldn't verify French locale",
                locales.contains("fr_FR"));
        Assert.assertFalse("Japanese locale shouldn't be defined",
                locales.contains("ja_JP"));

        Assert.assertEquals("Default",
                $(SpanElement.class).id("english").getText());
        Assert.assertEquals("français",
                $(SpanElement.class).id("french").getText());
        Assert.assertEquals("Default",
                $(SpanElement.class).id("japanese").getText());
    }
}
