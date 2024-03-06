/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
