/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.commons.io.IOUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.data.binder.testcomponents.TestTextField;
import com.vaadin.flow.tests.data.bean.BeanToValidate;

/**
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class NotEmptyTest {

    private static String NOT_EMPTY = "org.hibernate.validator.constraints.NotEmpty";

    private static class TestClassLoader extends URLClassLoader {

        public TestClassLoader() {
            super(new URL[0], Thread.currentThread().getContextClassLoader());
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            String vaadinPackagePrefix = getClass().getPackage().getName();
            vaadinPackagePrefix = vaadinPackagePrefix.substring(0,
                    vaadinPackagePrefix.lastIndexOf('.'));
            if (name.equals(UnitTest.class.getName())) {
                super.loadClass(name);
            } else if (name.startsWith(NotEmpty.class.getPackage().getName())) {
                throw new ClassNotFoundException();
            } else if (name.startsWith(vaadinPackagePrefix)) {
                String path = name.replace('.', '/').concat(".class");
                URL resource = Thread.currentThread().getContextClassLoader()
                        .getResource(path);
                InputStream stream;
                try {
                    stream = resource.openStream();
                    byte[] bytes = IOUtils.toByteArray(stream);
                    return defineClass(name, bytes, 0, bytes.length);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return super.loadClass(name);
        }
    }

    public interface UnitTest {
        void execute();
    }

    public static class NotEmptyUnitTest implements UnitTest {

        private final TestTextField nameField = new TestTextField();

        @Override
        public void execute() {
            try {
                Class.forName(NOT_EMPTY);
                // The NotEmpty class must not be in the classpath
                Assert.fail();
            } catch (ClassNotFoundException e) {
            }
            BeanValidationBinder<BeanToValidate> binder = new BeanValidationBinder<>(
                    BeanToValidate.class);

            BeanToValidate item = new BeanToValidate();
            String name = "Johannes";
            item.setFirstname(name);
            item.setAge(32);

            binder.bind(nameField, "firstname");
            binder.setBean(item);

            Assert.assertTrue(nameField.isRequiredIndicatorVisible());
        }

    }

    @Test
    public void notEmptyAnnotationIsNotInClasspath()
            throws ClassNotFoundException, NoSuchMethodException,
            SecurityException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, IOException,
            InterruptedException {
        try (URLClassLoader loader = new TestClassLoader()) {
            Class<?> clazz = loader.loadClass(NotEmptyUnitTest.class.getName());
            UnitTest test = (UnitTest) clazz.newInstance();
            test.execute();
        }
    }

}
