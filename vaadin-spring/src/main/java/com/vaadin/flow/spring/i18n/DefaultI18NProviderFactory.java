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
package com.vaadin.flow.spring.i18n;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotatedTypeMetadata;

import com.vaadin.flow.i18n.DefaultI18NProvider;
import com.vaadin.flow.i18n.I18NUtil;

/**
 * Factory for {@link DefaultI18NProvider}. Factory creates I18N provider for
 * locales based on all .properties files in /vaadin-i18n folder in the
 * classpath including JAR contents.
 */
public class DefaultI18NProviderFactory implements Condition, Serializable {

    /**
     * Default location pattern to be used with {@link ResourcePatternResolver}.
     */
    public static final String DEFAULT_LOCATION_PATTERN = "classpath*:/vaadin-i18n/*.properties";

    /**
     * Creates new instance of {@link DefaultI18NProvider} with the given
     * location pattern.
     *
     * @param locationPattern
     *            location pattern for {@link ResourcePatternResolver} to find
     *            translation files for available locales.
     * @return new instance of {@link DefaultI18NProvider}. May be null.
     * @see ResourcePatternResolver
     * @see ResourcePatternResolver#getResources(String)
     */
    public static DefaultI18NProvider create(String locationPattern) {
        try {
            Resource[] translations = getTranslationResources(locationPattern);
            if (translations.length > 0) {
                List<Locale> locales = I18NUtil.collectLocalesFromFileNames(
                        Arrays.stream(translations).map(Resource::getFilename)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()));
                // Makes use of the RestartClassLoader to invalidate the
                // ResourceBundle cache on SpringBoot application dev mode
                // reload. See https://github.com/vaadin/hilla/issues/2554
                ClassLoader classLoader = Thread.currentThread()
                        .getContextClassLoader();
                return new DefaultI18NProvider(locales, classLoader);
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(DefaultI18NProviderFactory.class)
                    .error("Unable to create DefaultI18NProvider instance.", e);
        }
        return null;
    }

    @Override
    public boolean matches(ConditionContext context,
            AnnotatedTypeMetadata metadata) {
        try {
            String locationPattern = context.getEnvironment().getProperty(
                    "vaadin.i18n.location-pattern", String.class,
                    DEFAULT_LOCATION_PATTERN);
            Resource[] translations = getTranslationResources(locationPattern);
            return translations.length > 0;
        } catch (IOException e) {
            LoggerFactory.getLogger(DefaultI18NProviderFactory.class).error(
                    "Unable to detect if DefaultI18NProvider instance is needed.",
                    e);
            return false;
        }
    }

    private static Resource[] getTranslationResources(String locationPattern)
            throws IOException {
        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        return resourcePatternResolver
                .getResources(locationPattern != null ? locationPattern
                        : DEFAULT_LOCATION_PATTERN);
    }
}
