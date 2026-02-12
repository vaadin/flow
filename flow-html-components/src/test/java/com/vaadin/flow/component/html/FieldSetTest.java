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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FieldSetTest extends ComponentTest {

    @Override
    protected void addProperties() {
        whitelistProperty("content");
        whitelistProperty("legend");
        whitelistProperty("legendText");
    }

    @Test
    public void testConstructorParams() {
        FieldSet fieldset = new FieldSet("sample-legend");
        Assertions.assertEquals("sample-legend",
                fieldset.getLegend().getText());
        Assertions.assertEquals(0, fieldset.getContent().count());

        fieldset = new FieldSet(new Paragraph("sample-content"));
        Assertions.assertNull(fieldset.getLegend());
        Assertions.assertEquals("sample-content",
                ((Paragraph) fieldset.getContent().findFirst().get())
                        .getText());

        Paragraph content = new Paragraph("content");
        fieldset = new FieldSet("sample-legend", content);
        Assertions.assertEquals("sample-legend",
                fieldset.getLegend().getText());
        Assertions.assertEquals(content,
                fieldset.getContent().findFirst().get());
    }

    @Test
    public void testSetLegendReplacesLegendText() {
        FieldSet fieldset = new FieldSet("legend1", new Paragraph("content"));
        Assertions.assertEquals("legend1", fieldset.getLegend().getText());

        fieldset.setLegendText("legend2");
        Assertions.assertEquals("legend2", fieldset.getLegend().getText());

        fieldset.setLegendText(null);
        Assertions.assertNull(fieldset.getLegend());
    }

    @Test
    public void testSetContentReplacesContent() {
        Paragraph content1 = new Paragraph("content1");
        Paragraph content2 = new Paragraph("content2");
        FieldSet fieldset = new FieldSet("text-legend", content1);
        Assertions.assertEquals(content1,
                fieldset.getContent().findFirst().get());

        fieldset.remove(content1);
        fieldset.add(content2);
        Assertions.assertEquals(content2,
                fieldset.getContent().findFirst().get());

        Assertions.assertNull(content1.getElement().getParent());
    }

    @Test
    public void testFieldSetLegendTextSetting() {
        String expectedText = "Test Legend";
        FieldSet fieldSet = new FieldSet(expectedText);
        Assertions.assertEquals(expectedText, fieldSet.getLegend().getText(),
                "The legend text should be set correctly.");
    }
}
