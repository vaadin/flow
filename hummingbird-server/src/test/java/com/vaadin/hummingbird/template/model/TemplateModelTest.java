package com.vaadin.hummingbird.template.model;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.change.NodeChange;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.hummingbird.template.InlineTemplate;
import com.vaadin.ui.Template;

public class TemplateModelTest {

    public interface EmptyModel extends TemplateModel {

    }

    public interface BasicTypeModel extends TemplateModel {
        boolean getBooleanPrimitive();

        boolean isBooleanPrimitive();

        void setBooleanPrimitive(boolean b);

        Boolean getBoolean();

        void setBoolean(Boolean b);

        int getInt();

        void setInt(int i);

        Integer getInteger();

        void setInteger(Integer i);

        double getDoublePrimitive();

        void setDoublePrimitive(double d);

        Double getDouble();

        void setDouble(Double d);

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

    public interface BeanModel extends TemplateModel {
        void setBean(Bean bean);
    }

    public interface ListBeanModel extends TemplateModel {
        void setBeans(List<Bean> beans);

        List<Bean> getBeans();
    }

    public interface SubBeanIface {

        String getValue();

        void setValue(String value);

        void setBean(SubSubBeanIface bean);

        SubSubBeanIface getBean();

        void setBeanClass(SubBean bean);

        SubBean getBeanClass();
    }

    public interface SubSubBeanIface {

        int getValue();

        void setValue(int value);
    }

    public static class SuperBean {

        public void setSubBean(SubSubBean bean) {

        }

        public SubSubBean getSubBean() {
            return null;
        }
    }

    public static class SubBean extends SuperBean {

        private boolean visible;

        public boolean isVisible() {
            return visible;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public void setBean(SubSubBeanIface bean) {

        }

        public SubSubBeanIface getBean() {
            return null;
        }
    }

    public static class SubSubBean {
        public Double getValue() {
            return null;
        }

        public void setValue(Double value) {

        }
    }

    public interface SubBeansModel extends TemplateModel {
        void setBean(SubBeanIface bean);

        SubBeanIface getBean();

        void setBeanClass(SubBean bean);

        SubBean getBeanClass();
    }

    public static class SubBeansTemplate extends InlineTemplate {
        public SubBeansTemplate() {
            super("<div></div>");
        }

        @Override
        protected SubBeansModel getModel() {
            return (SubBeansModel) super.getModel();
        }
    }

    public static class NoModelTemplate extends Template {
        public NoModelTemplate() {
            super(new ByteArrayInputStream(
                    "<div>foo</div>".getBytes(StandardCharsets.UTF_8)));
        }

        @Override
        public TemplateModel getModel() {
            return super.getModel();
        }
    }

    static class EmptyModelTemplate extends NoModelTemplate {
        @Override
        public EmptyModel getModel() {
            return (EmptyModel) super.getModel();
        }
    }

    static class BasicTypeModelTemplate extends NoModelTemplate {
        @Override
        public BasicTypeModel getModel() {
            return (BasicTypeModel) super.getModel();
        };
    }

    static class NotSupportedModelTemplate extends NoModelTemplate {
        @Override
        public NotSupportedModel getModel() {
            return (NotSupportedModel) super.getModel();
        }
    }

    static class BeanModelTemplate extends NoModelTemplate {
        @Override
        public BeanModel getModel() {
            return (BeanModel) super.getModel();
        }
    }

    static class ListBeanModelTemplate extends NoModelTemplate {
        @Override
        public ListBeanModel getModel() {
            return (ListBeanModel) super.getModel();
        }
    }

    public static interface ModelWithList extends TemplateModel {
        List<Bean> getBeans();

        void setBeans(List<Bean> beans);

        List<String> getItems();

        void setItems(List<String> items);
    }

    public static class TemplateWithList extends NoModelTemplate {

        @Override
        public ModelWithList getModel() {
            return (ModelWithList) super.getModel();
        }
    }

