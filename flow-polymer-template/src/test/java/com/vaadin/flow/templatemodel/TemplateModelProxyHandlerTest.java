/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.templatemodel;

import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.polymertemplate.HasCurrentService;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.templatemodel.TemplateModelTest.EmptyModel;
import com.vaadin.flow.templatemodel.TemplateModelTest.EmptyModelTemplate;

@NotThreadSafe
public class TemplateModelProxyHandlerTest extends HasCurrentService {

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

    public static class TestModelType extends BeanModelType<Model> {

        public TestModelType() {
            super(Model.class, PropertyFilter.ACCEPT_ALL, true);
        }

    }

    @Override
    protected VaadinService createService() {
        VaadinService service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);

        return service;
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

        class TestTemplateModelType extends BeanModelType<TemplateModel> {

            public TestTemplateModelType() {
                super(TemplateModel.class, PropertyFilter.ACCEPT_ALL, true);
            }

        }

        BeanModelType<TemplateModel> wrongModelType = new TestTemplateModelType();
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
                template.getElement().getNode(), new TestModelType());
        Assert.assertEquals(System.identityHashCode(proxy), proxy.hashCode());
    }

    @Test
    public void notAccessorIsNotIntercepted() {
        EmptyModelTemplate template = new EmptyModelTemplate();

        Model proxy = TemplateModelProxyHandler.createModelProxy(
                template.getElement().getNode(), new TestModelType());
        Assert.assertEquals("foo", proxy.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void noDefaultConstructor_throwsException() {
        EmptyModelTemplate template = new EmptyModelTemplate();

        TemplateModelProxyHandler.createModelProxy(
                template.getElement().getNode(), new BeanModelType<>(
                        BadModel.class, PropertyFilter.ACCEPT_ALL, false));
    }

    // https://github.com/vaadin/flow/issues/1205
    public class NotStaticModel {
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonStaticNestedClass_throwsException() {
        EmptyModelTemplate template = new EmptyModelTemplate();

        TemplateModelProxyHandler.createModelProxy(
                template.getElement().getNode(),
                new BeanModelType<>(NotStaticModel.class,
                        PropertyFilter.ACCEPT_ALL, false));
    }

    @Test
    public void proxyModelTypeNameIsOriginalTypeNameWithSuffix() {
        EmptyModelTemplate emptyModelTemplate1 = new EmptyModelTemplate();
        TemplateModelTest.EmptyModel model = emptyModelTemplate1.getModel();

        Assert.assertTrue(model.getClass().getCanonicalName().startsWith(
                TemplateModelTest.EmptyModel.class.getCanonicalName()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void beanHasNoProperties_throwsException() {
        new BeanModelType<>(Model.class, PropertyFilter.ACCEPT_ALL, false);
    }
}
