package com.vaadin.hummingbird.template.model;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.template.model.TemplateModelTest.MyModel;
import com.vaadin.hummingbird.template.model.TemplateModelTest.TestTemplate;
import com.vaadin.ui.UI;

public class TemplateModelProxyHandlerTest {

    @Test
    public void testEquals() {
        TestTemplate testTemplate1 = new TestTemplate();
        TestTemplate testTemplate2 = new TestTemplate();
        TemplateModel m1 = testTemplate1.getModel();
        TemplateModel m2 = testTemplate2.getModel();

        Assert.assertFalse(m1.equals(null));
        Assert.assertFalse(m1.equals("foobar"));
        Assert.assertFalse(m1.equals(m2));
        Assert.assertTrue(m1.equals(TemplateModelProxyHandler.createModelProxy(
                testTemplate1.getElement().getNode(), MyModel.class)));
        Assert.assertTrue(m1.equals(TemplateModelProxyHandler.createModelProxy(
                testTemplate1.getElement().getNode(), TemplateModel.class)));
        Assert.assertTrue(m2.equals(m2));
    }

    @Test
    public void testHashCode() {
        TestTemplate testTemplate1 = new TestTemplate();
        TestTemplate testTemplate2 = new TestTemplate();
        TemplateModel m1 = testTemplate1.getModel();
        TemplateModel m2 = testTemplate2.getModel();

        Assert.assertNotEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    public void testToString() {
        TestTemplate testTemplate1 = new TestTemplate();
        TestTemplate testTemplate2 = new TestTemplate();
        TemplateModel m1 = testTemplate1.getModel();
        TemplateModel m2 = testTemplate2.getModel();

        new UI().add(testTemplate1, testTemplate2);

        Assert.assertEquals(m1.toString(), m1.toString());
        Assert.assertNotEquals(m1.toString(), m2.toString());
    }
}
