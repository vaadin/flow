/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring;

import java.io.File;
import java.io.Serializable;

import org.springframework.boot.devtools.classpath.ClassPathChangedEvent;
import org.springframework.context.ApplicationListener;

import com.vaadin.flow.function.SerializableConsumer;

/**
 * Listens to {@link ClassPathChangedEvent} events and fires a Vaadin- specific
 * {@link ReloadEvent}.
 */
class ReloadListener
        implements ApplicationListener<ClassPathChangedEvent>, Serializable {

    private final SerializableConsumer<ReloadEvent> callback;

    public ReloadListener(SerializableConsumer<ReloadEvent> callback) {
        this.callback = callback;
    }

    @Override
    public void onApplicationEvent(ClassPathChangedEvent event) {
        ReloadEvent reloadEvent = new ReloadEvent();

        event.getChangeSet().forEach(changedFiles -> {
            changedFiles.getFiles().forEach(file -> {
                String className = convertToClassName(file.getRelativeName());
                if (className != null) {
                    switch (file.getType()) {
                    case ADD:
                        reloadEvent.getAddedClasses().add(className);
                        break;
                    case DELETE:
                        reloadEvent.getRemovedClasses().add(className);
                        break;
                    case MODIFY:
                        reloadEvent.getChangedClasses().add(className);
                        break;
                    }
                }
            });
        });
        callback.accept(reloadEvent);
    }

    private String convertToClassName(String fileName) {
        if (fileName.endsWith(".class")) {
            String name = fileName.replace(".class", "").replace('/', '.');
            if (File.separatorChar != '/') {
                return name.replace(File.separatorChar, '.');
            }
            return name;
        } else {
            return null;
        }
    }
}