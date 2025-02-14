/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.FeatureView", layout = ViewTestLayout.class)
public class FeatureView extends Div {

    private H2 h2;

    public FeatureView() {
        h2 = new H2();
        h2.setId("value");
        readFeatureFile();

        NativeButton checkFeatureFile = new NativeButton("Check feature file",
                click -> {
                    readFeatureFile();
                });
        checkFeatureFile.setId("check");

        NativeButton removeFeatures = new NativeButton("Remove feature file",
                click -> {
                    deleteFeatureFile();
                    readFeatureFile();
                });
        removeFeatures.setId("remove");

        add(h2, checkFeatureFile, removeFeatures);
    }

    private void readFeatureFile() {
        final File file = getFeatureFile();

        StringBuilder status = new StringBuilder();
        status.append("Feature file");
        if (file.exists()) {
            status.append(" exists with properties:");
            try (FileInputStream propertiesStream = new FileInputStream(file)) {
                Properties props = new Properties();

                if (propertiesStream != null) {
                    props.load(propertiesStream);
                }
                props.stringPropertyNames().forEach(
                        property -> status.append(" ").append(property));
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "Failed to read properties file from filesystem", e);
            }
        } else {
            status.append(" missing");
        }
        h2.setText(status.toString());
    }

    private void deleteFeatureFile() {
        getFeatureFile().delete();
    }

    private File getFeatureFile() {
        final VaadinContext context = VaadinSession.getCurrent().getService()
                .getContext();
        final ApplicationConfiguration configuration = ApplicationConfiguration
                .get(context);
        final File file = new File(configuration.getJavaResourceFolder(),
                FeatureFlags.PROPERTIES_FILENAME);
        return file;
    }
}