    @Test
    public void testTemplateModelTypeReading() {
        Class<? extends TemplateModel> templateModelType = TemplateModelTypeParser
                .getType(NoModelTemplate.class);

        Assert.assertEquals(TemplateModel.class, templateModelType);

        templateModelType = TemplateModelTypeParser
                .getType(EmptyModelTemplate.class);

        Assert.assertEquals(EmptyModel.class, templateModelType);
    }

    @Test
    public void testTemplateModelTypeCache() {
        TemplateModelTypeParser.cache.clear();

        Class<? extends TemplateModel> first = TemplateModelTypeParser
                .getType(EmptyModelTemplate.class);
        Class<? extends TemplateModel> second = TemplateModelTypeParser
                .getType(EmptyModelTemplate.class);

        Assert.assertSame(first, second);
    }

    @Test
    public void testTemplateModelCreation() {
        EmptyModelTemplate emptyModelTemplate = new EmptyModelTemplate();

        TemplateModel modelProxy = emptyModelTemplate.getModel();
        TemplateModel modelProxy2 = emptyModelTemplate.getModel();

        Assert.assertTrue(modelProxy == modelProxy2);

        modelProxy2 = new EmptyModelTemplate().getModel();
        Assert.assertNotSame(modelProxy, modelProxy2);
    }

    @Test
    public void testSetterSameValue_noUpdates() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        // Initial populate properties model changes. Clear them out.
        template.getElement().getNode().clearChanges();

        model.setString("foobar");

        Assert.assertEquals("foobar", model.getString());

        ArrayList<NodeChange> changes = new ArrayList<>();
        ModelMap modelMap = template.getElement().getNode()
                .getFeature(ModelMap.class);
        modelMap.collectChanges(changes::add);

        Assert.assertEquals(1, changes.size());
        Assert.assertEquals(template.getElement().getNode(),
                changes.get(0).getNode());

        changes.clear();
        template.getElement().getNode().clearChanges();

        model.setString("foobar");

        Assert.assertEquals("foobar", model.getString());
        modelMap.collectChanges(changes::add);

        Assert.assertEquals(0, changes.size());
    }

    @Test
    public void testBooleanValue() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        Assert.assertEquals(null, model.getBoolean());

        model.setBoolean(Boolean.TRUE);

        Assert.assertEquals(Boolean.TRUE, model.getBoolean());

        model.setBoolean(Boolean.FALSE);

        Assert.assertEquals(Boolean.FALSE, model.getBoolean());

        model.setBoolean(null);

        Assert.assertEquals(null, model.getBoolean());
    }

    @Test(expected = ClassCastException.class)
    public void testBooleanParseString() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        Assert.assertEquals(null, model.getBoolean());

        template.getElement().getNode().getFeature(ModelMap.class)
                .setValue("boolean", "True");

