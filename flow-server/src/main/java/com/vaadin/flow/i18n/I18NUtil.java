/*
 * Copyright 2000-2023 Vaadin Ltd.
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

package com.vaadin.flow.i18n;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.vaadin.flow.i18n.DefaultI18NProvider.BUNDLE_FILENAME;

public final class I18NUtil {

    public static final String PROPERTIES_SUFFIX = ".properties";

    public static boolean containsDefaultTranslation() {
        URL resource = getClassLoader()
                .getResource(DefaultI18NProvider.BUNDLE_FOLDER + "/"
                        + DefaultI18NProvider.BUNDLE_FILENAME
                        + PROPERTIES_SUFFIX);
        if (resource == null) {
            return false;
        }
        return true;
    }

    public static List<Locale> getDefaultTranslationLocales() {
        List<Locale> locales = new ArrayList<>();

        URL resource = getClassLoader()
                .getResource(DefaultI18NProvider.BUNDLE_FOLDER);
        if (resource == null) {
            return locales;
        }
        File bundleFolder = new File(resource.getFile());
        if (bundleFolder.exists() && bundleFolder.isDirectory()) {
            List<File> listedFiles = Arrays.stream(bundleFolder.listFiles())
                    .filter(file -> file.isFile())
                    .filter(file -> file.getName().endsWith(PROPERTIES_SUFFIX))
                    .collect(Collectors.toList());
            if (!listedFiles.isEmpty()) {
                for (File file : listedFiles) {
                    String name = file.getName();

                    if (!name.contains("_")) {
                        // This is the default bundle and that doesn't have a
                        // locale
                        continue;
                    }
                    String langCode = name.substring(
                            BUNDLE_FILENAME.length() + 1,
                            name.lastIndexOf('.'));
                    String[] langParts = langCode.split("_");
                    if (langParts.length == 1) {
                        locales.add(new Locale(langParts[0]));
                    } else if (langParts.length == 2) {
                        locales.add(new Locale(langParts[0], langParts[1]));
                    } else if (langParts.length == 3) {
                        locales.add(new Locale(langParts[0], langParts[1],
                                langParts[2]));
                    }
                }
            }
        }
        return locales;
    }

    protected static ClassLoader getClassLoader() {
        return I18NUtil.class.getClassLoader();
    }
}
