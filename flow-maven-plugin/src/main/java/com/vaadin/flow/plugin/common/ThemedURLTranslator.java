/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.plugin.common;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;

/**
 * Translates the URLs according to the theme discovered in the project.
 *
 * @author Vaadin Ltd.
 */
public class ThemedURLTranslator extends ClassPathIntrospector {

    static final String VALUE = "value";

    private final Class<? extends AbstractTheme> themeClass;

    private final Function<String, File> fileFactory;

    private static final String TRANSLATE_URL_METHOD = "translateUrl";

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ThemedURLTranslator.class);

    static {
        assert Stream.of(AbstractTheme.class.getMethods()).map(Method::getName)
                .anyMatch(name -> name
                        .equals(TRANSLATE_URL_METHOD)) : "There is no method '"
                                + TRANSLATE_URL_METHOD + "' in the class "
                                + AbstractTheme.class.getName();
    }

    /**
     * Creates a new translator instance using the provided {@code fileFactory}
     * which produces the {@link File} on the filesystem by the URL and some
     * {@code otherIntrospector} to use its reflections tools.
     *
     * @param fileFactory
     *            file factory to produce a file on the fielsystem by the given
     *            URL
     * @param otherIntrospector
     *            another introspector whose reflection tools will be reused to
     *            find the theme
     */
    public ThemedURLTranslator(Function<String, File> fileFactory,
            ClassPathIntrospector otherIntrospector) {
        super(otherIntrospector);

        themeClass = findTheme();
        this.fileFactory = fileFactory;
    }

    /**
     * Returns the theme discovered in the project.
     * <p>
     * The theme may be {@code null} if there are no theme used in the project.
     *
     * @return the theme used in the project or {@code null} if there is no any
     *         theme
     */
    public Class<? extends AbstractTheme> getTheme() {
        return themeClass;
    }

    /**
     * Applies theme to the {@code urls} collection.
     *
     * @param urls
     *            a set of URLs
     * @return the URLs rewritten using theme
     */
    public Set<String> applyTheme(Set<String> urls) {
        if (themeClass == null) {
            return urls;
        }

        Set<String> resultingUrls = new HashSet<>();

        for (String url : urls) {
            String translatedUrl = translateUrl(url);
            if (sourceDirectoryHasFile(translatedUrl)) {
                LOGGER.debug(
                        "The URL '{}' has been translated "
                                + "to the url '{}' using theme '{}'",
                        url, translatedUrl, themeClass.getSimpleName());
                resultingUrls.add(translatedUrl);
            } else {
                LOGGER.debug("The theme '{}' gives '{}' as a "
                        + "translation for url '{}' but the file is not found on the filesystem",
                        themeClass.getSimpleName(), translatedUrl, url);
                resultingUrls.add(url);
            }
        }
        return resultingUrls;
    }

    private String translateUrl(String url) {
        // It's not possible to use AbstractTheme here as a type since it's
        // load by the different classloader.
        Object theme = ReflectTools.createInstance(themeClass);
        Method translateMethod = Stream.of(themeClass.getMethods())
                .filter(method -> method.getName().equals(TRANSLATE_URL_METHOD))
                .findFirst().get();
        try {
            return (String) translateMethod.invoke(theme, url);
        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException exception) {
            throw new RuntimeException(String.format(
                    "Unable to invoke '%s' on the theme instance with type '%s' and the given URL '%s'",
                    TRANSLATE_URL_METHOD, themeClass.getName(), url),
                    exception);
        }
    }

    private boolean sourceDirectoryHasFile(String url) {
        File file = fileFactory.apply(url);
        if (!file.exists()) {
            LOGGER.warn(
                    "The translated URL '{}' has no corresponding "
                            + "file on the filesystem,"
                            + " the file is addressed by path='{}'",
                    url, file.getPath());
            return false;
        }
        if (!file.isFile()) {
            LOGGER.warn(
                    "The translated URL '{}' corresponding "
                            + "path '{}' on the filesystem is not a file",
                    url, file.getPath());
            return false;
        }
        return true;
    }

    private Class<? extends AbstractTheme> findTheme() {
        Class<? extends Annotation> theme = loadClassInProjectClassLoader(
                Theme.class.getName());
        Map<Class<? extends AbstractTheme>, List<Class<?>>> themedComponents = getAnnotatedClasses(
                theme).filter(this::isNavigationTarget).collect(
                        Collectors.toMap(clazz -> getTheme(clazz, theme),
                                Collections::singletonList, this::mergeLists));
        if (themedComponents.size() > 1) {
            throw new IllegalStateException(
                    "Multiple themes are not supported, "
                            + themedComponents.entrySet().stream()
                                    .map(this::printThemeAnnotatedClasses)
                                    .collect(Collectors.joining(",\n")));
        }
        return themedComponents.size() == 1
                ? themedComponents.keySet().iterator().next()
                : null;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends AbstractTheme> getTheme(Class<?> clazz,
            Class<? extends Annotation> themeClass) {
        Annotation annotation = clazz.getAnnotation(themeClass);
        if (annotation == null) {
            return null;
        }
        return (Class<? extends AbstractTheme>) doInvokeAnnotationMethod(
                annotation, VALUE);
    }

    private List<Class<?>> mergeLists(List<Class<?>> list1,
            List<Class<?>> list2) {
        if (list1 instanceof ArrayList<?>) {
            list1.addAll(list2);
            return list1;
        }
        ArrayList<Class<?>> list = new ArrayList<>();
        list.addAll(list1);
        list.addAll(list2);
        return list;
    }

    private String printThemeAnnotatedClasses(
            Entry<Class<? extends AbstractTheme>, List<Class<?>>> entry) {
        StringBuilder builder;
        if (entry.getKey() == null) {
            builder = new StringBuilder("No theme ");
        } else {
            builder = new StringBuilder("Theme '");
            builder.append(entry.getKey()).append("'");
        }
        builder.append(
                " is discovered for classes which are navigation targets: ");
        builder.append(entry.getValue().stream().map(Class::getName)
                .collect(Collectors.joining(", ")));
        return builder.toString();
    }

    private boolean isNavigationTarget(Class<?> clazz) {
        return Stream.of(clazz.getAnnotations()).map(Annotation::annotationType)
                .map(Class::getName)
                .anyMatch(fqn -> fqn.equals(Route.class.getName())
                        || fqn.equals(RouteAlias.class.getName())
                        || fqn.equals(ParentLayout.class.getName()));
    }
}
