/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.servlet;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

import com.vaadin.hummingbird.router.View;
import com.vaadin.hummingbird.uitest.ui.EmptyUI;

public class ViewClassLocator {
    private LinkedHashMap<String, Class<? extends View>> views = new LinkedHashMap<>();
    private final ClassLoader classLoader;

    public ViewClassLocator(ClassLoader classLoader) {
        this.classLoader = classLoader;
        String str = EmptyUI.class.getName().replace('.', '/') + ".class";
        URL url = classLoader.getResource(str);
        if ("file".equals(url.getProtocol())) {
            String path = url.getPath();
            File testFolder = new File(path).getParentFile();

            // This scans parts of the classpath. If it becomes slow, we have to
            // remove this or make the scanning lazy
            findViews(testFolder, views, EmptyUI.class.getPackage().getName());
            getLogger().info("Found " + views.size() + " views");
        } else {
            throw new RuntimeException(
                    "Could not find EmptyUI.class using a file:// URL. Got URL: "
                            + url);
        }
    }

    @SuppressWarnings("unchecked")
    private void findViews(File parent,
            LinkedHashMap<String, Class<? extends View>> packages,
            String parentPackage) {

        for (File f : parent.listFiles()) {
            if (f.isDirectory()) {
                String newPackage = parentPackage + "." + f.getName();
                findViews(f, views, newPackage);
            } else if (f.getName().endsWith(".class")
                    && !f.getName().contains("$")) {
                String className = parentPackage + "."
                        + f.getName().replace(".class", "");
                try {
                    Class<?> cls = classLoader.loadClass(className);
                    if (View.class.isAssignableFrom(cls)
                            && !Modifier.isAbstract(cls.getModifiers())) {
                        views.put(cls.getSimpleName(),
                                (Class<? extends View>) cls);
                    }
                } catch (Exception e) {
                    getLogger().warning("Unable to load class " + className);
                }
            }
        }
    }

    private Logger getLogger() {
        return Logger.getLogger(ViewClassLocator.class.getName());
    }

    Collection<Class<? extends View>> getAllViewClasses() {
        return views.values();
    }

    @SuppressWarnings("unchecked")
    public Class<? extends View> findViewClass(String fullOrSimpleName)
            throws ClassNotFoundException {
        if (fullOrSimpleName == null) {
            return null;
        }
        String baseName = fullOrSimpleName;
        try {
            return (Class<? extends View>) getClass().getClassLoader()
                    .loadClass(baseName);
        } catch (Exception e) {
        }
        if (views.containsKey(fullOrSimpleName)) {
            return views.get(fullOrSimpleName);
        }

        throw new ClassNotFoundException(baseName);
    }
}
