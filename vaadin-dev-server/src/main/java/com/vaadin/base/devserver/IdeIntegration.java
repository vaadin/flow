package com.vaadin.base.devserver;

import java.io.File;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentReference;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.ComponentTracker;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ComponentMapping;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

/**
 * Integration with IDEs for development mode.
 */
public class IdeIntegration {

    private ApplicationConfiguration configuration;

    /**
     * Creates a new integration with the given configuration.
     */
    public IdeIntegration(ApplicationConfiguration configuration) {
        this.configuration = configuration;
        if (configuration.isProductionMode()) {
            getLogger().error(getClass().getSimpleName()
                    + " should never be created in production mode");
        }

    }

    /**
     * Try to open the location where the given component was created.
     *
     * @param session
     *            the user session
     * @param componentReference
     *            a reference to the component, from the browser
     */
    public void showComponentCreateInIde(VaadinSession session,
            ComponentReference componentReference) {
        session.access(() -> internalShowComponentCreateInIde(
                ComponentUtil.findComponent(session, componentReference)));
    }

    private void internalShowComponentCreateInIde(Component component) {
        StackTraceElement location = ComponentTracker.findCreate(component);
        if (location == null) {
            getLogger().error("Unable to find the location where the component "
                    + component.getClass().getName() + " was created");
            return;
        }

        String cls = location.getClassName();
        String filename = location.getFileName();

        if (cls.endsWith(filename.replace(".java", ""))) {
            File f = configuration.getProjectFolder().toPath().toAbsolutePath()
                    .toFile();
            f = new File(f, "src");
            f = new File(f, "main");
            f = new File(f, "java");
            f = new File(f, cls.replace(".", "/") + ".java");
            String absoluteFilename = f.getAbsolutePath();
            if (!f.exists()) {
                getLogger().error("Unable to find file in " + absoluteFilename);
                return;
            }

            if (!OpenInIde.openFile(absoluteFilename,
                    location.getLineNumber())) {
                // Failed to open in IDE so print the file and line info.
                // Either an IDE makes it clickable or you can copy the file
                // info
                System.out.println(location);
            }
        } else {
            System.out.println(location);
        }

    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(IdeIntegration.class);
    }

}
