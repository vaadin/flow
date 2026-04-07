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
import static org.junit.jupiter.api.Assertions.assertNull;

class FieldSetTest extends ComponentTest {

    @Override
    protected void addProperties() {
        whitelistProperty("content");
        whitelistProperty("legend");
        whitelistProperty("legendText");
    }

    @Test
    void testConstructorParams() {
        FieldSet fieldset = new FieldSet("sample-legend");
        assertEquals("sample-legend", fieldset.getLegend().getText());
        assertEquals(0, fieldset.getContent().count());

        fieldset = new FieldSet(new Paragraph("sample-content"));
        assertNull(fieldset.getLegend());
        assertEquals("sample-content",
                ((Paragraph) fieldset.getContent().findFirst().get())
                        .getText());

        Paragraph content = new Paragraph("content");
        fieldset = new FieldSet("sample-legend", content);
        assertEquals("sample-legend", fieldset.getLegend().getText());
        assertEquals(content, fieldset.getContent().findFirst().get());
    }

    @Test
    void testSetLegendReplacesLegendText() {
        FieldSet fieldset = new FieldSet("legend1", new Paragraph("content"));
        assertEquals("legend1", fieldset.getLegend().getText());

        fieldset.setLegendText("legend2");
        assertEquals("legend2", fieldset.getLegend().getText());

        fieldset.setLegendText(null);
        assertNull(fieldset.getLegend());
    }

    @Test
    void testSetContentReplacesContent() {
        Paragraph content1 = new Paragraph("content1");
        Paragraph content2 = new Paragraph("content2");
        FieldSet fieldset = new FieldSet("text-legend", content1);
        assertEquals(content1, fieldset.getContent().findFirst().get());

        fieldset.remove(content1);
        fieldset.add(content2);
        assertEquals(content2, fieldset.getContent().findFirst().get());

        assertNull(content1.getElement().getParent());
    }

    @Test
    void testFieldSetLegendTextSetting() {
        String expectedText = "Test Legend";
        FieldSet fieldSet = new FieldSet(expectedText);
        assertEquals(expectedText, fieldSet.getLegend().getText(),
                "The legend text should be set correctly.");
    }
}
