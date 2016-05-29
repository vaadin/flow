package com.vaadin.hummingbird.template.model;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.template.model.TemplateModelTest.EmptyModel;
import com.vaadin.hummingbird.template.model.TemplateModelTest.EmptyModelTemplate;
import com.vaadin.ui.UI;

public class TemplateModelProxyHandlerTest {

    @Test
    public void testEquals() {
        EmptyModelTemplate emptyModelTemplate1 = new EmptyModelTemplate();
        EmptyModelTemplate emptyModelTemplate2 = new EmptyModelTemplate();
        TemplateModel m1 = emptyModelTemplate1.getModel();
        TemplateModel m2 = emptyModelTemplate2.getModel();

        Assert.assertFalse(m1.equals(null));
        Assert.assertFalse(m1.equals("foobar"));
        Assert.assertFalse(m1.equals(m2));
        Assert.assertTrue(m1.equals(TemplateModelProxyHandler.createModelProxy(
                emptyModelTemplate1.getElement().getNode(), EmptyModel.class)));
        Assert.assertTrue(m1.equals(TemplateModelProxyHandler.createModelProxy(
                emptyModelTemplate1.getElement().getNode(), TemplateModel.class)));
        Assert.assertTrue(m2.equals(m2));
    }

    @Test
    public void testHashCode() {
        EmptyModelTemplate emptyModelTemplate1 = new EmptyModelTemplate();
        EmptyModelTemplate emptyModelTemplate2 = new EmptyModelTemplate();
        TemplateModel m1 = emptyModelTemplate1.getModel();
        TemplateModel m2 = emptyModelTemplate2.getModel();

        Assert.assertNotEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    public void testToString() {
        EmptyModelTemplate emptyModelTemplate1 = new EmptyModelTemplate();
        EmptyModelTemplate emptyModelTemplate2 = new EmptyModelTemplate();
        TemplateModel m1 = emptyModelTemplate1.getModel();
        TemplateModel m2 = emptyModelTemplate2.getModel();

        // add templates to UI so that their state nodes get an id which is used
        // in toString()
        new UI().add(emptyModelTemplate1, emptyModelTemplate2);

        Assert.assertEquals(m1.toString(), m1.toString());
        Assert.assertNotEquals(m1.toString(), m2.toString());
    }
}
