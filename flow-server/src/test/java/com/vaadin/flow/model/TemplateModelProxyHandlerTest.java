package com.vaadin.flow.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.model.TemplateModelTest.EmptyModel;
import com.vaadin.flow.model.TemplateModelTest.EmptyModelTemplate;
import com.vaadin.function.DeploymentConfiguration;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.UI;
import com.vaadin.util.HasCurrentService;

import net.jcip.annotations.NotThreadSafe;

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

        assertSame(m1.getClass(), m2.getClass());

        assertFalse(m1.equals(null));
        assertFalse(m1.equals("foobar"));
        assertFalse(m1.equals(m2));

        ModelDescriptor<EmptyModel> realModelType = ModelDescriptor
                .get(EmptyModel.class);

        assertTrue(m1.equals(TemplateModelProxyHandler.createModelProxy(
                emptyModelTemplate1.getElement().getNode(), realModelType)));

        class TestTemplateModelType extends BeanModelType<TemplateModel> {

            public TestTemplateModelType() {
                super(TemplateModel.class, PropertyFilter.ACCEPT_ALL, true);
            }

        }

        BeanModelType<TemplateModel> wrongModelType = new TestTemplateModelType();
        assertFalse(m1.equals(TemplateModelProxyHandler.createModelProxy(
                emptyModelTemplate1.getElement().getNode(), wrongModelType)));

        assertTrue(m2.equals(m2));
    }

    @Test
    public void testHashCode() {
        EmptyModelTemplate emptyModelTemplate1 = new EmptyModelTemplate();
        EmptyModelTemplate emptyModelTemplate2 = new EmptyModelTemplate();
        TemplateModel m1 = emptyModelTemplate1.getModel();
        TemplateModel m2 = emptyModelTemplate2.getModel();

        assertNotEquals(m1.hashCode(), m2.hashCode());
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

        assertEquals(m1.toString(), m1.toString());
        assertNotEquals(m1.toString(), m2.toString());
    }

    @Test
    public void objectMethodIsNotIntercepted() {
        EmptyModelTemplate template = new EmptyModelTemplate();

        Model proxy = TemplateModelProxyHandler.createModelProxy(
                template.getElement().getNode(), new TestModelType());
        assertEquals(System.identityHashCode(proxy), proxy.hashCode());
    }

    @Test
    public void notAccessorIsNotIntercepted() {
        EmptyModelTemplate template = new EmptyModelTemplate();

        Model proxy = TemplateModelProxyHandler.createModelProxy(
                template.getElement().getNode(), new TestModelType());
        assertEquals("foo", proxy.toString());
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

    @Test
    public void proxyModelTypeNameIsOriginalTypeNameWithSuffix() {
        EmptyModelTemplate emptyModelTemplate1 = new EmptyModelTemplate();
        TemplateModelTest.EmptyModel model = emptyModelTemplate1.getModel();

        assertTrue(model.getClass().getCanonicalName().startsWith(
                TemplateModelTest.EmptyModel.class.getCanonicalName()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void beanHasNoProperties_throwsException() {
        new BeanModelType<>(Model.class, PropertyFilter.ACCEPT_ALL);
    }
}