        model.getBoolean();
    }

    @Test(expected = ClassCastException.class)
    public void testBooleanPrimitiveParseString() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        Assert.assertEquals(Boolean.FALSE, model.isBooleanPrimitive());

        template.getElement().getNode().getFeature(ModelMap.class)
                .setValue("booleanPrimitive", "TRUE");

        model.getBooleanPrimitive();
    }

    @Test
    public void testPrimitiveBooleanValue() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        Assert.assertFalse(model.getBooleanPrimitive());
        Assert.assertFalse(model.isBooleanPrimitive());

        model.setBooleanPrimitive(true);

        Assert.assertTrue(model.getBooleanPrimitive());
        Assert.assertTrue(model.isBooleanPrimitive());
    }

    @Test
    public void testDoubleValue() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        Assert.assertEquals(null, model.getDouble());

        model.setDouble(new Double(1.0D));

        Assert.assertEquals(new Double(1.0D), model.getDouble());
    }

    @Test
    public void testDoublePrimitiveValue() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        Assert.assertEquals(0.0d, model.getDoublePrimitive(), 0.0d);

        model.setDoublePrimitive(1.5d);

        Assert.assertEquals(1.5d, model.getDoublePrimitive(), 0.0d);
    }

    @Test
    public void testIntegerValue() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        Assert.assertEquals(null, model.getInteger());

        model.setInteger(new Integer(10));

        Assert.assertEquals(new Integer(10), model.getInteger());
    }

    @Test
    public void testIntValue() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        Assert.assertEquals(0, model.getInt());

        model.setInt(1000);

        Assert.assertEquals(1000, model.getInt());
    }

    @Test
    public void testStringValue() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        Assert.assertEquals(null, model.getString());

        model.setString("foobar");

        Assert.assertEquals("foobar", model.getString());
    }

    @Test
    public void testBeanInModel() {
        BeanModelTemplate template = new BeanModelTemplate();
        BeanModel model = template.getModel();

        AtomicInteger beanTriggered = new AtomicInteger();
        Bean bean = new Bean() {
            @Override
            public String getString() {
                beanTriggered.incrementAndGet();
                return super.getString();
            }
        };
        bean.setString("foobar");

        StateNode stateNode = (StateNode) template.getElement().getNode()
                .getFeature(ModelMap.class).getValue("bean");

        Assert.assertNull(stateNode);
        Assert.assertEquals(0, beanTriggered.get());

        model.setBean(bean);

        stateNode = (StateNode) template.getElement().getNode()
                .getFeature(ModelMap.class).getValue("bean");

        // enough to verify that TemplateModelBeanUtil.importBeanIntoModel is
        // triggered, since TemplatemodelBeanUtilTests covers the bean import
        Assert.assertNotNull(stateNode);
        Assert.assertEquals(1, beanTriggered.get());

        ModelMap modelMap = stateNode.getFeature(ModelMap.class);
        Assert.assertNotNull(modelMap);
        Assert.assertEquals("foobar", modelMap.getValue("string"));
    }

    @Test(expected = InvalidTemplateModelException.class)
    public void testUnsupportedPrimitiveSetter() {
        NotSupportedModelTemplate template = new NotSupportedModelTemplate();
        NotSupportedModel model = template.getModel();

        model.setLong(0L);
    }

    @Test(expected = InvalidTemplateModelException.class)
    public void testUnsupportedPrimitiveGetter() {
        NotSupportedModelTemplate template = new NotSupportedModelTemplate();
        NotSupportedModel model = template.getModel();

        model.getLong();
    }

    @Test(expected = InvalidTemplateModelException.class)
    public void testGetterVoid() {
        NotSupportedModelTemplate template = new NotSupportedModelTemplate();
        NotSupportedModel model = template.getModel();

        model.getFoo();
    }

    @Test(expected = InvalidTemplateModelException.class)
    public void testGetterWithParam() {
        NotSupportedModelTemplate template = new NotSupportedModelTemplate();
        NotSupportedModel model = template.getModel();

        model.getFoo(0);
    }

    @Test(expected = InvalidTemplateModelException.class)
    public void testSetterReturns() {
        NotSupportedModelTemplate template = new NotSupportedModelTemplate();
        NotSupportedModel model = template.getModel();

        model.setFoo(0);
    }

    @Test(expected = InvalidTemplateModelException.class)
    public void testSetterNoParam() {
        NotSupportedModelTemplate template = new NotSupportedModelTemplate();
        NotSupportedModel model = template.getModel();

        model.setFoo();
    }

    @Test(expected = InvalidTemplateModelException.class)
    public void testSetterTwoParams() {
        NotSupportedModelTemplate template = new NotSupportedModelTemplate();
        NotSupportedModel model = template.getModel();

        model.setFoo(1, 2);
    }

    @Test(expected = InvalidTemplateModelException.class)
    public void testInvalidSetterMethodName() {
        NotSupportedModelTemplate template = new NotSupportedModelTemplate();
        NotSupportedModel model = template.getModel();

        model.setfoo(1);
    }

    @Test(expected = InvalidTemplateModelException.class)
    public void testInvalidGetterMethodName() {
        NotSupportedModelTemplate template = new NotSupportedModelTemplate();
        NotSupportedModel model = template.getModel();

        model.isbar();
    }

    @Test
    public void getProxyInterface_getValueFromProxy_proxyIsNotNullAndProxyValueEqualsModelValue() {
        SubBeansTemplate template = new SubBeansTemplate();
        SubBeansModel model = template.getModel();
        SubBeanIface proxy = model.getProxy("bean", SubBeanIface.class);

        setModelPropertyAndVerifyGetter(template, () -> proxy.getValue(),
                "bean", "value", "foo");
    }

    @Test
    public void getProxyInterface_getSubIfacePropertyValueFromProxy_proxyIsNotNullAndProxyValueEqualsModelValue() {
        SubBeansTemplate template = new SubBeansTemplate();
        SubBeansModel model = template.getModel();
        SubSubBeanIface proxy = model.getProxy("bean.bean",
                SubSubBeanIface.class);

        setModelPropertyAndVerifyGetter(template, () -> proxy.getValue(),
                "bean.bean", "value", 2);
    }

    @Test
    public void getProxyInterface_getSubClassPropertyValueFromProxy_proxyIsNotNullAndProxyValueEqualsModelValue() {
        SubBeansTemplate template = new SubBeansTemplate();
        SubBeansModel model = template.getModel();
        SubBean proxy = model.getProxy("bean.beanClass", SubBean.class);

        setModelPropertyAndVerifyGetter(template, () -> proxy.isVisible(),
                "bean.beanClass", "visible", true);
    }

    @Test
    public void getProxyClass_sameClasses() {
        SubBeansModel model1 = new SubBeansTemplate().getModel();
        model1.setBeanClass(new SubBean());

        SubBeansModel model2 = new SubBeansTemplate().getModel();
        model2.setBeanClass(new SubBean());

        Assert.assertSame(model1.getBeanClass().getClass(),
                model2.getBeanClass().getClass());
    }

    @Test
    public void getProxyClass_getSubIfacePropertyValueFromProxy_proxyIsNotNullAndProxyValueEqualsModelValue() {
        SubBeansTemplate template = new SubBeansTemplate();
        SubBeansModel model = template.getModel();
        SubSubBeanIface proxy = model.getProxy("beanClass.bean",
                SubSubBeanIface.class);

        setModelPropertyAndVerifyGetter(template, () -> proxy.getValue(),
                "beanClass.bean", "value", 5);
    }

    @Test
    public void getProxyInterface_setValueToProxy_proxyIsNotNullAndValueSetToModel() {
        SubBeansTemplate template = new SubBeansTemplate();
        SubBeansModel model = template.getModel();
        SubBeanIface proxy = model.getProxy("bean", SubBeanIface.class);
        proxy.setValue("foo");

        verifyModel(template, "bean", "value", "foo");
    }

    @Test
    public void getProxyClass_getValueFromProxy_proxyIsNotNullAndProxyValueEqualsModelValue() {
        SubBeansTemplate template = new SubBeansTemplate();
        SubBeansModel model = template.getModel();
        SubBean proxy = model.getProxy("beanClass", SubBean.class);

        setModelPropertyAndVerifyGetter(template, () -> proxy.isVisible(),
                "beanClass", "visible", true);
    }

    @Test
    public void getProxyClass_setValueToProxy_proxyIsNotNullAndValueSetToModel() {
        SubBeansTemplate template = new SubBeansTemplate();
        SubBeansModel model = template.getModel();
        SubBean proxy = model.getProxy("beanClass", SubBean.class);
        proxy.setVisible(true);

        verifyModel(template, "beanClass", "visible", true);
    }

    @Test
    public void getProxyInterface_getSubIfacePropertyValueFromProxy_proxyIsNotNullAndValueSetToModel() {
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
    public void getProxyInterface_getSubClassPropertyValueFromProxy_proxyIsNotNullAndValueSetToModel() {
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
    public void getProxyClass_getSubIfacePropertyValueFromProxy_proxyIsNotNullAndValueSetToModel() {
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
    public void getProxyClass_getSubClassPropertyValueFromProxy_proxyIsNotNullAndValueSetToModel() {
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
    public void getProxyInterface_setSubBeanIface_proxyIsCorrectAndValueSetToModel() {
        SubBeansTemplate template = new SubBeansTemplate();
        SubBeansModel model = template.getModel();
        SubBeanIface proxy = model.getProxy("bean", SubBeanIface.class);
        SubSubBeanIface subBean = new SubSubBeanIface() {

            private int value;

            @Override
            public void setValue(int value) {
                this.value = value;
            }

            @Override
            public int getValue() {
                return value;
            }
        };
        subBean.setValue(4);
        proxy.setBean(subBean);

        SubSubBeanIface subProxy = model.getProxy("bean.bean",
                SubSubBeanIface.class);

        Assert.assertEquals(4, subProxy.getValue());
    }

    @Test
    public void modelMapContainsModelProperties() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();

        // create model (populate properties)
        template.getModel();

        ModelMap model = template.getElement().getNode()
                .getFeature(ModelMap.class);

        Assert.assertTrue(model.hasValue("booleanPrimitive"));
        Assert.assertTrue(model.hasValue("boolean"));
        Assert.assertTrue(model.hasValue("int"));
        Assert.assertTrue(model.hasValue("integer"));
        Assert.assertTrue(model.hasValue("doublePrimitive"));
        Assert.assertTrue(model.hasValue("double"));
        Assert.assertTrue(model.hasValue("string"));
    }

    @Test
    public void notSupportedModelMapHasNoProperties() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();

        // create model (populate properties)
        template.getModel();

        ModelMap model = template.getElement().getNode()
                .getFeature(ModelMap.class);

        Assert.assertFalse(model.hasValue("long"));
        Assert.assertFalse(model.hasValue("foo"));
        Assert.assertFalse(model.hasValue("bar"));
    }

    @Test
    public void modelMapContainsBeanProperty() {
        BeanModelTemplate template = new BeanModelTemplate();

        // create model (populate properties)
        template.getModel();

        ModelMap model = template.getElement().getNode()
                .getFeature(ModelMap.class);

        Assert.assertTrue(model.hasValue("bean"));
    }

    @Test
    public void templateWithListProperty_modelMapContainsListProperty() {
        TemplateWithList template = new TemplateWithList();

        // create model (populate properties)
        template.getModel();

        ModelMap model = template.getElement().getNode()
                .getFeature(ModelMap.class);

        Assert.assertTrue(model.hasValue("beans"));
        Assert.assertFalse(model.hasValue("items"));
    }

    private void setModelPropertyAndVerifyGetter(Template template,
            Supplier<Object> getter, String beanPath, String property,
            Serializable expected) {
        ModelMap feature = getModelMap(template, beanPath);
        feature.setValue(property, expected);
        Assert.assertEquals(expected, getter.get());
    }

    private void verifyModel(Template template, String beanPath,
            String property, Serializable expected) {
        ModelMap feature = getModelMap(template, beanPath);
        Assert.assertNotNull(feature);
        Assert.assertEquals(expected, feature.getValue(property));
    }

    private ModelMap getModelMap(Template template, String beanPath) {
        StateNode node = template.getElement().getNode();
        String[] path = beanPath.split("\\.");
        for (int i = 0; i < path.length; i++) {
            Serializable bean = node.getFeature(ModelMap.class)
                    .getValue(path[i]);
            Assert.assertNotNull(bean);
            Assert.assertTrue(bean instanceof StateNode);
            node = (StateNode) bean;
        }
        ModelMap feature = node.getFeature(ModelMap.class);
        return feature;
    }

    @Test
    public void getListFromModel() {
        ListBeanModelTemplate template = new ListBeanModelTemplate();
        ArrayList<Bean> beans = new ArrayList<>();
        beans.add(new Bean(100));
        beans.add(new Bean(200));
        beans.add(new Bean(300));
        template.getModel().setBeans(beans);
        TemplateModelUtilTest.assertListContentsEquals(
                template.getModel().getBeans(), new Bean(100), new Bean(200),
                new Bean(300));
    }
}
