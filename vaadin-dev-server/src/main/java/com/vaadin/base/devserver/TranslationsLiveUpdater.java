/*
 * Copyright 2000-2024 Vaadin Ltd.
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
package com.vaadin.base.devserver;

import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.server.VaadinContext;
import elemental.json.Json;
import elemental.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

/**
 * Watches the default translations resource folder for changes, and pushes the
 * new version to the browser.
 *
 */
public class TranslationsLiveUpdater implements Closeable {

    private FileWatcher watcher;

    /**
     * Starts watching the translations resource folder.
     *
     * @param context
     *            the current context
     */
    public TranslationsLiveUpdater(VaadinContext context) {
        File translationsFolder = new File("src/main/resources/vaadin-i18n");
        Optional<BrowserLiveReload> liveReload = BrowserLiveReloadAccessor
                .getLiveReloadFromContext(context);
        if (liveReload.isPresent()) {
            try {
                watcher = new FileWatcher(file -> handleFileUpdate(file, liveReload.get()), translationsFolder);
                watcher.start();
                getLogger().debug("Watching {} for translation changes", translationsFolder);
            } catch (IOException e) {
                getLogger().error("Unable to watch {} for translation changes",
                        translationsFolder, e);
            }
        } else {
            getLogger().error(
                    "Browser live reload is not available. Unable to watch {} for translation changes",
                    translationsFolder);
        }
    }

    private void handleFileUpdate(File file, BrowserLiveReload liveReload) {
        if (!file.getName().endsWith(".properties")) {
            return;
        }

        Properties properties = new Properties();
        try {
            properties.load(file.toURI().toURL().openStream());
        } catch (IOException e) {
            getLogger().error("Unable to load properties from {}", file, e);
            return;
        }

        JsonObject json = Json.createObject();
        properties.stringPropertyNames().forEach(
                key -> json.put(key, properties.getProperty(key)));

        liveReload.update(file.getPath(), json.toJson());
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

    /**
     * Stops watching the folder and cleans up resources.
     */
    @Override
    public void close() throws IOException {
        watcher.stop();
        watcher = null;
    }

}
