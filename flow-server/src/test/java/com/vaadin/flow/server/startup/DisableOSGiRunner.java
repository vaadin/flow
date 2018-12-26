/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.commons.io.IOUtils;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.osgi.framework.FrameworkUtil;

import com.vaadin.flow.server.VaadinServlet;

public class DisableOSGiRunner extends BlockJUnit4ClassRunner {

    private static class TestClassLoader extends URLClassLoader {

        public TestClassLoader() {
            super(new URL[0], Thread.currentThread().getContextClassLoader());
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            String vaadinPackagePrefix = VaadinServlet.class.getPackage()
                    .getName();
            vaadinPackagePrefix = vaadinPackagePrefix.substring(0,
                    vaadinPackagePrefix.lastIndexOf('.'));
            if (name.startsWith(FrameworkUtil.class.getPackage().getName())) {
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

    private static final TestClassLoader classLoader = new TestClassLoader();

    public DisableOSGiRunner(Class<?> clazz)
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
