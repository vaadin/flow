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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.hummingbird.router.View;
import com.vaadin.hummingbird.uitest.ui.EmptyUI;

public class ViewClassLocator {
    private LinkedHashSet<String> packages = new LinkedHashSet<String>();

    public ViewClassLocator(ClassLoader classLoader) {
        String str = EmptyUI.class.getName().replace('.', '/') + ".class";
        URL url = classLoader.getResource(str);
        if ("file".equals(url.getProtocol())) {
            String path = url.getPath();
            try {
                path = new URI(path).getPath();
            } catch (URISyntaxException e) {
                getLogger().log(Level.FINE, "Failed to decode url", e);
            }
            File testFolder = new File(path).getParentFile().getParentFile();
            addDirectories(testFolder, packages,
                    EmptyUI.class.getPackage().getName());

        }
    }

    private void addDirectories(File parent, LinkedHashSet<String> packages,
            String parentPackage) {
        packages.add(parentPackage);

        for (File f : parent.listFiles()) {
            if (f.isDirectory()) {
                String newPackage = parentPackage + "." + f.getName();
                addDirectories(f, packages, newPackage);
            }
        }
    }

    private Logger getLogger() {
        return Logger.getLogger(ViewClassLocator.class.getName());
    }

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
            //
            for (String pkg : packages) {
                try {
                    return (Class<? extends View>) getClass().getClassLoader()
                            .loadClass(pkg + "." + baseName);
                } catch (ClassNotFoundException ee) {
                    // Ignore as this is expected for many packages
                } catch (Exception e2) {
                    getLogger().log(Level.FINE,
                            "Failed to find application class " + pkg + "."
                                    + baseName,
                            e2);
                }
            }
        }
        throw new ClassNotFoundException(baseName);
    }
}