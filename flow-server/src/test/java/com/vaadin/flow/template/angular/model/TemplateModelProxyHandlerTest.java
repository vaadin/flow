package com.vaadin.flow.template.angular.model;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.templatemodel.PropertyFilter;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.template.angular.model.BeanModelType;
import com.vaadin.flow.template.angular.model.ModelDescriptor;
import com.vaadin.flow.template.angular.model.TemplateModel;
import com.vaadin.flow.template.angular.model.TemplateModelProxyHandler;
import com.vaadin.flow.template.angular.model.TemplateModelTest.EmptyModel;
import com.vaadin.flow.template.angular.model.TemplateModelTest.EmptyModelTemplate;

public class TemplateModelProxyHandlerTest {

    public static class Model {

        @Override
        public String toString() {
            return "foo";
        }
    }

    public static class BadModel {
        public BadModel(String name) {
        }
    }

    @Test
    public void testEquals() {
        EmptyModelTemplate emptyModelTemplate1 = new EmptyModelTemplate();
        EmptyModelTemplate emptyModelTemplate2 = new EmptyModelTemplate();
        TemplateModel m1 = emptyModelTemplate1.getModel();
        TemplateModel m2 = emptyModelTemplate2.getModel();

        Assert.assertSame(m1.getClass(), m2.getClass());

        Assert.assertFalse(m1.equals(null));
        Assert.assertFalse(m1.equals("foobar"));
        Assert.assertFalse(m1.equals(m2));

        ModelDescriptor<EmptyModel> realModelType = ModelDescriptor
                .get(EmptyModel.class);

        Assert.assertTrue(m1.equals(TemplateModelProxyHandler.createModelProxy(
                emptyModelTemplate1.getElement().getNode(), realModelType)));

        BeanModelType<TemplateModel> wrongModelType = new BeanModelType<>(
                TemplateModel.class, PropertyFilter.ACCEPT_ALL);
        Assert.assertFalse(m1.equals(TemplateModelProxyHandler.createModelProxy(
                emptyModelTemplate1.getElement().getNode(), wrongModelType)));

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

    @Test
    public void objectMethodIsNotIntercepted() {
        EmptyModelTemplate template = new EmptyModelTemplate();

        Model proxy = TemplateModelProxyHandler.createModelProxy(
                template.getElement().getNode(),
                new BeanModelType<>(Model.class, PropertyFilter.ACCEPT_ALL));
        Assert.assertEquals(System.identityHashCode(proxy), proxy.hashCode());
    }

    @Test
    public void notAccessorIsNotIntercepted() {
        EmptyModelTemplate template = new EmptyModelTemplate();

        Model proxy = TemplateModelProxyHandler.createModelProxy(
                template.getElement().getNode(),
                new BeanModelType<>(Model.class, PropertyFilter.ACCEPT_ALL));
        Assert.assertEquals("foo", proxy.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void noDefaultConstructor_throwsException() {
        EmptyModelTemplate template = new EmptyModelTemplate();

        TemplateModelProxyHandler.createModelProxy(
                template.getElement().getNode(),
                new BeanModelType<>(BadModel.class, PropertyFilter.ACCEPT_ALL));
    }

    // https://github.com/vaadin/flow/issues/1205
    public class NotStaticModel {
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonStaticNestedClass_throwsException() {
        EmptyModelTemplate template = new EmptyModelTemplate();

        TemplateModelProxyHandler.createModelProxy(
                template.getElement().getNode(), new BeanModelType<>(
                        NotStaticModel.class, PropertyFilter.ACCEPT_ALL));
    }
}
