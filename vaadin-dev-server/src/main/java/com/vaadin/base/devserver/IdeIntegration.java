package com.vaadin.base.devserver;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentReference;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.internal.ComponentTracker;
import com.vaadin.flow.internal.UsageStatistics;
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
     * @param componentReference
     *            a reference to the component, from the browser
     */
    public void showComponentCreateInIde(
            ComponentReference componentReference) {
                componentReference.getSession().access(() -> {
            UsageStatistics.markAsUsed("flow/showComponentCreateInIde", null);
            Component component = ComponentUtil.findComponent(
                    componentReference);
            internalShowInIde(component,
                    ComponentTracker.findCreate(component));
        });
    }

    /**
     * Try to open the location where the given component was attached.
     *
     * @param componentReference
     *            a reference to the component, from the browser
     */
    public void showComponentAttachInIde(
            ComponentReference componentReference) {
                componentReference.getSession().access(() -> {
            UsageStatistics.markAsUsed("flow/showComponentAttachInIde", null);
            Component component = ComponentUtil.findComponent(
                    componentReference);
            internalShowInIde(component,
                    ComponentTracker.findAttach(component));
        });
    }

    private void internalShowInIde(Component component,
            StackTraceElement location) {
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
