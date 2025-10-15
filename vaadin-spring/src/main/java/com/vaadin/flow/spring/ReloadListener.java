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
