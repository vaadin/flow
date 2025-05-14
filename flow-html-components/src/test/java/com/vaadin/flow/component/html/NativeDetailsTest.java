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
