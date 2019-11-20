/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import java.net.URL;
import java.util.stream.Stream;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.project.MavenProject;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.scanner.ReflectionsClassFinder;
import com.vaadin.flow.utils.FlowFileUtils;

/**
 * Utility methods used by all goals.
 *
 * @since 2.0
 */
public class FlowPluginFrontendUtils {

    /**
     * Additionally include compile-time-only dependencies matching the pattern.
     */
    private static final String INCLUDE_FROM_COMPILE_DEPS_REGEX =
            ".*(/|\\\\)(portlet-api|javax\\.servlet-api)-.+jar$";

    private FlowPluginFrontendUtils() {
    }

    /**
     * Gets a <code>ClassFinder</code> for the maven project.
     *
     * @param project
     *            a maven project instance used as source for the
     *            <code>ClassFinder</code>.
     * @return a <code>ClassFinder</code> instance.
     */
    public static ClassFinder getClassFinder(MavenProject project) {
        final Stream<String> classpathElements;
        try {
            classpathElements = Stream.concat(
                    project.getRuntimeClasspathElements().stream(),
                    project.getCompileClasspathElements().stream()
                        .filter(s -> s.matches(INCLUDE_FROM_COMPILE_DEPS_REGEX)));
        } catch (DependencyResolutionRequiredException e) {
            throw new IllegalStateException(String.format(
                    "Failed to retrieve runtime classpath elements from project '%s'",
                    project), e);
        }
        URL[] urls = classpathElements.distinct().map(File::new)
                .map(FlowFileUtils::convertToUrl).toArray(URL[]::new);

        return new ReflectionsClassFinder(urls);
    }

}
