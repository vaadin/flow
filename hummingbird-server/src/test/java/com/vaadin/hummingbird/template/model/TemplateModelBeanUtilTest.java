package com.vaadin.hummingbird.template.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.nodefeature.ModelList;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.hummingbird.template.model.TemplateModelTest.NoModelTemplate;
import com.vaadin.ui.Template;

public class TemplateModelBeanUtilTest {

    public static class BeanWithUnsupportedProperty {
        // not a bean
        private StringBuilder sb;

        public StringBuilder getSb() {
            return sb;
        }

        public void setSb(StringBuilder sb) {
            this.sb = sb;
        }
    }

    public static class BeanWithList {
        private List<Bean> beans;

        public List<Bean> getBeans() {
            return beans;
        }

        public void setBeans(List<Bean> beans) {
            this.beans = beans;
        }
    }

    public static class BeanWithNestedList {
        private List<List<Bean>> beans;

        public List<List<Bean>> getBeans() {
            return beans;
        }

        public void setBeans(List<List<Bean>> beans) {
            this.beans = beans;
        }
    }

    public static class BeanWithPrimitiveList {
        private List<String> strings;

        public List<String> getStrings() {
            return strings;
        }

        public void setStrings(List<String> strings) {
            this.strings = strings;
        }
    }

    public static class BeanWithSet {
        private Set<Bean> beans;

        public Set<Bean> getBeans() {
            return beans;
        }

        public void setBeans(Set<Bean> beans) {
            this.beans = beans;
        }
    }

    public static class BeanWithNestedBean {
        private Bean bean;

        public Bean getBean() {
            return bean;
        }

        public void setBean(Bean bean) {
            this.bean = bean;
        }
    }

    public static class BeanTemplate extends NoModelTemplate {

        @Override
        public BeanModel getModel() {
            return (BeanModel) super.getModel();
        }
    }

    private static final Predicate<String> INCLUDE_ALL = propertyName -> true;

    @Test
    public void testBeanToModelMapImport() {
        Bean bean = new Bean();
        bean.setBooleanObject(Boolean.TRUE);
        bean.setBooleanValue(true);
        bean.setIntValue(1);
        bean.setIntObject(Integer.valueOf(2));
        bean.setDoubleValue(1.0d);
        bean.setDoubleObject(Double.valueOf(2.0d));
        bean.setString("foobar");

        NoModelTemplate template = new NoModelTemplate();
        template.getModel().importBean("namespace", bean, INCLUDE_ALL);

        assertBeanEqualsModelMap(bean, template, "namespace");
    }

    @Test
    public void testBeanToModelImport() {
        Bean bean = new Bean();
        bean.setBooleanObject(Boolean.TRUE);
        bean.setBooleanValue(true);
        bean.setIntValue(1);
        bean.setIntObject(Integer.valueOf(2));
        bean.setDoubleValue(1.0d);
        bean.setDoubleObject(Double.valueOf(2.0d));
        bean.setString("foobar");

        BeanTemplate beanTemplate = new BeanTemplate();
        BeanModel beanModel = beanTemplate.getModel();
        beanTemplate.getModel().importBean("", bean, INCLUDE_ALL);

        assertBeanEqualsModelViaInterface(bean, beanModel);
        assertBeanEqualsModelMap(bean, beanTemplate, "");

        Bean bean2 = new Bean();
        bean2.setBooleanObject(Boolean.FALSE);
        bean2.setBooleanValue(false);
        bean2.setIntValue(5);
        bean2.setIntObject(Integer.valueOf(123));
        bean2.setDoubleValue(10.0d);
        bean2.setDoubleObject(Double.valueOf(20.0d));
        bean2.setString("shazbot");

        beanTemplate.getModel().importBean("", bean2, INCLUDE_ALL);

        assertBeanEqualsModelViaInterface(bean2, beanModel);
        assertBeanEqualsModelMap(bean2, beanTemplate, "");
    }

