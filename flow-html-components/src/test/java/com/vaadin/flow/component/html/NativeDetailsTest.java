/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.html;

import org.junit.Assert;
import org.junit.Test;

public class NativeDetailsTest extends ComponentTest {
    // Actual test methods in super class

    @Override
    protected void addProperties() {
        // Properties are whitelisted because ComponentTest
        // expects to have PropertyDescriptor for each property.
        whitelistProperty("content");
        whitelistProperty("summary");
        whitelistProperty("summaryText");
        whitelistProperty("open");
    }

    @Test
    public void testConstructorParams() {
        NativeDetails details = new NativeDetails("text-summary");
        Assert.assertEquals("text-summary", details.getSummaryText());
        Assert.assertNull(details.getContent());

        details = new NativeDetails(new Paragraph("text-summary"));
        Assert.assertEquals("", details.getSummaryText());
        Assert.assertEquals("text-summary",
                details.getSummary().getElement().getTextRecursively());
        Assert.assertNull(details.getContent());

        Paragraph content = new Paragraph("content");
        details = new NativeDetails("text-summary", content);
        Assert.assertEquals("text-summary", details.getSummaryText());
        Assert.assertEquals(content, details.getContent());
    }

    @Test
    public void testSetSummaryReplacesSummary() {
        Paragraph summmary2 = new Paragraph("summary2");
        NativeDetails details = new NativeDetails("summary1",
                new Paragraph("content"));
        Assert.assertEquals("summary1", details.getSummaryText());

        details.setSummary(summmary2);
        Assert.assertEquals("summary2",
                details.getSummary().getElement().getTextRecursively());

        details.setSummaryText("summary3");
        Assert.assertEquals("summary3", details.getSummaryText());
    }

    @Test
    public void testSetContentReplacesContent() {
        Paragraph content1 = new Paragraph("content1");
        Paragraph content2 = new Paragraph("content2");
        NativeDetails details = new NativeDetails("text-summary", content1);
        Assert.assertEquals(content1, details.getContent());
        Assert.assertTrue(content1.getParent().isPresent());
        Assert.assertFalse(content2.getParent().isPresent());

        details.setContent(content2);
        Assert.assertEquals(content2, details.getContent());
        Assert.assertTrue(content2.getParent().isPresent());
        Assert.assertFalse(content1.getParent().isPresent());
    }
}
