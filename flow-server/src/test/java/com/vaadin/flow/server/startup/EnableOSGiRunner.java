/*
 * Copyright 2000-2020 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.server.startup;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import com.vaadin.flow.server.VaadinServlet;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

public class EnableOSGiRunner extends BlockJUnit4ClassRunner {

    public static class Interceptor {

        @RuntimeType
        public static Object intercept() {
            return new Object();
        }
    }

    private static class TestClassLoader extends URLClassLoader {

        private final Map<String, Class<?>> loadedClasses = new HashMap<>();

        public TestClassLoader() {
            super(new URL[0], Thread.currentThread().getContextClassLoader());
        }

        @Override
        public synchronized Class<?> loadClass(String name)
                throws ClassNotFoundException {
            Class<?> class1 = loadedClasses.get(name);
            if (class1 != null) {
                return class1;
            }
            String vaadinPackagePrefix = VaadinServlet.class.getPackage()
                    .getName();
            vaadinPackagePrefix = vaadinPackagePrefix.substring(0,
                    vaadinPackagePrefix.lastIndexOf('.'));
            if (name.equals("org.osgi.framework.InvalidSyntaxException")) {
                Builder<Exception> builder = new ByteBuddy()
                        .subclass(Exception.class);
                return builder.name("org.osgi.framework.InvalidSyntaxException")
                        .make().load(this, ClassLoadingStrategy.Default.WRAPPER)
                        .getLoaded();
            } else if (name.equals("org.osgi.framework.FrameworkUtil")) {
                Builder<Object> builder = new ByteBuddy()
                        .subclass(Object.class);

                Class<?> fwUtil = builder
                        .defineMethod("getBundle", Object.class,
                                Modifier.PUBLIC | Modifier.STATIC)
                        .withParameter(Class.class)
                        .intercept(MethodDelegation.to(Interceptor.class))
                        .name(name).make()
                        .load(this, ClassLoadingStrategy.Default.WRAPPER)
                        .getLoaded();
                return fwUtil;
            } else if (name.startsWith(vaadinPackagePrefix)) {
                String path = name.replace('.', '/').concat(".class");
                URL resource = Thread.currentThread().getContextClassLoader()
                        .getResource(path);
                InputStream stream;
                try {
                    stream = resource.openStream();
                    byte[] bytes = IOUtils.toByteArray(stream);
                    Class<?> clazz = defineClass(name, bytes, 0, bytes.length);
                    loadedClasses.put(name, clazz);
                    return clazz;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return super.loadClass(name);
        }

        private Class<?> defineBundleClass() {
            Builder<?> builder = new ByteBuddy().makeInterface();

            Class<?> bundleClass = builder.name("org.osgi.framework.Bundle")
                    .make().load(this, ClassLoadingStrategy.Default.WRAPPER)
                    .getLoaded();
            return bundleClass;
        }
    }

    private static final TestClassLoader classLoader = new TestClassLoader();

    public EnableOSGiRunner(Class<?> clazz)
            throws InitializationError, ClassNotFoundException {
        super(classLoader.loadClass(clazz.getName()));
    }

    @Override
    public void run(final RunNotifier notifier) {
        Thread thread = new Thread(() -> super.run(notifier));
        thread.setContextClassLoader(classLoader);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
