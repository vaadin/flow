/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
        Assert.assertEquals(0, fieldset.getContent().count());

        fieldset = new FieldSet(new Paragraph("sample-content"));
        Assert.assertNull(fieldset.getLegend());
        Assert.assertEquals("sample-content",
                ((Paragraph) fieldset.getContent().findFirst().get())
                        .getText());

        Paragraph content = new Paragraph("content");
        fieldset = new FieldSet("sample-legend", content);
        Assert.assertEquals("sample-legend", fieldset.getLegend().getText());
        Assert.assertEquals(content, fieldset.getContent().findFirst().get());
    }

    @Test
    public void testSetLegendReplacesLegendText() {
        FieldSet fieldset = new FieldSet("legend1", new Paragraph("content"));
        Assert.assertEquals("legend1", fieldset.getLegend().getText());

        fieldset.setLegendText("legend2");
        Assert.assertEquals("legend2", fieldset.getLegend().getText());

        fieldset.setLegendText(null);
        Assert.assertNull(fieldset.getLegend());
    }

    @Test
    public void testSetContentReplacesContent() {
        Paragraph content1 = new Paragraph("content1");
        Paragraph content2 = new Paragraph("content2");
        FieldSet fieldset = new FieldSet("text-legend", content1);
        Assert.assertEquals(content1, fieldset.getContent().findFirst().get());

        fieldset.remove(content1);
        fieldset.add(content2);
        Assert.assertEquals(content2, fieldset.getContent().findFirst().get());

        Assert.assertNull(content1.getElement().getParent());
    }

    @Test
    public void testFieldSetLegendTextSetting() {
        String expectedText = "Test Legend";
        FieldSet fieldSet = new FieldSet(expectedText);
        Assert.assertEquals("The legend text should be set correctly.",
                expectedText, fieldSet.getLegend().getText());
    }
}
