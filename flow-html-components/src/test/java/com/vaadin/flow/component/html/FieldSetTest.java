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
package com.vaadin.flow.component.html;

import org.junit.Assert;
import org.junit.Test;

public class FieldSetTest extends ComponentTest {

    @Override
    protected void addProperties() {
        whitelistProperty("content");
        whitelistProperty("legend");
        whitelistProperty("legendText");
    }

    @Test
    public void testConstructorParams() {
        FieldSet fieldset = new FieldSet("sample-legend");
        Assert.assertEquals("sample-legend", fieldset.getLegend().getText());
        Assert.assertNull(fieldset.getContent());

        fieldset = new FieldSet(new Paragraph("sample-content"));
        Assert.assertEquals("", fieldset.getLegend().getText());
        Assert.assertEquals("sample-content",
                fieldset.getContent().getElement().getTextRecursively());

        Paragraph content = new Paragraph("content");
        fieldset = new FieldSet("sample-legend", content);
        Assert.assertEquals("sample-legend", fieldset.getLegend().getText());
        Assert.assertEquals(content, fieldset.getContent());
    }

    @Test
    public void testSetLegendReplacesLegendText() {
        Paragraph legend2 = new Paragraph("legend2");
        FieldSet fieldset = new FieldSet("legend1", new Paragraph("content"));
        Assert.assertEquals("legend1", fieldset.getLegend().getText());

        fieldset.setLegendText("legend2");
        Assert.assertEquals("legend2", fieldset.getLegend().getText());

        fieldset.getLegend().setText("legend3");
        Assert.assertEquals("legend3", fieldset.getLegend().getText());
    }

    @Test
    public void testSetContentReplacesContent() {
        Paragraph content1 = new Paragraph("content1");
        Paragraph content2 = new Paragraph("content2");
        FieldSet fieldset = new FieldSet("text-legend", content1);
        Assert.assertEquals(content1, fieldset.getContent());
        Assert.assertTrue(content1.getParent().isPresent());
        Assert.assertFalse(content2.getParent().isPresent());

        fieldset.setContent(content2);
        Assert.assertEquals(content2, fieldset.getContent());
        Assert.assertTrue(content2.getParent().isPresent());
        Assert.assertFalse(content1.getParent().isPresent());
    }
}
