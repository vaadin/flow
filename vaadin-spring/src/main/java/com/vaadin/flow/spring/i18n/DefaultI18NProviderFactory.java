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

package com.vaadin.flow.spring.i18n;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.vaadin.flow.i18n.DefaultI18NProvider;
import com.vaadin.flow.i18n.I18NUtil;

/**
 * Factory for {@link DefaultI18NProvider}. Factory creates I18N provider for
 * locales based on all .properties files in /vaadin-i18n folder in the
 * classpath including JAR contents.
 */
public class DefaultI18NProviderFactory implements Serializable {

    private static final String DEFAULT_LOCATION_PATTERN = "classpath*:/vaadin-i18n/*.properties";

    /**
     * Get location pattern for {@link ResourcePatternResolver} to find
     * translation files for available locales. Default value finds all
     * .properties files in /vaadin-i18n folder in the classpath including JAR
     * contents.
     *
     * @return location pattern
     * @see ResourcePatternResolver
     * @see ResourcePatternResolver#getResources(String)
     */
    protected String getLocationPattern() {
        return DEFAULT_LOCATION_PATTERN;
    }

    /**
     * Creates new instance of {@link DefaultI18NProvider}.
     *
     * @return new instance of {@link DefaultI18NProvider}. Not null.
     */
    public DefaultI18NProvider create() {
        try {
            ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
            Resource[] translations = resourcePatternResolver
                    .getResources(getLocationPattern());
            if (translations.length > 0) {
                List<Locale> locales = I18NUtil.collectLocalesFromFileNames(
                        Arrays.stream(translations).map(Resource::getFilename)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()));
                return new DefaultI18NProvider(locales);
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(DefaultI18NProviderFactory.class)
                    .error("Unable to create DefaultI18NProvider instance.", e);
        }
        return new DefaultI18NProvider(Collections.emptyList());
    }
}
