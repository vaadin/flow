package com.vaadin.flow.template.angular.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.change.NodeChange;
import com.vaadin.flow.internal.nodefeature.ModelList;
import com.vaadin.flow.internal.nodefeature.ModelMap;
import com.vaadin.flow.template.angular.AngularTemplate;
import com.vaadin.flow.template.angular.InlineTemplate;
import com.vaadin.flow.template.angular.model.TemplateModel;
import com.vaadin.flow.template.angular.model.TemplateModelTypeParser;
import com.vaadin.flow.templatemodel.Bean;
import com.vaadin.flow.templatemodel.BeanContainingBeans;
import com.vaadin.flow.templatemodel.Exclude;
import com.vaadin.flow.templatemodel.Include;
import com.vaadin.flow.templatemodel.InvalidTemplateModelException;

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
        void setBean(Bean bean);
    }

    public interface ListBeanModel extends TemplateModel {
        void setBeans(List<Bean> beans);

        List<Bean> getBeans();
    }

    public interface ListInsideListBeanModel extends TemplateModel {
        void setBeans(List<List<Bean>> beans);

        List<List<Bean>> getBeans();
    }

    public interface ListInsideListInsideList extends TemplateModel {
        void setBeans(List<List<List<Bean>>> beans);

        List<List<List<Bean>>> getBeans();
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

    public static class EmptyDivTemplate extends InlineTemplate {
        public EmptyDivTemplate() {
            super("<div></div>");
        }

    }

    public static class SubBeansTemplate extends EmptyDivTemplate {

        @Override
        protected SubBeansModel getModel() {
            return (SubBeansModel) super.getModel();
        }
    }

    public static class NoModelTemplate extends EmptyDivTemplate {

        @Override
        public TemplateModel getModel() {
            return super.getModel();
        }
    }

    public static class EmptyModelTemplate extends NoModelTemplate {
        @Override
        public EmptyModel getModel() {
            return (EmptyModel) super.getModel();
        }
    }

    public static class BasicTypeModelTemplate extends NoModelTemplate {
        @Override
        public BasicTypeModel getModel() {
            return (BasicTypeModel) super.getModel();
        };
    }

    public static class NotSupportedModelTemplate extends NoModelTemplate {
        @Override
        public NotSupportedModel getModel() {
            return (NotSupportedModel) super.getModel();
        }
    }

    public static class BeanModelTemplate extends NoModelTemplate {
        @Override
        public BeanModel getModel() {
            return (BeanModel) super.getModel();
        }
    }

    public static class ListBeanModelTemplate extends NoModelTemplate {
        @Override
        public ListBeanModel getModel() {
            return (ListBeanModel) super.getModel();
        }
    }

    public interface ModelWithList extends TemplateModel {
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

    public static class TemplateWithInclude extends EmptyDivTemplate {
        public interface ModelWithInclude extends TemplateModel {
            Bean getBean();

            @Include({ "doubleValue", "booleanObject" })
            void setBean(Bean bean);
        }

        @Override
        protected ModelWithInclude getModel() {
            return (ModelWithInclude) super.getModel();
        }
    }

    public static class TemplateWithExclude extends EmptyDivTemplate {

        public interface ModelWithExclude extends TemplateModel {
            Bean getBean();

            @Exclude({ "doubleValue", "booleanObject" })
            void setBean(Bean bean);
        }

        @Override
        protected ModelWithExclude getModel() {
            return (ModelWithExclude) super.getModel();
        }
    }

    public static class TemplateWithExcludeAndInclude extends EmptyDivTemplate {

        public interface ModelWithExcludeAndInclude extends TemplateModel {
            Bean getBean();

            @Include({ "doubleValue", "booleanObject" })
            @Exclude("doubleValue")
            void setBean(Bean bean);
        }

        @Override
        protected ModelWithExcludeAndInclude getModel() {
            return (ModelWithExcludeAndInclude) super.getModel();
        }

    }

    public static class TemplateWithExcludeAndIncludeSubclass
    extends TemplateWithExcludeAndInclude {
        // Should work exactly the same way as the parent class
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

    public static class TemplateWithExcludeAndIncludeSubclassOverrides
    extends TemplateWithExcludeAndInclude {

        public interface ModelWithExcludeAndIncludeSubclass
        extends ModelWithExcludeAndInclude {

            /*
             * Super class has annotations for this method to only include
             * 'booleanObject'. This tests that includes can be overridden in a
             * sub class (and parent class annotations are ignored).
             */
            @Override
            @Include("doubleValue")
            void setBean(Bean bean);
        }

        @Override
        protected ModelWithExcludeAndIncludeSubclass getModel() {
            return (ModelWithExcludeAndIncludeSubclass) super.getModel();
        }

        @Override
        protected Class<? extends TemplateModel> getModelType() {
            return ModelWithExcludeAndIncludeSubclass.class;
        }
    }

    public static class TemplateWithExcludeForSubBean extends EmptyDivTemplate {

        public interface ModelWithExcludeForSubBean extends TemplateModel {
            BeanContainingBeans getBeanContainingBeans();

            @Exclude({ "bean1.booleanObject", "bean2" })
            void setBeanContainingBeans(
                    BeanContainingBeans beanContainingBeans);
        }

        @Override
        protected ModelWithExcludeForSubBean getModel() {
            return (ModelWithExcludeForSubBean) super.getModel();
        }

    }

    public static class TemplateWithIncludeForSubBean extends EmptyDivTemplate {

        public interface ModelWithIncludeForSubBean extends TemplateModel {
            BeanContainingBeans getBeanContainingBeans();

            @Include({ "bean1.booleanObject" })
            void setBeanContainingBeans(
                    BeanContainingBeans beanContainingBeans);
        }

        @Override
        protected ModelWithIncludeForSubBean getModel() {
            return (ModelWithIncludeForSubBean) super.getModel();
        }

    }

    public static class TemplateWithIncludeOnList extends EmptyDivTemplate {

        public interface ModelWithIncludeOnList extends TemplateModel {
            @Include("intValue")
            void setBeans(List<Bean> beans);

            List<Bean> getBeans();
        }

        @Override
        protected ModelWithIncludeOnList getModel() {
            return (ModelWithIncludeOnList) super.getModel();
        }

    }

    public static class TemplateWithIncludeOnListSubBean
    extends EmptyDivTemplate {

        public interface ModelWithIncludeOnListSubBean extends TemplateModel {
            @Include({ "bean1.intValue", "bean2.booleanValue" })
            void setBeanContainingBeans(List<BeanContainingBeans> beans);
        }

        @Override
        protected ModelWithIncludeOnListSubBean getModel() {
            return (ModelWithIncludeOnListSubBean) super.getModel();
        }

    }

    public static class TemplateWithExcludeOnList extends EmptyDivTemplate {

        public interface ModelWithExcludeOnList extends TemplateModel {
            @Exclude("intValue")
            void setBeans(List<Bean> beans);

            List<Bean> getBeans();
        }

        @Override
        protected ModelWithExcludeOnList getModel() {
            return (ModelWithExcludeOnList) super.getModel();
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

        List<NodeChange> changes = new ArrayList<>();
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

    @Test(expected = IllegalArgumentException.class)
    public void testBooleanParseString() {
        BasicTypeModelTemplate template = new BasicTypeModelTemplate();
        BasicTypeModel model = template.getModel();

        Assert.assertEquals(null, model.getBoolean());

        template.getElement().getNode().getFeature(ModelMap.class)
        .setValue("boolean", "True");

        model.getBoolean();
    }

    @Test(expected = IllegalArgumentException.class)
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
        Bean bean = new BeanImpl(beanTriggered);
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

        ModelMap modelMap = ModelMap.get(stateNode);
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
        SubSubBeanIface subBean = new SubSubBeanIfaceImpl();
        subBean.setValue(4);
        proxy.setBean(subBean);

        SubSubBeanIface subProxy = model.getProxy("bean.bean",
                SubSubBeanIface.class);

        Assert.assertEquals(4, subProxy.getValue());
    }

    private void setModelPropertyAndVerifyGetter(AngularTemplate template,
            Supplier<Object> getter, String beanPath, String property,
            Serializable expected) {
        ModelMap feature = getModelMap(template, beanPath);
        feature.setValue(property, expected);
        Assert.assertEquals(expected, getter.get());
    }

    private void verifyModel(AngularTemplate template, String beanPath,
            String property, Serializable expected) {
        ModelMap feature = getModelMap(template, beanPath);
        Assert.assertNotNull(feature);
        Assert.assertEquals(expected, feature.getValue(property));
    }

    private ModelMap getModelMap(AngularTemplate template, String beanPath) {
        StateNode node = template.getElement().getNode();
        return ModelMap.get(node).resolveModelMap(beanPath);
    }

    private ModelList getModelList(AngularTemplate template, String beanPath) {
        StateNode node = template.getElement().getNode();
        return ModelMap.get(node).resolveModelList(beanPath);
    }

    @Test
    public void getListFromModel() {
        ListBeanModelTemplate template = new ListBeanModelTemplate();
        List<Bean> beans = new ArrayList<>();
        beans.add(new Bean(100));
        beans.add(new Bean(200));
        beans.add(new Bean(300));
        template.getModel().setBeans(beans);
        assertListContentsEquals(template.getModel().getBeans(), new Bean(100),
                new Bean(200), new Bean(300));
    }

    @SafeVarargs
    private static <T> void assertListContentsEquals(List<T> list, T... beans) {
        Assert.assertEquals(beans.length, list.size());
        for (int i = 0; i < beans.length; i++) {
            Assert.assertThat(list.get(i),
                    Matchers.samePropertyValuesAs(beans[i]));
            Assert.assertNotSame(beans[i], list.get(i));
        }

    }

    @Test
    public void setBeanIncludeProperties() {
        TemplateWithInclude template = new TemplateWithInclude();
        template.getModel().setBean(new Bean(123));

        ModelMap modelMap = getModelMap(template, "bean");
        Set<String> mapKeys = getKeys(modelMap);
        Assert.assertTrue("Model should contain included 'doubleValue'",
                mapKeys.remove("doubleValue"));
        Assert.assertTrue("Model should contain included 'booleanObject'",
                mapKeys.remove("booleanObject"));
        Assert.assertTrue("model should be empty but contains: " + mapKeys,
                mapKeys.isEmpty());
    }

    @Test
    public void setBeanExcludeProperties() {
        TemplateWithExclude template = new TemplateWithExclude();
        template.getModel().setBean(new Bean(123));

        ModelMap modelMap = getModelMap(template, "bean");
        Set<String> mapKeys = getKeys(modelMap);
        Set<String> excluded = new HashSet<>();
        excluded.add("doubleValue");
        excluded.add("booleanObject");

        for (String excludedPropertyName : excluded) {
            Assert.assertFalse("Model should not contain excluded '"
                    + excludedPropertyName + "'",
                    mapKeys.contains(excludedPropertyName));
        }

        ReflectTools.getSetterMethods(Bean.class)
        .map(method -> ReflectTools.getPropertyName(method))
        .forEach(propertyName -> {
            if (!excluded.contains(propertyName)) {
                Assert.assertTrue(
                        "Model should contain the property '"
                                + propertyName + "'",
                                mapKeys.remove(propertyName));
            }
        });
        Assert.assertTrue("model should be empty but contains: " + mapKeys,
                mapKeys.isEmpty());
    }

    @Test
    public void setBeanIncludeAndExcludeProperties() {
        TemplateWithExcludeAndInclude template = new TemplateWithExcludeAndInclude();
        template.getModel().setBean(new Bean(123));
        ModelMap modelMap = getModelMap(template, "bean");
        Assert.assertTrue(modelMap.hasValue("booleanObject"));
        Assert.assertEquals(1, modelMap.getKeys().count());
    }

    @Test
    public void includeExcludeWhenUsingSubclass() {
        TemplateWithExcludeAndIncludeSubclass template = new TemplateWithExcludeAndIncludeSubclass();
        template.getModel().setBean(new Bean(123));
        ModelMap modelMap = getModelMap(template, "bean");
        Assert.assertTrue(modelMap.hasValue("booleanObject"));
        Assert.assertEquals(1, modelMap.getKeys().count());
    }

    @Test
    public void includeExcludeOverrideInSubclass() {
        TemplateWithExcludeAndIncludeSubclassOverrides template = new TemplateWithExcludeAndIncludeSubclassOverrides();
        template.getModel().setBean(new Bean(123));
        ModelMap modelMap = getModelMap(template, "bean");
        Assert.assertTrue(modelMap.hasValue("doubleValue"));
        Assert.assertEquals(1, modelMap.getKeys().count());
    }

    @Test
    public void setBeanExcludeSubBeanProperties() {
        TemplateWithExcludeForSubBean template = new TemplateWithExcludeForSubBean();
        BeanContainingBeans beanContainer = new BeanContainingBeans();
        beanContainer.setBean1(new Bean(1));
        beanContainer.setBean2(new Bean(2));

        template.getModel().setBeanContainingBeans(beanContainer);

        Assert.assertNotNull(
                template.getModel().getBeanContainingBeans().getBean1());
        Assert.assertTrue(getModelMap(template, "beanContainingBeans.bean1")
                .hasValue("booleanValue"));
        // bean1.booleanObject is excluded
        Assert.assertFalse(getModelMap(template, "beanContainingBeans.bean1")
                .hasValue("booleanObject"));
    }

    @Test(expected = InvalidTemplateModelException.class)
    public void setBeanExcludeSubBeanProperties_getterThrows() {
        TemplateWithExcludeForSubBean template = new TemplateWithExcludeForSubBean();
        BeanContainingBeans beanContainer = new BeanContainingBeans();
        beanContainer.setBean2(new Bean(2));

        template.getModel().setBeanContainingBeans(beanContainer);

        template.getModel().getBeanContainingBeans().getBean2();
    }

    @Test
    public void setBeanIncludeSubBeanProperties() {
        TemplateWithIncludeForSubBean template = new TemplateWithIncludeForSubBean();
        BeanContainingBeans beanContainer = new BeanContainingBeans();
        beanContainer.setBean1(new Bean(1));
        beanContainer.setBean2(new Bean(2));
        template.getModel().setBeanContainingBeans(beanContainer);

        Assert.assertNotNull(
                template.getModel().getBeanContainingBeans().getBean1());

        ModelMap bean1Map = getModelMap(template, "beanContainingBeans.bean1");
        Set<String> bean1Keys = getKeys(bean1Map);
        Assert.assertTrue(bean1Keys.contains("booleanObject"));
        Assert.assertEquals(1, bean1Keys.size());
    }

    @Test(expected = InvalidTemplateModelException.class)
    public void setBeanIncludeSubBeanProperties_getterThrows() {
        TemplateWithIncludeForSubBean template = new TemplateWithIncludeForSubBean();
        BeanContainingBeans beanContainer = new BeanContainingBeans();
        beanContainer.setBean2(new Bean(2));
        template.getModel().setBeanContainingBeans(beanContainer);

        template.getModel().getBeanContainingBeans().getBean2();
    }

    @Test
    public void setListWithInclude() {
        TemplateWithIncludeOnList template = new TemplateWithIncludeOnList();
        List<Bean> beans = new ArrayList<>();
        beans.add(new Bean(1));
        beans.add(new Bean(2));
        template.getModel().setBeans(beans);

        ModelList modelList = getModelList(template, "beans");
        ModelMap bean1 = ModelMap.get(modelList.get(0));
        Set<String> propertiesInMap = bean1.getKeys()
                .collect(Collectors.toSet());
        Assert.assertTrue("Bean in model should have an 'intValue' property",
                propertiesInMap.remove("intValue"));
        Assert.assertEquals(
                "All other properties should have been filtered out", 0,
                propertiesInMap.size());
    }

    @Test
    public void setListWithExclude() {
        TemplateWithExcludeOnList template = new TemplateWithExcludeOnList();
        List<Bean> beans = new ArrayList<>();
        beans.add(new Bean(1));
        beans.add(new Bean(2));
        template.getModel().setBeans(beans);

        ModelList modelList = getModelList(template, "beans");
        ModelMap bean1 = ModelMap.get(modelList.get(0));
        ModelMap bean2 = ModelMap.get(modelList.get(1));

        Set<String> bean1InMap = getKeys(bean1);
        Set<String> bean2InMap = getKeys(bean2);
        Assert.assertFalse(
                "Bean1 in model should not have an 'intValue' property",
                bean1InMap.contains("intValue"));
        Assert.assertFalse(
                "Bean2 in model should not have an 'intValue' property",
                bean2InMap.contains("intValue"));
        Assert.assertEquals("All other properties should have been included", 6,
                bean1InMap.size());
        Assert.assertEquals("All other properties should have been included", 6,
                bean2InMap.size());
    }

    @Test
    public void setListWithSubBeanInclude() {
        TemplateWithIncludeOnListSubBean template = new TemplateWithIncludeOnListSubBean();
        List<BeanContainingBeans> beanContainingBeans = new ArrayList<>();
        beanContainingBeans
        .add(new BeanContainingBeans(new Bean(11), new Bean(12)));
        beanContainingBeans.add(new BeanContainingBeans(null, new Bean(22)));
        template.getModel().setBeanContainingBeans(beanContainingBeans);

        ModelList modelList = getModelList(template, "beanContainingBeans");
        Assert.assertEquals(2, modelList.size());

        ModelMap container1Map = ModelMap.get(modelList.get(0));
        ModelMap container2Map = ModelMap.get(modelList.get(1));
        Set<String> bean1bean2 = new HashSet<>();
        bean1bean2.add("bean1");
        bean1bean2.add("bean2");
        Assert.assertEquals(bean1bean2,
                container1Map.getKeys().collect(Collectors.toSet()));
        Assert.assertEquals(bean1bean2,
                container2Map.getKeys().collect(Collectors.toSet()));

        Set<String> container1Bean1Properties = getKeys(
                container1Map.resolveModelMap("bean1"));
        Assert.assertTrue(container1Bean1Properties.remove("intValue"));
        Assert.assertEquals(0, container1Bean1Properties.size());

        Set<String> container1Bean2Properties = getKeys(
                container1Map.resolveModelMap("bean2"));
        Assert.assertTrue(container1Bean2Properties.remove("booleanValue"));
        Assert.assertEquals(0, container1Bean2Properties.size());

        Set<String> container2Bean1Properties = getKeys(
                container2Map.resolveModelMap("bean1"));
        // Null value in the initial bean implies not imported or created
        Assert.assertEquals(0, container2Bean1Properties.size());

        Set<String> container2Bean2Properties = getKeys(
                container2Map.resolveModelMap("bean2"));
        Assert.assertTrue(container2Bean2Properties.remove("booleanValue"));
        Assert.assertEquals(0, container2Bean2Properties.size());
    }

    private static Set<String> getKeys(ModelMap map) {
        return map.getKeys().collect(Collectors.toSet());
    }

}
