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
package com.vaadin.flow.component.html;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeDetailsTest extends ComponentTest {
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
    void testConstructorParams() {
        NativeDetails details = new NativeDetails("text-summary");
        assertEquals("text-summary", details.getSummaryText());
        assertNull(details.getContent());

        details = new NativeDetails(new Paragraph("text-summary"));
        assertEquals("", details.getSummaryText());
        assertEquals("text-summary",
                details.getSummary().getElement().getTextRecursively());
        assertNull(details.getContent());

        Paragraph content = new Paragraph("content");
        details = new NativeDetails("text-summary", content);
        assertEquals("text-summary", details.getSummaryText());
        assertEquals(content, details.getContent());
    }

    @Test
    void testSetSummaryReplacesSummary() {
        Paragraph summmary2 = new Paragraph("summary2");
        NativeDetails details = new NativeDetails("summary1",
                new Paragraph("content"));
        assertEquals("summary1", details.getSummaryText());

        details.setSummary(summmary2);
        assertEquals("summary2",
                details.getSummary().getElement().getTextRecursively());

        details.setSummaryText("summary3");
        assertEquals("summary3", details.getSummaryText());
    }

    @Test
    void testSetContentReplacesContent() {
        Paragraph content1 = new Paragraph("content1");
        Paragraph content2 = new Paragraph("content2");
        NativeDetails details = new NativeDetails("text-summary", content1);
        assertEquals(content1, details.getContent());
        assertTrue(content1.getParent().isPresent());
        assertFalse(content2.getParent().isPresent());

        details.setContent(content2);
        assertEquals(content2, details.getContent());
        assertTrue(content2.getParent().isPresent());
        assertFalse(content1.getParent().isPresent());
    }
}