    @Test
    public void testBeanToModelImportNullAndDefaultValues() {
        Bean bean = new Bean();
        BeanTemplate beanTemplate = new BeanTemplate();
        BeanModel beanModel = beanTemplate.getModel();
        beanTemplate.getModel().importBean("namespace", bean, INCLUDE_ALL);

        assertBeanEqualsModelViaInterface(bean, beanModel);
        assertBeanEqualsModelMap(bean, beanTemplate, "namespace");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidBeanProperty() {
        NoModelTemplate template = new NoModelTemplate();
        BeanWithUnsupportedProperty bean = new BeanWithUnsupportedProperty();
        // won't crash if both are null
        bean.setSb(new StringBuilder());

        template.getModel().importBean("namespace", bean, INCLUDE_ALL);
    }

    @Test(expected = InvalidTemplateModelException.class)
    public void testBeanWithSetProperty() {
        NoModelTemplate template = new NoModelTemplate();
        BeanWithSet bean = new BeanWithSet();
        // won't crash if both are null
        bean.setBeans(new HashSet<>());

        template.getModel().importBean("namespace", bean, INCLUDE_ALL);
    }

    @Test(expected = InvalidTemplateModelException.class)
    public void testBeanWithNestedListProperty() {
        NoModelTemplate template = new NoModelTemplate();
        BeanWithNestedList bean = new BeanWithNestedList();
        // won't crash if both are null
        bean.setBeans(new ArrayList<>());

        template.getModel().importBean("namespace", bean, INCLUDE_ALL);
    }

    @Test
    public void testSetBeanWithListProperty() {
        NoModelTemplate template = new NoModelTemplate();
        BeanWithList bean = new BeanWithList();

        Bean listItem1 = new Bean();
        listItem1.setString("item1");
        Bean listItem2 = new Bean();
        listItem2.setString("item2");

        bean.setBeans(Arrays.asList(listItem1, listItem2));

        template.getModel().importBean("namespace", bean, INCLUDE_ALL);

        ModelMap modelMap = ModelPathResolver.forPath("namespace")
                .resolveModelMap(template.getElement().getNode());
        StateNode beansNode = (StateNode) modelMap.getValue("beans");

        Assert.assertTrue(beansNode.hasFeature(ModelList.class));

        ModelList modelList = beansNode.getFeature(ModelList.class);

        Assert.assertEquals(2, modelList.size());

        assertBeanEqualsModelMap(listItem1,
                modelList.get(0).getFeature(ModelMap.class));
        assertBeanEqualsModelMap(listItem2,
                modelList.get(1).getFeature(ModelMap.class));
    }

    @Test
    public void testImportBeans() {
        NoModelTemplate template = new NoModelTemplate();

        Bean listItem1 = new Bean();
        listItem1.setString("item1");
        Bean listItem2 = new Bean();
        listItem2.setString("item2");

        List<Bean> beans = Arrays.asList(listItem1, listItem2);

        template.getModel().importBeans("beans", beans, Bean.class,
                name -> true);

        ModelMap modelMap = template.getElement().getNode()
                .getFeature(ModelMap.class);
        StateNode beansNode = (StateNode) modelMap.getValue("beans");

        Assert.assertTrue(beansNode.hasFeature(ModelList.class));

        ModelList modelList = beansNode.getFeature(ModelList.class);

        Assert.assertEquals(2, modelList.size());

        assertBeanEqualsModelMap(listItem1,
                modelList.get(0).getFeature(ModelMap.class));
        assertBeanEqualsModelMap(listItem2,
                modelList.get(1).getFeature(ModelMap.class));
    }

    @Test
    public void testImportBeansModelPath() {
        NoModelTemplate template = new NoModelTemplate();

        Bean listItem1 = new Bean(1);
        Bean listItem2 = new Bean(2);

        List<Bean> beans = Arrays.asList(listItem1, listItem2);

        template.getModel().importBeans("some.part.contains.beans", beans,
                Bean.class, name -> true);

        ModelList modelList = ModelPathResolver
                .forPath("some.part.contains.beans")
                .resolveModelList(template.getElement().getNode());

        Assert.assertEquals(2, modelList.size());

        assertBeanEqualsModelMap(listItem1,
                modelList.get(0).getFeature(ModelMap.class));
        assertBeanEqualsModelMap(listItem2,
                modelList.get(1).getFeature(ModelMap.class));
    }

    @Test
    public void testImportFilteredBeans() {
        NoModelTemplate template = new NoModelTemplate();

        Bean bean = new Bean();
        bean.setString("item1");
        bean.setBooleanValue(true);

        List<Bean> beans = Arrays.asList(bean);

        template.getModel().importBeans("beans", beans, Bean.class,
                name -> "booleanValue".equals(name));

        ModelMap modelMap = template.getElement().getNode()
                .getFeature(ModelMap.class);
        StateNode beansNode = (StateNode) modelMap.getValue("beans");
        ModelList beansModel = beansNode.getFeature(ModelList.class);
        ModelMap beanModel = beansModel.get(0).getFeature(ModelMap.class);

        Assert.assertFalse(beanModel.hasValue("string"));
        Assert.assertTrue(beanModel.hasValue("booleanValue"));
    }

    @Test
    public void testImportFilteredSubBeans() {
        NoModelTemplate template = new NoModelTemplate();

        Bean childBean = new Bean();
        childBean.setString("item1");
        childBean.setBooleanValue(true);

        BeanWithNestedBean parentBean = new BeanWithNestedBean();
        parentBean.setBean(childBean);

        List<BeanWithNestedBean> beans = Arrays.asList(parentBean);

        template.getModel().importBeans("beans", beans,
                BeanWithNestedBean.class,
                name -> "bean.booleanValue".equals(name)
                        || !name.startsWith("bean."));

        ModelMap modelMap = template.getElement().getNode()
                .getFeature(ModelMap.class);
        StateNode beansNode = (StateNode) modelMap.getValue("beans");
        ModelList beansModel = beansNode.getFeature(ModelList.class);
        ModelMap parentBeanModel = beansModel.get(0).getFeature(ModelMap.class);

        StateNode childBeanNode = (StateNode) parentBeanModel.getValue("bean");
        ModelMap childBeanModel = childBeanNode.getFeature(ModelMap.class);

        Assert.assertFalse(childBeanModel.hasValue("string"));
        Assert.assertTrue(childBeanModel.hasValue("booleanValue"));
    }

    @Test(expected = InvalidTemplateModelException.class)
    public void testBeanWithPrimitiveList() {
        NoModelTemplate template = new NoModelTemplate();
        BeanWithPrimitiveList bean = new BeanWithPrimitiveList();
        // won't crash if both are null
        bean.setStrings(new ArrayList<>());

        template.getModel().importBean("namespace", bean, INCLUDE_ALL);
    }

    @Test
    public void testBeanAndModelNotConnected() {
        Bean bean = new Bean();
        bean.setIntValue(1);

        BeanTemplate template = new BeanTemplate();
        BeanModel model = template.getModel();
        model.importBean("", bean, INCLUDE_ALL);

        assertBeanEqualsModelViaInterface(bean, model);
        assertBeanEqualsModelMap(bean, template, "");

        bean.setIntValue(5);
        bean.setString("foobar");

        Assert.assertNotEquals(bean.getIntValue(), model.getIntValue());
        Assert.assertNotEquals(bean.getString(), model.getString());

        model.setString("shazbot");
        model.setIntValue(0);

        Assert.assertNotEquals(bean.getIntValue(), model.getIntValue());
        Assert.assertNotEquals(bean.getString(), model.getString());

        model.importBean("", bean, INCLUDE_ALL);

        assertBeanEqualsModelViaInterface(bean, model);
        assertBeanEqualsModelMap(bean, template, "");
    }

    @Test
    public void testNestedBean() {
        BeanWithNestedBean beanWithNestedBean = new BeanWithNestedBean();
        beanWithNestedBean.setBean(new Bean());

        NoModelTemplate template = new NoModelTemplate();
        StateNode stateNode = template.getElement().getNode();
        TemplateModel model = template.getModel();
        model.importBean("namespace", beanWithNestedBean, INCLUDE_ALL);

        ModelMap modelMap = ModelPathResolver.forPath("namespace.bean")
                .resolveModelMap(stateNode);
        assertBeanEqualsModelMap(beanWithNestedBean.getBean(), modelMap);

        // ((StateNode) template.getElement().getNode()
        // .getFeature(ModelMap.class).getValue("bean"))
        // .getFeature(ModelMap.class));

        Bean bean2 = new Bean();
        bean2.setBooleanObject(Boolean.FALSE);
        bean2.setBooleanValue(false);
        bean2.setIntValue(5);
        bean2.setIntObject(Integer.valueOf(123));
        bean2.setDoubleValue(10.0d);
        bean2.setDoubleObject(Double.valueOf(20.0d));
        bean2.setString("shazbot");

        beanWithNestedBean.setBean(bean2);

        model.importBean("namespace", beanWithNestedBean, INCLUDE_ALL);

        assertBeanEqualsModelMap(beanWithNestedBean.getBean(), modelMap);
    }

    @Test
    public void testBeanPropertyFilter() {
        Bean bean = new Bean();
        bean.setBooleanObject(Boolean.TRUE);
        bean.setBooleanValue(true);
        bean.setIntValue(1);
        bean.setIntObject(Integer.valueOf(2));
        bean.setDoubleValue(1.0d);
        bean.setDoubleObject(Double.valueOf(2.0d));
        bean.setString("foobar");

        BeanTemplate beanTemplate = new BeanTemplate();
        BeanModel beanModel = beanTemplate.getModel();

        ArrayList<String> properties = new ArrayList<>();

        // filter out everything except the "string" & intValue property
        Predicate<String> capturingFilter = propertyName -> {
            properties.add(propertyName);
            return propertyName.equals("string")
                    || propertyName.equals("intValue");

        };
        beanModel.importBean("", bean, capturingFilter);

        Assert.assertEquals(bean.getString(), beanModel.getString());
        Assert.assertEquals(bean.getIntValue(), beanModel.getIntValue());

        ModelMap modelMap = beanTemplate.getElement().getNode()
                .getFeature(ModelMap.class);

        String[] expectedProperties = new String[] { "string", "intValue",
                "intObject", "doubleObject", "doubleValue", "booleanValue",
                "booleanObject" };
        for (String s : expectedProperties) {
            Assert.assertTrue(properties.contains(s));
        }

        Assert.assertFalse(modelMap.hasValue("booleanObject"));
        Assert.assertFalse(modelMap.hasValue("booleanValue"));
        Assert.assertFalse(modelMap.hasValue("intObject"));
        Assert.assertFalse(modelMap.hasValue("doubleObject"));
        Assert.assertFalse(modelMap.hasValue("doubleValue"));
    }

    @Test
    public void testNestedBeanPropertyFilter() {
        Bean bean = new Bean();
        bean.setBooleanObject(Boolean.TRUE);
        bean.setBooleanValue(true);
        bean.setIntValue(1);
        bean.setIntObject(Integer.valueOf(2));
        bean.setDoubleValue(1.0d);
        bean.setDoubleObject(Double.valueOf(2.0d));
        bean.setString("foobar");

        BeanWithNestedBean beanWithNestedBean = new BeanWithNestedBean();
        beanWithNestedBean.setBean(bean);

        ArrayList<String> properties = new ArrayList<>();
        Predicate<String> capturingFilter = propertyName -> {
            properties.add(propertyName);
            return propertyName.equals("bean")
                    || propertyName.equals("bean.string");
        };

        NoModelTemplate template = new NoModelTemplate();
        template.getModel().importBean("namespace", beanWithNestedBean,
                capturingFilter);

        String[] expectedProperties = new String[] { "bean", "bean.string",
                "bean.intValue", "bean.intObject", "bean.doubleObject",
                "bean.doubleValue", "bean.booleanValue", "bean.booleanObject" };
        for (String s : expectedProperties) {
            Assert.assertTrue("Could not find property " + s,
                    properties.contains(s));
        }

        ModelMap nestedBeanModelMap = ModelPathResolver
                .forPath("namespace.bean")
                .resolveModelMap(template.getElement().getNode());

        Assert.assertEquals(bean.getString(),
                nestedBeanModelMap.getValue("string"));
        Assert.assertFalse(nestedBeanModelMap.hasValue("booleanObject"));
        Assert.assertFalse(nestedBeanModelMap.hasValue("booleanValue"));
        Assert.assertFalse(nestedBeanModelMap.hasValue("intObject"));
        Assert.assertFalse(nestedBeanModelMap.hasValue("doubleObject"));
        Assert.assertFalse(nestedBeanModelMap.hasValue("doubleValue"));
    }

    @Test
    public void importBean_getterThrowsExcpetion_noModelStateNodeCreatedAndImportFails() {
        Bean bean = new Bean() {

            @Override
            public String getString() {
                throw new NullPointerException();
            }
        };
        BeanWithNestedBean beanWithNestedBean = new BeanWithNestedBean();
        beanWithNestedBean.setBean(bean);

        NoModelTemplate template = new NoModelTemplate();
        TemplateModel model = template.getModel();

        ModelMap map = template.getElement().getNode()
                .getFeature(ModelMap.class);

        Assert.assertFalse(map.hasValue("bean"));

        try {
            model.importBean("namespace", beanWithNestedBean, INCLUDE_ALL);
            Assert.fail();
        } catch (IllegalStateException e) {
            Assert.assertFalse(map.hasValue("bean"));
        }
    }

    private void assertBeanEqualsModelViaInterface(Bean bean, BeanModel model) {
        Assert.assertEquals(bean.getBooleanObject(), model.getBooleanObject());
        Assert.assertEquals(bean.getDoubleObject(), model.getDoubleObject());
        Assert.assertEquals(bean.getIntObject(), model.getIntObject());
        Assert.assertEquals(bean.isBooleanValue(), model.isBooleanValue());
        Assert.assertEquals(bean.getDoubleValue(), model.getDoubleValue(),
                0.0D);
        Assert.assertEquals(bean.getIntValue(), model.getIntValue());
        Assert.assertEquals(bean.getString(), model.getString());
    }

    private void assertBeanEqualsModelMap(Bean bean, Template template,
            String namespace) {
        ModelMap modelMap = ModelPathResolver.forPath(namespace)
                .resolveModelMap(template.getElement().getNode());
        assertBeanEqualsModelMap(bean, modelMap);
    }

    private void assertBeanEqualsModelMap(Bean bean, ModelMap model) {
        Assert.assertEquals(bean.getBooleanObject(),
                model.getValue("booleanObject"));
        Assert.assertEquals(bean.getDoubleObject(),
                model.getValue("doubleObject"));
        Assert.assertEquals(bean.getIntObject(), model.getValue("intObject"));
        Assert.assertEquals(bean.isBooleanValue(),
                model.getValue("booleanValue"));
        Assert.assertEquals(bean.getDoubleValue(),
                model.getValue("doubleValue"));
        Assert.assertEquals(bean.getIntValue(), model.getValue("intValue"));
        Assert.assertEquals(bean.getString(), model.getValue("string"));
    }

    @Test
    public void filterNotAffectedByImportPath() {
        NoModelTemplate template = new NoModelTemplate();
        template.getModel().importBean("foo.bar", new Bean(), propertyName -> {
            Assert.assertFalse(propertyName.contains("foo.bar"));
            return true;
        });
    }

    @Test
    public void getListOfBeans() {
        TemplateModel model = new NoModelTemplate().getModel();
        List<Bean> proxyList = importAndGetBeans("list", model, 3);
        assertListContentsEquals(proxyList, new Bean(1), new Bean(2),
                new Bean(3));
    }

    private List<Bean> importAndGetBeans(String modelPath, TemplateModel model,
            int nrOfBeans) {
        List<Bean> beans = new ArrayList<>();
        for (int i = 0; i < nrOfBeans; i++) {
            Bean bean = new Bean(i + 1);
            beans.add(bean);
        }
        TemplateModelBeanUtil.importBeans(
                TemplateModelProxyHandler.getStateNodeForProxy(model),
                modelPath, beans, Bean.class, propertyName -> true);
        return model.getListProxy(modelPath, Bean.class);
    }

    @Test
    public void addBeanToListOfBeans() {
        TemplateModel model = new NoModelTemplate().getModel();
        List<Bean> proxyList = importAndGetBeans("list", model, 1);

        proxyList.add(new Bean(2));

        assertModelListEquals(model, "list", new Bean(1), new Bean(2));
        assertListProxyEquals(model, "list", new Bean(1), new Bean(2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addNullToListOfBeans() {
        TemplateModel model = new NoModelTemplate().getModel();
        List<Bean> proxyList = importAndGetBeans("list", model, 1);
        proxyList.add(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setBeanInListOfBeansToNull() {
        TemplateModel model = new NoModelTemplate().getModel();
        List<Bean> proxyList = importAndGetBeans("list", model, 1);

        proxyList.set(0, null);
    }

    @Test
    public void insertBeanIntoListOfBeans() {
        TemplateModel model = new NoModelTemplate().getModel();
        List<Bean> proxyList = importAndGetBeans("list", model, 1);

        proxyList.add(0, new Bean(0));

        assertModelListEquals(model, "list", new Bean(0), new Bean(1));
        assertListProxyEquals(model, "list", new Bean(0), new Bean(1));
    }

    @Test
    public void removeBeanByIndexFromListOfBeans() {
        TemplateModel model = new NoModelTemplate().getModel();
        List<Bean> proxyList = importAndGetBeans("list", model, 3);

        proxyList.remove(1); // Remove bean2

        assertModelListEquals(model, "list", new Bean(1), new Bean(3));
        assertListProxyEquals(model, "list", new Bean(1), new Bean(3));
    }

    @Test
    public void removeBeanFromListOfBeansReturnsProxy() {
        TemplateModel model = new NoModelTemplate().getModel();
        List<Bean> proxyList = importAndGetBeans("list", model, 3);

        Bean removed = proxyList.remove(1); // Remove bean2
        Assert.assertTrue(TemplateModelProxyHandler.isProxy(removed));
    }

    @Test
    public void indexOfBeanInListOfBeans() {
        TemplateModel model = new NoModelTemplate().getModel();
        List<Bean> proxyList = importAndGetBeans("list", model, 2);

        Assert.assertEquals(0, proxyList.indexOf(proxyList.get(0)));
        Assert.assertEquals(1, proxyList.indexOf(proxyList.get(1)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void indexOfNonProxyBeanInListOfBeans() {
        TemplateModel model = new NoModelTemplate().getModel();
        List<Bean> proxyList = importAndGetBeans("list", model, 2);
        proxyList.indexOf(new Bean(1));
    }

    @Test
    public void indexOfProxyBeanNotInList() {
        TemplateModel model = new NoModelTemplate().getModel();
        List<Bean> proxyList = importAndGetBeans("list", model, 2);
        Bean removedBean = proxyList.remove(1);
        Assert.assertEquals(-1, proxyList.indexOf(removedBean));
    }

    @Test
    public void removeBeanByInstanceFromListOfBeans() {
        TemplateModel model = new NoModelTemplate().getModel();
        List<Bean> proxyList = importAndGetBeans("list", model, 3);

        proxyList.remove(proxyList.get(1));

        assertModelListEquals(model, "list", new Bean(1), new Bean(3));
        assertListProxyEquals(model, "list", new Bean(1), new Bean(3));
    }

    @Test
    public void setBeanInstanceInListOfBeans() {
        TemplateModel model = new NoModelTemplate().getModel();
        List<Bean> proxyList = importAndGetBeans("list", model, 3);

        proxyList.set(1, new Bean(4));

        assertModelListEquals(model, "list", new Bean(1), new Bean(4),
                new Bean(3));
        assertListProxyEquals(model, "list", new Bean(1), new Bean(4),
                new Bean(3));
    }

    private void assertModelListEquals(TemplateModel model, String modelPath,
            Bean... beans) {
        StateNode stateNode = TemplateModelProxyHandler
                .getStateNodeForProxy(model);
        ModelList modelList = ModelPathResolver.forPath(modelPath)
                .resolveModelList(stateNode);
        assertModelListContentsEquals(modelList, beans);
    }

    private void assertListProxyEquals(TemplateModel model, String modelPath,
            Bean... beans) {
        assertListContentsEquals(model.getListProxy(modelPath, Bean.class),
                beans);

    }

    private <T> void assertModelListContentsEquals(ModelList list,
            Bean... beans) {
        Assert.assertEquals(beans.length, list.size());
        for (int i = 0; i < beans.length; i++) {
            ModelMap map = list.get(i).getFeature(ModelMap.class);
            Bean modelProxy = TemplateModelProxyHandler
                    .createModelProxy(map.getNode(), Bean.class);
            Assert.assertThat(modelProxy,
                    Matchers.samePropertyValuesAs(beans[i]));
        }

    }

    static <T> void assertListContentsEquals(List<T> list, T... beans) {
        Assert.assertEquals(beans.length, list.size());
        for (int i = 0; i < beans.length; i++) {
            Assert.assertThat(list.get(i),
                    Matchers.samePropertyValuesAs(beans[i]));
            Assert.assertNotSame(beans[i], list.get(i));
        }

    }
}
