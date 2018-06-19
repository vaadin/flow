package com.vaadin.flow.server.startup;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import java.io.IOException;
import java.io.Serializable;
import java.util.Set;

import com.vaadin.flow.server.Manifest;

@HandlesTypes({ Manifest.class })
public class ManifestInitializer implements ServletContainerInitializer,
        Serializable {
    @Override
    public void onStartup(Set<Class<?>> classSet, ServletContext servletContext)
            throws ServletException {
        Manifest manifest = null;
        for (Class<?> clazz : classSet) {
            if (clazz.isAnnotationPresent(Manifest.class)) {
                // more checking needed?
                // Is it from "master layout"?
                // Are there multiple manifests?
                manifest = clazz.getAnnotation(Manifest.class);
                break;
            }
        }

        try {
            ManifestRegistry.initRegistry(servletContext, manifest);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

}
