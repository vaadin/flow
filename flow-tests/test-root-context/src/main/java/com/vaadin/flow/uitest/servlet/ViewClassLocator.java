/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.uitest.servlet;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

import com.vaadin.flow.router.View;

public class ViewClassLocator {
    private LinkedHashMap<String, Class<? extends View>> views = new LinkedHashMap<>();
    private final ClassLoader classLoader;

    public ViewClassLocator(ClassLoader classLoader) {
        this.classLoader = classLoader;
        URL url = classLoader.getResource(".");
        if ("file".equals(url.getProtocol())) {
            String path = url.getPath();
            File testFolder = new File(path);

            // This scans parts of the classpath. If it becomes slow, we have to
            // remove this or make the scanning lazy
            try {
                findViews(testFolder, views);
                getLogger().info("Found " + views.size() + " views");
            } catch (IOException exception) {
                throw new RuntimeException(
                        "Unable to scan classpath to find views", exception);
            }
        } else {
            throw new RuntimeException(
                    "Could not find 'com' package using a file:// URL. Got URL: "
                            + url);
        }
    }

    private void findViews(File parent,
            LinkedHashMap<String, Class<? extends View>> packages)
            throws IOException {
        Path root = parent.toPath();
        Files.walkFileTree(parent.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path,
                    BasicFileAttributes attrs) throws IOException {
                tryLoadClass(root, path);
                return super.visitFile(path, attrs);
            }
        });

    }

    @SuppressWarnings("unchecked")
    private void tryLoadClass(Path root, Path path) {
        File file = path.toFile();
        if (file.getName().endsWith(".class")
                && !file.getName().contains("$")) {
            Path relative = root.relativize(path);
            String className = relative.toString()
                    .replace(File.separatorChar, '.').replace(".class", "");
            try {
                Class<?> cls = classLoader.loadClass(className);
                if (View.class.isAssignableFrom(cls)
                        && !Modifier.isAbstract(cls.getModifiers())) {
                    try {
                        // Only include views which have a no-arg
                        // constructor
                        Constructor<?> constructor = cls.getConstructor();
                        assert constructor != null;
                        views.put(cls.getSimpleName(),
                                (Class<? extends View>) cls);
                    } catch (Exception e) {
                        // InlineTemplate or similar
                    }
                }
            } catch (Exception e) {
                getLogger().warning("Unable to load class " + className);
            }
        }
    }

    private Logger getLogger() {
        return Logger.getLogger(ViewClassLocator.class.getName());
    }

    public Collection<Class<? extends View>> getAllViewClasses() {
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
