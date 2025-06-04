/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.templatemodel;

import org.hamcrest.MatcherAssert;
import org.junit.Test;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ModelList;

import static org.hamcrest.CoreMatchers.is;

public class TemplateModelListProxyTest {

    private static class ClassWithDefaultConstructor {
        private String field;

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }
    }

    private static class ClassWithoutDefaultConstructor {
        private String field;

        private ClassWithoutDefaultConstructor(String field) {
            this.field = field;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
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
        MatcherAssert.assertThat(
                "List should be of size 0, since we've called clear()",
                list.size(), is(0));
    }

    private static <T> TemplateModelListProxy<T> createModelListProxy(
            Class<T> proxyClass) {
        return new TemplateModelListProxy<>(new StateNode(ModelList.class),
                new BeanModelType<>(proxyClass, PropertyFilter.ACCEPT_ALL,
                        false));
    }
}
