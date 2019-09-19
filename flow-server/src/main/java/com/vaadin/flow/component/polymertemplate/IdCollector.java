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
package com.vaadin.flow.component.polymertemplate;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.jsoup.nodes.Element;

import com.vaadin.flow.internal.AnnotationReader;

/**
 * Collects information of {@link Id @Id} mapped fields in a template class.
 *
 * @since 2.0
 */
public class IdCollector {
    private final Map<String, String> tagById = new HashMap<>();
    private final Map<Field, String> idByField = new HashMap<>();
    private Element templateRoot;
    private Class<?> templateClass;
    private String templateFile;

    /**
     * Creates a collector the the given template.
     *
     * @param templateClass
     *            the template class, containing the {@code @Id} fields
     * @param templateFile
     *            The name of the file containing the template
     * @param templateRoot
     *            The root element of the template or <code>null</code> if not
     *            available
     */
    public IdCollector(Class<?> templateClass, String templateFile,
            Element templateRoot) {
        this.templateClass = templateClass;
        this.templateFile = templateFile;
        this.templateRoot = templateRoot;
    }

    /**
     * Scans the given template class and finds fields mapped using
     * {@link Id @Id}.
     *
     * @param notInjectableElementIds
     *            ids which cannot be injected
     */
    public void collectInjectedIds(Set<String> notInjectableElementIds) {
        collectInjectedIds(templateClass, notInjectableElementIds);
    }

    private void collectInjectedIds(Class<?> cls,
            Set<String> notInjectableElementIds) {
        if (!AbstractTemplate.class.equals(cls.getSuperclass())) {
            // Parent fields
            collectInjectedIds(cls.getSuperclass(), notInjectableElementIds);
        }

        Stream.of(cls.getDeclaredFields()).filter(field -> !field.isSynthetic())
                .forEach(field -> collectedInjectedId(field,
                        notInjectableElementIds));
    }

    private void collectedInjectedId(Field field,
            Set<String> notInjectableElementIds) {
        Optional<Id> idAnnotation = AnnotationReader.getAnnotationFor(field,
                Id.class);
        if (!idAnnotation.isPresent()) {
            return;
        }
        String id = idAnnotation.get().value();
        boolean emptyValue = id.isEmpty();
        if (emptyValue) {
            id = field.getName();
        }
        if (notInjectableElementIds.contains(id)) {
            throw new IllegalStateException(String.format(
                    "Class '%s' contains field '%s' annotated with @Id%s. "
                            + "Corresponding element was found in a sub template, "
                            + "for which injection is not supported.",
                    templateClass.getName(), field.getName(),
                    emptyValue
                            ? " without value (so the name of the field should match the id of an element in the template)"
                            : "(\"" + id + "\")"));
        }

        if (!addTagName(id, field)) {
            throw new IllegalStateException(String.format(
                    "There is no element with "
                            + "id='%s' in the template file '%s'. Cannot map it using @%s",
                    id, templateFile, Id.class.getSimpleName()));
        }
    }

    /**
     * Stores mapping between the given id and field.
     *
     * @param id
     *            the id value
     * @param field
     *            the Java field
     * @return <code>false</code> if the mapping did not pass validation,
     *         <code>true</code> otherwise
     */
    private boolean addTagName(String id, Field field) {
        idByField.put(field, id);
        if (templateRoot != null) {
            // The template is available for parsing so check up front if the id
            // exists
            Optional<String> tagName = Optional
                    .ofNullable(templateRoot.getElementById(id))
                    .map(org.jsoup.nodes.Element::tagName);
            if (tagName.isPresent()) {
                tagById.put(id, tagName.get());
            }

            return tagName.isPresent();
        }

        return true;
    }

    /**
     * Gets a map from fields to their ids.
     *
     * @return a map from fields to the ids
     */
    public Map<Field, String> getIdByField() {
        return idByField;
    }

    /**
     * Gets a map from field ids to their component tags.
     *
     * @return a map from field ids to their component tags
     */
    public Map<String, String> getTagById() {
        return tagById;
    }

}
