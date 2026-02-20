/**
 * Copyright (C) 2022-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.templatemodel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.polymertemplate.HasCurrentService;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.polymertemplate.TemplateParser.TemplateData;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.change.NodeChange;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.internal.nodefeature.ModelList;
import com.vaadin.flow.internal.nodefeature.NodeList;
import com.vaadin.flow.server.VaadinService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TemplateModelTest extends HasCurrentService {

    public interface EmptyModel extends TemplateModel {
    }

    public interface BasicTypeModel extends TemplateModel {
        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        boolean getBooleanPrimitive();

        boolean isBooleanPrimitive();

        void setBooleanPrimitive(boolean b);

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        Boolean getBoolean();

        void setBoolean(Boolean b);

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        int getInt();

        void setInt(int i);

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        Integer getInteger();

        void setInteger(Integer i);

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        double getDoublePrimitive();

        void setDoublePrimitive(double d);

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        Double getDouble();

        void setDouble(Double d);

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        String getString();

        void setString(String s);
    }

    public interface NotSupportedModel extends TemplateModel {
        void setLong(long l);

        long getLong();

        void setFoo();

        int setFoo(int foo);

        int getFoo(int foo);

        void getFoo();

        void setFoo(int x, int y);

        void setfoo(int i);

        int isbar();
    }

    public static class BeanImpl extends Bean {

        private final AtomicInteger stringAccessCount;

        public BeanImpl(AtomicInteger stringAccessCount) {
            this.stringAccessCount = stringAccessCount;
        }

        @Override
        public String getString() {
            stringAccessCount.incrementAndGet();
            return super.getString();
        }
    }

    public interface BeanModel extends TemplateModel {
        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        @AllowClientUpdates(value = ClientUpdateMode.ALLOW, path = "booleanObject")
        @AllowClientUpdates(value = ClientUpdateMode.ALLOW, path = "intObject")
        @AllowClientUpdates(value = ClientUpdateMode.ALLOW, path = "intValue")
        @AllowClientUpdates(value = ClientUpdateMode.ALLOW, path = "doubleValue")
        @AllowClientUpdates(value = ClientUpdateMode.ALLOW, path = "booleanValue")
        Bean getBean();

        void setBean(Bean bean);
    }

    public interface ListBeanModel extends TemplateModel {
        void setBeans(List<Bean> beans);

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        List<Bean> getBeans();
    }

    public interface ListInsideListBeanModel extends TemplateModel {
        void setBeans(List<List<Bean>> beans);

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        List<List<Bean>> getBeans();
    }

    public interface ListInsideListInsideList extends TemplateModel {
        void setBeans(List<List<List<Bean>>> beans);

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        List<List<List<Bean>>> getBeans();
    }

    public interface SubBeanIface {

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        String getValue();

        void setValue(String value);

        void setBean(SubSubBeanIface bean);

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        SubSubBeanIface getBean();

        void setBeanClass(SubBean bean);

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        SubBean getBeanClass();
    }

    public interface SubSubBeanIface {

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        int getValue();

        void setValue(int value);
    }

    public static class SuperBean {

        void setSubBean(SubSubBean bean) {

        }

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        public SubSubBean getSubBean() {
            return null;
        }
    }

    public static class SubBean extends SuperBean {

        private boolean visible;

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        public boolean isVisible() {
            return visible;
        }

        void setVisible(boolean visible) {
            this.visible = visible;
        }

        void setBean(SubSubBeanIface bean) {

        }

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        public SubSubBeanIface getBean() {
            return null;
        }
    }

    public static class SubSubBean {
        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        public Double getValue() {
            return null;
        }

        void setValue(Double value) {

        }
    }

    public interface SubBeansModel extends TemplateModel {
        void setBean(SubBeanIface bean);

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        SubBeanIface getBean();

        void setBeanClass(SubBean bean);

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        SubBean getBeanClass();
    }

    public static class SubSubBeanIfaceImpl implements SubSubBeanIface {
        private int value;

        @Override
        public void setValue(int value) {
            this.value = value;
        }

        @Override
        public int getValue() {
            return value;
        }
    }

    @Tag("div")
    public static class EmptyDivTemplate<M extends TemplateModel>
            extends PolymerTemplate<M> {
        public EmptyDivTemplate() {
            super((clazz, tag, service) -> new TemplateData("",
                    Jsoup.parse("<dom-module id='div'></dom-module>")));
        }

    }

    public static class TemplateWithExclude
            extends EmptyDivTemplate<TemplateWithExclude.ModelWithExclude> {

        public interface ModelWithExclude extends TemplateModel {
            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            @AllowClientUpdates(value = ClientUpdateMode.ALLOW, path = "booleanObject")
            @AllowClientUpdates(value = ClientUpdateMode.ALLOW, path = "intObject")
            @AllowClientUpdates(value = ClientUpdateMode.ALLOW, path = "intValue")
            @AllowClientUpdates(value = ClientUpdateMode.ALLOW, path = "doubleValue")
            @AllowClientUpdates(value = ClientUpdateMode.ALLOW, path = "booleanValue")
            Bean getBean();

            @Exclude({ "doubleValue", "booleanObject" })
            void setBean(Bean bean);
        }

        @Override
        public ModelWithExclude getModel() {
            return super.getModel();
        }

    }

    public static class TemplateWithExcludeAndInclude<M extends TemplateWithExcludeAndInclude.ModelWithExcludeAndInclude>
            extends EmptyDivTemplate<M> {

        public interface ModelWithExcludeAndInclude extends TemplateModel {
            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            @AllowClientUpdates(value = ClientUpdateMode.ALLOW, path = "booleanObject")
            Bean getBean();

            @Include({ "doubleValue", "booleanObject" })
            @Exclude("doubleValue")
            void setBean(Bean bean);
        }

        @Override
        protected M getModel() {
            return super.getModel();
        }

    }

    public static class TemplateWithExcludeAndIncludeImpl extends
            TemplateWithExcludeAndInclude<TemplateWithExcludeAndInclude.ModelWithExcludeAndInclude> {

    }

    public static class TemplateWithExcludeAndIncludeSubclassOverrides extends
            TemplateWithExcludeAndInclude<TemplateWithExcludeAndIncludeSubclassOverrides.ModelWithExcludeAndIncludeSubclass> {

        public interface ModelWithExcludeAndIncludeSubclass extends
                TemplateModelTest.TemplateWithExcludeAndInclude.ModelWithExcludeAndInclude {

            /*
             * Super class has annotations for this method to only include
             * 'booleanObject'. This tests that includes can be overridden in a
             * sub class (and parent class annotations are ignored).
             */
            @Override
            @Include("doubleValue")
            @AllowClientUpdates(value = ClientUpdateMode.ALLOW, path = "doubleValue")
            void setBean(Bean bean);
        }

    }

    public static class TemplateWithExcludeForSubBean extends
            EmptyDivTemplate<TemplateWithExcludeForSubBean.ModelWithExcludeForSubBean> {

        public interface ModelWithExcludeForSubBean extends TemplateModel {
            BeanContainingBeans getBeanContainingBeans();

            @Exclude({ "bean1.booleanObject", "bean2", "bean3" })
            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            @AllowClientUpdates(value = ClientUpdateMode.ALLOW, path = "bean1.intObject")
            @AllowClientUpdates(value = ClientUpdateMode.ALLOW, path = "bean1.intValue")
            @AllowClientUpdates(value = ClientUpdateMode.ALLOW, path = "bean1.booleanValue")
            @AllowClientUpdates(value = ClientUpdateMode.ALLOW, path = "bean1.doubleValue")
            void setBeanContainingBeans(
                    BeanContainingBeans beanContainingBeans);
        }

        @Override
        protected ModelWithExcludeForSubBean getModel() {
            return super.getModel();
        }

    }

    public static class TemplateWithExcludeOnList extends
            EmptyDivTemplate<TemplateWithExcludeOnList.ModelWithExcludeOnList> {

        public interface ModelWithExcludeOnList extends TemplateModel {
            @Exclude("intValue")
            void setBeans(List<Bean> beans);

            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            List<Bean> getBeans();
        }

        @Override
        protected ModelWithExcludeOnList getModel() {
            return super.getModel();
        }
    }

    public static class TemplateWithInclude
            extends EmptyDivTemplate<TemplateWithInclude.ModelWithInclude> {
        public interface ModelWithInclude extends TemplateModel {

            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            @AllowClientUpdates(value = ClientUpdateMode.ALLOW, path = "booleanObject")
            @AllowClientUpdates(value = ClientUpdateMode.ALLOW, path = "doubleValue")
            Bean getBean();

            @Include({ "doubleValue", "booleanObject" })
            void setBean(Bean bean);
        }

        @Override
        public ModelWithInclude getModel() {
            return super.getModel();
        }

    }

    public static class TemplateWithIncludeForSubBean extends
            EmptyDivTemplate<TemplateWithIncludeForSubBean.ModelWithIncludeForSubBean> {

        public interface ModelWithIncludeForSubBean extends TemplateModel {
            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            BeanContainingBeans getBeanContainingBeans();

            @Include({ "bean1.booleanObject" })
            void setBeanContainingBeans(
                    BeanContainingBeans beanContainingBeans);
        }

        @Override
        protected ModelWithIncludeForSubBean getModel() {
            return super.getModel();
        }
    }

    public static class TemplateWithIncludeOnList extends
            EmptyDivTemplate<TemplateWithIncludeOnList.ModelWithIncludeOnList> {

        public interface ModelWithIncludeOnList extends TemplateModel {
            @Include("intValue")
            void setBeans(List<Bean> beans);

            @AllowClientUpdates(ClientUpdateMode.ALLOW)
            List<Bean> getBeans();
        }

        @Override
        protected ModelWithIncludeOnList getModel() {
            return super.getModel();
        }
    }

    public static class NoModelTemplate<M extends TemplateModel>
            extends EmptyDivTemplate<M> {

    }

    public static class EmptyModelTemplate extends NoModelTemplate<EmptyModel> {

        @Override
        public EmptyModel getModel() {
            return super.getModel();
        }
    }

    public static class BasicTypeModelTemplate
            extends NoModelTemplate<BasicTypeModel> {
        @Override
        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        public BasicTypeModel getModel() {
            return super.getModel();
        };
    }

    public static class BeanModelTemplate extends NoModelTemplate<BeanModel> {
        @Override
        public BeanModel getModel() {
            return super.getModel();
        }
    }

    public static class NotSupportedModelTemplate
            extends NoModelTemplate<NotSupportedModel> {
        @Override
        public NotSupportedModel getModel() {
            return super.getModel();
        }
    }

    public static class SubBeansTemplate
            extends EmptyDivTemplate<SubBeansModel> {

        @Override
        protected SubBeansModel getModel() {
            return super.getModel();
        }
    }

    public static class ListBeanModelTemplate
            extends NoModelTemplate<ListBeanModel> {
        @Override
        public ListBeanModel getModel() {
            return super.getModel();
        }
    }

    public static class StringListModelTemplate
            extends NoModelTemplate<StringListModel> {
        @Override
        public StringListModel getModel() {
            return super.getModel();
        }
    }

    public interface StringListModel extends TemplateModel {
        void setItems(List<String> beans);

        @AllowClientUpdates(value = ClientUpdateMode.ALLOW)
        List<String> getItems();
    }

    public static class TemplateWithExcludeAndIncludeSubclass extends
            TemplateWithExcludeAndInclude<TemplateWithExcludeAndInclude.ModelWithExcludeAndInclude> {
        // Should work exactly the same way as the parent class
    }

    public static class TemplateWithIncludeOnListSubBean extends
            EmptyDivTemplate<TemplateWithIncludeOnListSubBean.ModelWithIncludeOnListSubBean> {

        public interface ModelWithIncludeOnListSubBean extends TemplateModel {
            @Include({ "bean1.intValue", "bean2.booleanValue" })
            void setBeanContainingBeans(List<BeanContainingBeans> beans);
        }

        @Override
        protected ModelWithIncludeOnListSubBean getModel() {
            return super.getModel();
        }

    }

    public static class BeanWithFinalAccessors implements TemplateModel {
        public final void setFoo(String foo) {

        }

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        public final String getBar() {
            return null;
        }

        void setName(String name) {

        }
    }

    public static class BeanWithInvalidSubBean implements TemplateModel {

        public BeanWithFinalAccessors getBean() {
            return null;
        }
    }

    public static class BeanWithExcludedInvalidAccessorsInSubBean
            implements TemplateModel {

        @Exclude({ "foo", "bar" })
        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        public BeanWithFinalAccessors getBean() {
            return null;
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
    void testTemplateModelCreation() {
        EmptyModelTemplate emptyModelTemplate = new EmptyModelTemplate();

        TemplateModel modelProxy = emptyModelTemplate.getModel();
        TemplateModel modelProxy2 = emptyModelTemplate.getModel();

        assertTrue(modelProxy == modelProxy2);

        modelProxy2 = new EmptyModelTemplate().getModel();
        assertNotSame(modelProxy, modelProxy2);
    }

    @Test
    void testSetterSameValue_noUpdates() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        // Initial populate properties model changes. Clear them out.
        template.getElement().getNode().clearChanges();

        model.setString("foobar");

        assertEquals("foobar", model.getString());

        List<NodeChange> changes = new ArrayList<>();
        ElementPropertyMap modelMap = template.getElement().getNode()
                .getFeature(ElementPropertyMap.class);
        modelMap.collectChanges(changes::add);

        assertEquals(1, changes.size());
        assertEquals(template.getElement().getNode(), changes.get(0).getNode());

        changes.clear();
        template.getElement().getNode().clearChanges();

        model.setString("foobar");

        assertEquals("foobar", model.getString());
        modelMap.collectChanges(changes::add);

        assertEquals(0, changes.size());
    }

    @Test
    void testBooleanValue() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        assertEquals(null, model.getBoolean());

        model.setBoolean(Boolean.TRUE);

        assertEquals(Boolean.TRUE, model.getBoolean());

        model.setBoolean(Boolean.FALSE);

        assertEquals(Boolean.FALSE, model.getBoolean());

        model.setBoolean(null);

        assertEquals(null, model.getBoolean());
    }

    @Test
    void testBooleanParseString() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        assertEquals(null, model.getBoolean());

        template.getElement().getNode().getFeature(ElementPropertyMap.class)
                .setProperty("boolean", "True");

        assertThrows(IllegalArgumentException.class, () -> model.getBoolean());
    }

    @Test
    void testBooleanPrimitiveParseString() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        assertEquals(Boolean.FALSE, model.isBooleanPrimitive());

        template.getElement().getNode().getFeature(ElementPropertyMap.class)
                .setProperty("booleanPrimitive", "TRUE");

        assertThrows(IllegalArgumentException.class,
                () -> model.getBooleanPrimitive());
    }

    @Test
    void testPrimitiveBooleanValue() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        assertFalse(model.getBooleanPrimitive());
        assertFalse(model.isBooleanPrimitive());

        model.setBooleanPrimitive(true);

        assertTrue(model.getBooleanPrimitive());
        assertTrue(model.isBooleanPrimitive());
    }

    @Test
    void testDoubleValue() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        assertEquals(null, model.getDouble());

        model.setDouble(Double.valueOf(1.0D));

        assertEquals(Double.valueOf(1.0D), model.getDouble());
    }

    @Test
    void testDoublePrimitiveValue() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        assertEquals(0.0d, model.getDoublePrimitive(), 0.0d);

        model.setDoublePrimitive(1.5d);

        assertEquals(1.5d, model.getDoublePrimitive(), 0.0d);
    }

    @Test
    void testIntegerValue() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        assertEquals(null, model.getInteger());

        model.setInteger(Integer.valueOf(10));

        assertEquals(Integer.valueOf(10), model.getInteger());
    }

    @Test
    void testIntValue() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        assertEquals(0, model.getInt());

        model.setInt(1000);

        assertEquals(1000, model.getInt());
    }

    @Test
    void testStringValue() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        assertEquals(null, model.getString());

        model.setString("foobar");

        assertEquals("foobar", model.getString());
    }

    @Test
    void testBeanInModel() {
        BeanModelTemplate template = new BeanModelTemplate();
        BeanModel model = template.getModel();

        AtomicInteger beanTriggered = new AtomicInteger();
        Bean bean = new BeanImpl(beanTriggered);
        bean.setString("foobar");

        StateNode stateNode = (StateNode) template.getElement().getNode()
                .getFeature(ElementPropertyMap.class).getProperty("bean");

        assertEquals(0, beanTriggered.get());

        model.setBean(bean);

        stateNode = (StateNode) template.getElement().getNode()
                .getFeature(ElementPropertyMap.class).getProperty("bean");

        // enough to verify that TemplateModelBeanUtil.importBeanIntoModel is
        // triggered, since TemplatemodelBeanUtilTests covers the bean import
        assertNotNull(stateNode);
        assertEquals(1, beanTriggered.get());

        ElementPropertyMap modelMap = ElementPropertyMap.getModel(stateNode);
        assertNotNull(modelMap);
        assertEquals("foobar", modelMap.getProperty("string"));
    }

    @Test
    void testUnsupportedPrimitiveSetter() {
        assertThrows(InvalidTemplateModelException.class,
                () -> new NotSupportedModelTemplate());
    }

    @Test
    void testUnsupportedPrimitiveGetter() {
        assertThrows(InvalidTemplateModelException.class,
                () -> new NotSupportedModelTemplate());
    }

    @Test
    void testGetterVoid() {
        assertThrows(InvalidTemplateModelException.class,
                () -> new NotSupportedModelTemplate());
    }

    @Test
    void testGetterWithParam() {
        assertThrows(InvalidTemplateModelException.class,
                () -> new NotSupportedModelTemplate());
    }

    @Test
    void testSetterReturns() {
        assertThrows(InvalidTemplateModelException.class,
                () -> new NotSupportedModelTemplate());
    }

    @Test
    void testSetterNoParam() {
        assertThrows(InvalidTemplateModelException.class,
                () -> new NotSupportedModelTemplate());
    }

    @Test
    void testSetterTwoParams() {
        assertThrows(InvalidTemplateModelException.class,
                () -> new NotSupportedModelTemplate());
    }

    @Test
    void testInvalidSetterMethodName() {
        assertThrows(InvalidTemplateModelException.class,
                () -> new NotSupportedModelTemplate());
    }

    @Test
    void testInvalidGetterMethodName() {
        assertThrows(InvalidTemplateModelException.class,
                () -> new NotSupportedModelTemplate());
    }

    @Test
    void getProxyInterface_getValueFromProxy_proxyIsNotNullAndProxyValueEqualsModelValue() {
        SubBeansTemplate template = new SubBeansTemplate();
        SubBeansModel model = template.getModel();
        SubBeanIface proxy = model.getProxy("bean", SubBeanIface.class);

        setModelPropertyAndVerifyGetter(template, () -> proxy.getValue(),
                "bean", "value", "foo");
    }

    @Test
    void getProxyInterface_getSubIfacePropertyValueFromProxy_proxyIsNotNullAndProxyValueEqualsModelValue() {
        SubBeansTemplate template = new SubBeansTemplate();
        SubBeansModel model = template.getModel();
        SubSubBeanIface proxy = model.getProxy("bean.bean",
                SubSubBeanIface.class);

        setModelPropertyAndVerifyGetter(template, () -> proxy.getValue(),
                "bean.bean", "value", 2);
    }

    @Test
    void getProxyInterface_getSubClassPropertyValueFromProxy_proxyIsNotNullAndProxyValueEqualsModelValue() {
        SubBeansTemplate template = new SubBeansTemplate();
        SubBeansModel model = template.getModel();
        SubBean proxy = model.getProxy("bean.beanClass", SubBean.class);

        setModelPropertyAndVerifyGetter(template, () -> proxy.isVisible(),
                "bean.beanClass", "visible", true);
    }

    @Test
    void getProxyClass_sameClasses() {
        SubBeansModel model1 = new SubBeansTemplate().getModel();
        model1.setBeanClass(new SubBean());

        SubBeansModel model2 = new SubBeansTemplate().getModel();
        model2.setBeanClass(new SubBean());

        assertSame(model1.getBeanClass().getClass(),
                model2.getBeanClass().getClass());
    }

    @Test
    void getProxyClass_getSubIfacePropertyValueFromProxy_proxyIsNotNullAndProxyValueEqualsModelValue() {
        SubBeansTemplate template = new SubBeansTemplate();
        SubBeansModel model = template.getModel();
        SubSubBeanIface proxy = model.getProxy("beanClass.bean",
                SubSubBeanIface.class);

        setModelPropertyAndVerifyGetter(template, () -> proxy.getValue(),
                "beanClass.bean", "value", 5);
    }

    @Test
    void getProxyInterface_setValueToProxy_proxyIsNotNullAndValueSetToModel() {
        SubBeansTemplate template = new SubBeansTemplate();
        SubBeansModel model = template.getModel();
        SubBeanIface proxy = model.getProxy("bean", SubBeanIface.class);
        proxy.setValue("foo");

        verifyModel(template, "bean", "value", "foo");
    }

    @Test
    void getProxyClass_getValueFromProxy_proxyIsNotNullAndProxyValueEqualsModelValue() {
        SubBeansTemplate template = new SubBeansTemplate();
        SubBeansModel model = template.getModel();
        SubBean proxy = model.getProxy("beanClass", SubBean.class);

        setModelPropertyAndVerifyGetter(template, () -> proxy.isVisible(),
                "beanClass", "visible", true);
    }

    @Test
    void getProxyClass_setValueToProxy_proxyIsNotNullAndValueSetToModel() {
        SubBeansTemplate template = new SubBeansTemplate();
        SubBeansModel model = template.getModel();
        SubBean proxy = model.getProxy("beanClass", SubBean.class);
        proxy.setVisible(true);

        verifyModel(template, "beanClass", "visible", true);
    }

    @Test
    void getProxyInterface_getSubIfacePropertyValueFromProxy_proxyIsNotNullAndValueSetToModel() {
        SubBeansTemplate template = new SubBeansTemplate();
        SubBeansModel model = template.getModel();
        SubSubBeanIface proxy = model.getProxy("bean.bean",
                SubSubBeanIface.class);
        proxy.setValue(3);

        SubBeanIface subBean = model.getProxy("bean", SubBeanIface.class);
        subBean.setValue("foo");

        verifyModel(template, "bean", "value", "foo");
        verifyModel(template, "bean.bean", "value", 3);
    }

    @Test
    void getProxyInterface_getSubClassPropertyValueFromProxy_proxyIsNotNullAndValueSetToModel() {
        SubBeansTemplate template = new SubBeansTemplate();
        SubBeansModel model = template.getModel();
        SubBean proxy = model.getProxy("bean.beanClass", SubBean.class);
        proxy.setVisible(true);

        SubBeanIface subBean = model.getProxy("bean", SubBeanIface.class);
        subBean.setValue("foo");

        verifyModel(template, "bean", "value", "foo");
        verifyModel(template, "bean.beanClass", "visible", true);
    }

    @Test
    void getProxyClass_getSubIfacePropertyValueFromProxy_proxyIsNotNullAndValueSetToModel() {
        SubBeansTemplate template = new SubBeansTemplate();
        SubBeansModel model = template.getModel();
        SubSubBeanIface proxy = model.getProxy("beanClass.bean",
                SubSubBeanIface.class);
        proxy.setValue(3);

        SubBean subBean = model.getProxy("beanClass", SubBean.class);
        subBean.setVisible(true);

        verifyModel(template, "beanClass", "visible", true);
        verifyModel(template, "beanClass.bean", "value", 3);
    }

    @Test
    void getProxyClass_getSubClassPropertyValueFromProxy_proxyIsNotNullAndValueSetToModel() {
        SubBeansTemplate template = new SubBeansTemplate();
        SubBeansModel model = template.getModel();
        SubSubBean proxy = model.getProxy("beanClass.subBean",
                SubSubBean.class);
        proxy.setValue(5d);

        SubBean subBean = model.getProxy("beanClass", SubBean.class);
        subBean.setVisible(true);

        verifyModel(template, "beanClass", "visible", true);
        verifyModel(template, "beanClass.subBean", "value", 5d);
    }

    @Test
    void getProxyInterface_setSubBeanIface_proxyIsCorrectAndValueSetToModel() {
        SubBeansTemplate template = new SubBeansTemplate();
        SubBeansModel model = template.getModel();
        SubBeanIface proxy = model.getProxy("bean", SubBeanIface.class);
        SubSubBeanIface subBean = new SubSubBeanIfaceImpl();
        subBean.setValue(4);
        proxy.setBean(subBean);

        SubSubBeanIface subProxy = model.getProxy("bean.bean",
                SubSubBeanIface.class);

        assertEquals(4, subProxy.getValue());
    }

    private void setModelPropertyAndVerifyGetter(PolymerTemplate<?> template,
            Supplier<Object> getter, String beanPath, String property,
            Serializable expected) {
        ElementPropertyMap feature = getModelMap(template, beanPath);
        feature.setProperty(property, expected);
        assertEquals(expected, getter.get());
    }

    private void verifyModel(PolymerTemplate<?> template, String beanPath,
            String property, Serializable expected) {
        ElementPropertyMap feature = getModelMap(template, beanPath);
        assertNotNull(feature);
        assertEquals(expected, feature.getProperty(property));
    }

    private ElementPropertyMap getModelMap(PolymerTemplate<?> template,
            String beanPath) {
        StateNode node = template.getElement().getNode();
        return ElementPropertyMap.getModel(node).resolveModelMap(beanPath);
    }

    private ModelList getModelList(PolymerTemplate<?> template,
            String beanPath) {
        StateNode node = template.getElement().getNode();
        return ElementPropertyMap.getModel(node).resolveModelList(beanPath);
    }

    @Test
    void getListFromModel() {
        ListBeanModelTemplate template = new ListBeanModelTemplate();
        List<Bean> beans = new ArrayList<>();
        beans.add(new Bean(100));
        beans.add(new Bean(200));
        beans.add(new Bean(300));
        template.getModel().setBeans(beans);
        assertListContentsEquals(template.getModel().getBeans(), new Bean(100),
                new Bean(200), new Bean(300));
    }

    @Test
    void stringListModel_handlesListOperationsProperly() {
        StringListModelTemplate template = new StringListModelTemplate();
        List<String> list = new ArrayList<>();
        list.add("foo");
        list.add("bar");
        list.add("foobar");
        template.getModel().setItems(list);
        assertListContentsEquals(template.getModel().getItems(), false,
                list.toArray(new String[list.size()]));

        template.getModel().getItems().add("barfoo");
        list = new ArrayList<>(list);
        list.add("barfoo");
        assertListContentsEquals(template.getModel().getItems(), false,
                list.toArray(new String[list.size()]));

        template.getModel().getItems().remove(0);
        list.remove(0);
        assertListContentsEquals(template.getModel().getItems(), false,
                list.toArray(new String[list.size()]));

        template.getModel().getItems().add(1, "abc");
        list.add(1, "abc");
        assertListContentsEquals(template.getModel().getItems(), false,
                list.toArray(new String[list.size()]));
    }

    @SafeVarargs
    private static <T> void assertListContentsEquals(List<T> list, T... beans) {
        assertListContentsEquals(list, true, beans);
    }

    @SafeVarargs
    private static <T> void assertListContentsEquals(List<T> list,
            boolean notSameInstances, T... beans) {
        assertEquals(beans.length, list.size());
        for (int i = 0; i < beans.length; i++) {
            MatcherAssert.assertThat(list.get(i),
                    Matchers.samePropertyValuesAs(beans[i]));
            if (notSameInstances) {
                assertNotSame(beans[i], list.get(i));
            }
        }

    }

    @Test
    void setBeanIncludeProperties() {
        TemplateWithInclude template = new TemplateWithInclude();
        template.getModel().setBean(new Bean(123));

        ElementPropertyMap modelMap = getModelMap(template, "bean");
        Set<String> mapKeys = getKeys(modelMap);
        assertTrue(mapKeys.remove("doubleValue"),
                "Model should contain included 'doubleValue'");
        assertTrue(mapKeys.remove("booleanObject"),
                "Model should contain included 'booleanObject'");
        assertTrue(mapKeys.isEmpty(),
                "model should be empty but contains: " + mapKeys);
    }

    @Test
    void setBeanExcludeProperties() {
        TemplateWithExclude template = new TemplateWithExclude();
        template.getModel().setBean(new Bean(123));

        ElementPropertyMap modelMap = getModelMap(template, "bean");
        Set<String> mapKeys = getKeys(modelMap);
        Set<String> excluded = new HashSet<>();
        excluded.add("doubleValue");
        excluded.add("booleanObject");

        for (String excludedPropertyName : excluded) {
            assertFalse(mapKeys.contains(excludedPropertyName),
                    "Model should not contain excluded '" + excludedPropertyName
                            + "'");
        }

        ReflectTools.getSetterMethods(Bean.class)
                .map(method -> ReflectTools.getPropertyName(method))
                .forEach(propertyName -> {
                    if (!excluded.contains(propertyName)) {
                        assertTrue(mapKeys.remove(propertyName),
                                "Model should contain the property '"
                                        + propertyName + "'");
                    }
                });
        assertTrue(mapKeys.isEmpty(),
                "model should be empty but contains: " + mapKeys);
    }

    @Test
    void setBeanIncludeAndExcludeProperties() {
        TemplateWithExcludeAndIncludeImpl template = new TemplateWithExcludeAndIncludeImpl();
        template.getModel().setBean(new Bean(123));
        ElementPropertyMap modelMap = getModelMap(template, "bean");
        assertTrue(modelMap.hasProperty("booleanObject"));
        assertEquals(1, modelMap.getPropertyNames().count());
    }

    @Test
    void includeExcludeWhenUsingSubclass() {
        TemplateWithExcludeAndIncludeSubclass template = new TemplateWithExcludeAndIncludeSubclass();
        template.getModel().setBean(new Bean(123));
        ElementPropertyMap modelMap = getModelMap(template, "bean");
        assertTrue(modelMap.hasProperty("booleanObject"));
        assertEquals(1, modelMap.getPropertyNames().count());
    }

    @Test
    void includeExcludeOverrideInSubclass() {
        TemplateWithExcludeAndIncludeSubclassOverrides template = new TemplateWithExcludeAndIncludeSubclassOverrides();
        template.getModel().setBean(new Bean(123));
        ElementPropertyMap modelMap = getModelMap(template, "bean");
        assertTrue(modelMap.hasProperty("doubleValue"));
        assertEquals(1, modelMap.getPropertyNames().count());
    }

    @Test
    void setBeanExcludeSubBeanProperties() {
        TemplateWithExcludeForSubBean template = new TemplateWithExcludeForSubBean();
        BeanContainingBeans beanContainer = new BeanContainingBeans();
        beanContainer.setBean1(new Bean(1));
        beanContainer.setBean2(new Bean(2));

        template.getModel().setBeanContainingBeans(beanContainer);

        assertNotNull(template.getModel().getBeanContainingBeans().getBean1());
        assertTrue(getModelMap(template, "beanContainingBeans.bean1")
                .hasProperty("booleanValue"));
        // bean1.booleanObject is excluded
        assertFalse(getModelMap(template, "beanContainingBeans.bean1")
                .hasProperty("booleanObject"));
    }

    @Test
    void setBeanExcludeSubBeanProperties_getterThrows() {
        TemplateWithExcludeForSubBean template = new TemplateWithExcludeForSubBean();
        BeanContainingBeans beanContainer = new BeanContainingBeans();
        beanContainer.setBean2(new Bean(2));

        template.getModel().setBeanContainingBeans(beanContainer);

        assertThrows(InvalidTemplateModelException.class,
                () -> template.getModel().getBeanContainingBeans().getBean2());
    }

    @Test
    void setBeanIncludeSubBeanProperties() {
        TemplateWithIncludeForSubBean template = new TemplateWithIncludeForSubBean();
        BeanContainingBeans beanContainer = new BeanContainingBeans();
        beanContainer.setBean1(new Bean(1));
        beanContainer.setBean2(new Bean(2));
        template.getModel().setBeanContainingBeans(beanContainer);

        assertNotNull(template.getModel().getBeanContainingBeans().getBean1());

        ElementPropertyMap bean1Map = getModelMap(template,
                "beanContainingBeans.bean1");
        Set<String> bean1Keys = getKeys(bean1Map);
        assertTrue(bean1Keys.contains("booleanObject"));
        assertEquals(1, bean1Keys.size());
    }

    @Test
    void setBeanIncludeSubBeanProperties_getterThrows() {
        TemplateWithIncludeForSubBean template = new TemplateWithIncludeForSubBean();
        BeanContainingBeans beanContainer = new BeanContainingBeans();
        beanContainer.setBean2(new Bean(2));
        template.getModel().setBeanContainingBeans(beanContainer);

        assertThrows(InvalidTemplateModelException.class,
                () -> template.getModel().getBeanContainingBeans().getBean2());
    }

    @Test
    void setListWithInclude() {
        TemplateWithIncludeOnList template = new TemplateWithIncludeOnList();
        List<Bean> beans = new ArrayList<>();
        beans.add(new Bean(1));
        beans.add(new Bean(2));
        template.getModel().setBeans(beans);

        ModelList modelList = getModelList(template, "beans");
        ElementPropertyMap bean1 = ElementPropertyMap
                .getModel(modelList.get(0));
        Set<String> propertiesInMap = bean1.getPropertyNames()
                .collect(Collectors.toSet());
        assertTrue(propertiesInMap.remove("intValue"),
                "Bean in model should have an 'intValue' property");
        assertEquals(0, propertiesInMap.size(),
                "All other properties should have been filtered out");
    }

    @Test
    void setListWithExclude() {
        TemplateWithExcludeOnList template = new TemplateWithExcludeOnList();
        List<Bean> beans = new ArrayList<>();
        beans.add(new Bean(1));
        beans.add(new Bean(2));
        template.getModel().setBeans(beans);

        ModelList modelList = getModelList(template, "beans");
        ElementPropertyMap bean1 = ElementPropertyMap
                .getModel(modelList.get(0));
        ElementPropertyMap bean2 = ElementPropertyMap
                .getModel(modelList.get(1));

        Set<String> bean1InMap = getKeys(bean1);
        Set<String> bean2InMap = getKeys(bean2);
        assertFalse(bean1InMap.contains("intValue"),
                "Bean1 in model should not have an 'intValue' property");
        assertFalse(bean2InMap.contains("intValue"),
                "Bean2 in model should not have an 'intValue' property");
        assertEquals(6, bean1InMap.size(),
                "All other properties should have been included");
        assertEquals(6, bean2InMap.size(),
                "All other properties should have been included");
    }

    @Test
    void setListWithSubBeanInclude() {
        TemplateWithIncludeOnListSubBean template = new TemplateWithIncludeOnListSubBean();
        List<BeanContainingBeans> beanContainingBeans = new ArrayList<>();
        beanContainingBeans
                .add(new BeanContainingBeans(new Bean(11), new Bean(12)));
        beanContainingBeans.add(new BeanContainingBeans(null, new Bean(22)));
        template.getModel().setBeanContainingBeans(beanContainingBeans);

        ModelList modelList = getModelList(template, "beanContainingBeans");
        assertEquals(2, modelList.size());

        ElementPropertyMap container1Map = ElementPropertyMap
                .getModel(modelList.get(0));
        ElementPropertyMap container2Map = ElementPropertyMap
                .getModel(modelList.get(1));
        Set<String> bean1bean2 = new HashSet<>();
        bean1bean2.add("bean1");
        bean1bean2.add("bean2");
        assertEquals(bean1bean2,
                container1Map.getPropertyNames().collect(Collectors.toSet()));
        assertEquals(bean1bean2,
                container2Map.getPropertyNames().collect(Collectors.toSet()));

        Set<String> container1Bean1Properties = getKeys(
                container1Map.resolveModelMap("bean1"));
        assertTrue(container1Bean1Properties.remove("intValue"));
        assertEquals(0, container1Bean1Properties.size());

        Set<String> container1Bean2Properties = getKeys(
                container1Map.resolveModelMap("bean2"));
        assertTrue(container1Bean2Properties.remove("booleanValue"));
        assertEquals(0, container1Bean2Properties.size());

        Set<String> container2Bean1Properties = getKeys(
                container2Map.resolveModelMap("bean1"));
        // Null value in the initial bean implies not imported or created
        assertEquals(0, container2Bean1Properties.size());

        Set<String> container2Bean2Properties = getKeys(
                container2Map.resolveModelMap("bean2"));
        assertTrue(container2Bean2Properties.remove("booleanValue"));
        assertEquals(0, container2Bean2Properties.size());
    }

    @Test
    void beanModelType_emptyBeanAsInitialValue() {
        BeanModelTemplate template = new BeanModelTemplate();

        // Check that even before calling any model method the properties are
        // available via features
        Serializable bean = template.getElement().getNode()
                .getFeature(ElementPropertyMap.class).getProperty("bean");

        assertNotNull(bean);
        StateNode node = (StateNode) bean;
        assertEquals(0, node.getFeature(ElementPropertyMap.class)
                .getProperty("intValue"));

        // Now check properties via API
        assertNotNull(template.getModel().getBean());

        assertEquals(0, template.getModel().getBean().getIntValue());
    }

    @Test
    void beanModelType_emptySubBeanAsInitialValue() {
        SubBeansTemplate template = new SubBeansTemplate();

        // Check that even before calling any model method the properties are
        // available via features
        Serializable bean = template.getElement().getNode()
                .getFeature(ElementPropertyMap.class).getProperty("bean");

        StateNode node = (StateNode) bean;
        Serializable subBean = node.getFeature(ElementPropertyMap.class)
                .getProperty("bean");
        assertTrue(subBean instanceof StateNode);

        // Now check properties via API
        assertNotNull(template.getModel().getBean().getBean());
    }

    @Test
    void beanModelType_setNullAsValue() {
        SubBeansTemplate template = new SubBeansTemplate();

        assertNotNull(template.getModel().getBean());
        template.getModel().setBean(null);
        assertNull(template.getModel().getBean());
    }

    @Test
    void listModelType_emptyListAsInitialValue() {
        ListBeanModelTemplate template = new ListBeanModelTemplate();

        // Check that even before calling any model method the properties are
        // available via features
        Serializable bean = template.getElement().getNode()
                .getFeature(ElementPropertyMap.class).getProperty("beans");
        assertNotNull(bean);

        StateNode node = (StateNode) bean;
        assertTrue(node.hasFeature(ModelList.class));

        // Now check properties via API
        List<Bean> list = template.getModel().getBeans();
        assertNotNull(list);
        assertEquals(0, list.size());
    }

    @Test
    void basicModelType_defaultValues() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        assertNull(template.getModel().getBoolean());
        assertFalse(template.getModel().getBooleanPrimitive());
        assertNull(template.getModel().getDouble());
        assertEquals(String.valueOf(0.0d),
                String.valueOf(template.getModel().getDoublePrimitive()));
        assertEquals(0, template.getModel().getInt());
        assertNull(template.getModel().getInteger());
        assertNull(template.getModel().getString());
    }

    @Test
    void modelHasFinalAccessors_throws() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new EmptyDivTemplate<BeanWithFinalAccessors>() {
                });
        String message = ex.getMessage();
        assertTrue(message.contains(BeanWithFinalAccessors.class.getName()));
        assertTrue(
                message.contains("property 'bar' has final getter 'getBar'"));
        assertTrue(
                message.contains("property 'foo' has final setter 'setFoo'"));
        assertTrue(message.contains("@" + Exclude.class.getSimpleName()));
        assertTrue(message.contains("@" + Include.class.getSimpleName()));
    }

    @Test
    void modelHasSubBeanWithFinalAccessors_throws() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new EmptyDivTemplate<BeanWithInvalidSubBean>() {
                });
        String message = ex.getMessage();
        assertTrue(message.contains(BeanWithFinalAccessors.class.getName()));
        assertTrue(
                message.contains("property 'bar' has final getter 'getBar'"));
        assertTrue(
                message.contains("property 'foo' has final setter 'setFoo'"));
        assertTrue(message.contains("@" + Exclude.class.getSimpleName()));
        assertTrue(message.contains("@" + Include.class.getSimpleName()));
    }

    @Test
    void modelHasSubBeanWithExcludedFinalAccessors_modelIsCreated() {
        new EmptyDivTemplate<BeanWithExcludedInvalidAccessorsInSubBean>() {

        };
    }

    @Test
    void emptyModelListShouldBeRepopulatedAfterDetach() {
        TemplateWithIncludeOnList template = new TemplateWithIncludeOnList();
        assertTrue(template.getModel().getBeans().isEmpty(),
                "Template should have its list empty");
        NodeList<?> nodeList = getModelList(template, "beans");

        List<NodeChange> changesAfterAttach = collectChanges(nodeList);
        assertEquals(1, changesAfterAttach.size(),
                "Expect empty model list to create a single change after attach");

        assertTrue(collectChanges(nodeList).isEmpty(),
                "After the empty model list is attached and created a change, no more changes are created");

        nodeList.onDetach();
        List<NodeChange> changesAfterDetach = collectChanges(nodeList);
        assertEquals(1, changesAfterDetach.size(),
                "Expect empty model list to create a single change after detach");

        assertTrue(
                changesAfterDetach.get(0).toJson(null)
                        .equals(changesAfterAttach.get(0).toJson(null)),
                "Changes to empty list after attach and detach should be the same");

        assertTrue(collectChanges(nodeList).isEmpty(),
                "After the empty model list is detached and created a change, no more changes are created");
    }

    private List<NodeChange> collectChanges(NodeList<?> nodeList) {
        List<NodeChange> changes = new ArrayList<>();
        nodeList.collectChanges(changes::add);
        return changes;
    }

    private static Set<String> getKeys(ElementPropertyMap map) {
        return map.getPropertyNames().collect(Collectors.toSet());
    }

}
