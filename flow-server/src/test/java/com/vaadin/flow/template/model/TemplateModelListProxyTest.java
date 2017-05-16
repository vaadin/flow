package com.vaadin.flow.template.model;

import static org.hamcrest.CoreMatchers.is;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.nodefeature.ModelList;

/**
 * @author Vaadin Ltd.
 */
public class TemplateModelListProxyTest {

    private static class ClassWithDefaultConstructor {
        private String field;

        private String getField() {
            return field;
        }

        private void setField(String field) {
            this.field = field;
        }
    }

    private static class ClassWithoutDefaultConstructor {
        private String field;

        private ClassWithoutDefaultConstructor(String field) {
            this.field = field;
        }

        private String getField() {
            return field;
        }

        private void setField(String field) {
            this.field = field;
        }
    }

    @Test
    public void clear_noDefaultConstructor() {
        TemplateModelListProxy<ClassWithoutDefaultConstructor> list = createModelListProxy(
                ClassWithoutDefaultConstructor.class);
        list.add(new ClassWithoutDefaultConstructor("one"));
        list.add(new ClassWithoutDefaultConstructor("two"));

        assertListClearedNormally(list);
    }

    @Test
    public void clear_defaultConstructor() {
        TemplateModelListProxy<ClassWithDefaultConstructor> list = createModelListProxy(
                ClassWithDefaultConstructor.class);
        ClassWithDefaultConstructor one = new ClassWithDefaultConstructor();
        one.setField("one");
        ClassWithDefaultConstructor two = new ClassWithDefaultConstructor();
        two.setField("one");

        list.add(one);
        list.add(two);

        assertListClearedNormally(list);
    }

    private static void assertListClearedNormally(
            TemplateModelListProxy<?> list) {
        list.clear();
        Assert.assertThat(
                "List should be of size 0, since we've called clear()",
                list.size(), is(0));
    }

    private static <T> TemplateModelListProxy<T> createModelListProxy(
            Class<T> proxyClass) {
        return new TemplateModelListProxy<>(new StateNode(ModelList.class),
                new BeanModelType<>(proxyClass, PropertyFilter.ACCEPT_ALL));
    }
}
