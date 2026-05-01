/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.plugin.maven;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Temporary backward-compatibility support for deprecated Mojo property names.
 * <p>
 * All {@code @Parameter} properties in the Vaadin Maven plugin now use the
 * {@code vaadin.} prefix (e.g. {@code vaadin.node.download.root}). This class
 * detects when a user still passes the old non-prefixed property name (e.g.
 * {@code node.download.root}), applies the value to the Mojo field, and logs a
 * deprecation warning.
 * <p>
 * Type conversion from string to field type is handled manually for
 * {@code String}, {@code boolean}/{@code Boolean}, and {@code List<String>}.
 * Maven's {@code DefaultConverterLookup} cannot be used because it depends on
 * {@code org.eclipse.sisu.inject.Weak}, which is not exported from the
 * {@code maven.api} classrealm to plugins, and {@code ConverterLookup} is not
 * registered as a Plexus/Sisu component so it cannot be injected.
 * <p>
 * This class is intended to be removed once the deprecation period ends.
 */
final class DeprecatedPropertyResolver {

    static final String VAADIN_PREFIX = "vaadin.";
    static final String EXPRESSION_PREFIX = "${" + VAADIN_PREFIX;

    // Properties that always had the vaadin. prefix and were not part of the
    // migration. Their non-prefixed counterparts are ignored.
    private static final Set<String> NOT_MIGRATED = Set.of("vaadin.skip",
            "vaadin.skip.dev.bundle", "vaadin.ignoreVersionChecks",
            "vaadin.commercialWithBanner", "vaadin.ci.build",
            "vaadin.force.production.build",
            "vaadin.clean.build.frontend.files", "vaadin.path",
            "vaadin.useLit1", "vaadin.disableOptionalChaining");

    private DeprecatedPropertyResolver() {
    }

    /**
     * Inspects the Mojo descriptor parameters for properties with the
     * {@code vaadin.} prefix. For each such property, checks if the old
     * (non-prefixed) name is set in user or project properties. If the old
     * property is set but the new one is not, the value is applied to the Mojo
     * field and a deprecation warning is logged. If both are set, a warning is
     * logged that the old property is ignored.
     *
     * @param mojo
     *            the Mojo instance to resolve properties on
     * @param mojoDescriptor
     *            the Mojo descriptor containing parameter metadata
     * @param session
     *            the Maven session (for user properties)
     * @param project
     *            the Maven project (for project properties)
     */
    static void resolve(FlowModeAbstractMojo mojo,
            MojoDescriptor mojoDescriptor, MavenSession session,
            MavenProject project) {
        Map<String, Parameter> parameterMap = mojoDescriptor.getParameterMap();
        for (Parameter param : parameterMap.values()) {
            String expression = param.getExpression();
            if (expression == null || !expression.startsWith(EXPRESSION_PREFIX)
                    || !expression.endsWith("}")) {
                continue;
            }
            String propertyName = expression.substring(2,
                    expression.length() - 1);
            if (NOT_MIGRATED.contains(propertyName)) {
                continue;
            }
            String oldPropertyName = propertyName
                    .substring(VAADIN_PREFIX.length());
            String oldPropertyValue = lookupProperty(oldPropertyName, session,
                    project);
            if (oldPropertyValue == null) {
                continue;
            }
            String newPropertyValue = lookupProperty(propertyName, session,
                    project);
            if (newPropertyValue != null) {
                mojo.logWarn("Both '" + oldPropertyName + "' and '"
                        + propertyName + "' are set. The deprecated property '"
                        + oldPropertyName + "' will be ignored.");
            } else {
                mojo.logWarn("Property '" + oldPropertyName
                        + "' is deprecated, please use '" + propertyName
                        + "' instead.");
                try {
                    applyPropertyValue(mojo, param.getName(), oldPropertyValue);
                } catch (Exception e) {
                    mojo.logWarn("Failed to apply deprecated property '"
                            + oldPropertyName + "': " + e.getMessage());
                }
            }
        }
    }

    // Maven has already expanded any ${...} expressions in both user and
    // project properties by the time the mojo executes, so the returned
    // values are always resolved literals.
    private static String lookupProperty(String name, MavenSession session,
            MavenProject project) {
        Properties userProps = session.getUserProperties();
        String value = userProps.getProperty(name);
        if (value == null && project != null) {
            value = project.getProperties().getProperty(name);
        }
        return value;
    }

    /**
     * Converts the string value to the field's type and sets it via reflection.
     * <p>
     * Maven's {@code DefaultConverterLookup} cannot be instantiated from the
     * plugin classrealm (it depends on {@code org.eclipse.sisu.inject.Weak}
     * which is not exported) and {@code ConverterLookup} is not a registered
     * Plexus/Sisu component so it cannot be injected. A manual conversion is
     * used instead, applying the same conversions that Maven's Plexus/Sisu
     * configurator would ({@code Boolean.valueOf} for booleans, comma-split
     * with {@code split(",", -1)} for lists, identity for strings). Only types
     * currently used by {@code @Parameter}-annotated fields in
     * {@link FlowModeAbstractMojo} are covered: {@code String},
     * {@code boolean}/{@code Boolean}, and {@code List<String>}. If a new field
     * type is added, extend this method accordingly.
     */
    private static void applyPropertyValue(FlowModeAbstractMojo target,
            String fieldName, String value)
            throws ReflectiveOperationException {
        Field field = findField(target.getClass(), fieldName);
        if (field == null) {
            target.logDebug("Cannot find field '" + fieldName
                    + "' for deprecated" + " property migration, skipping.");
            return;
        }
        field.setAccessible(true);
        Class<?> type = field.getType();
        if (type == boolean.class || type == Boolean.class) {
            field.set(target, Boolean.parseBoolean(value));
        } else if (type == List.class) {
            field.set(target, List.of(value.split(",", -1)));
        } else {
            field.set(target, value);
        }
    }

    private static Field findField(Class<?> clazz, String name) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}
