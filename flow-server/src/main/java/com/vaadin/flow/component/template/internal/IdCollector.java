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
package com.vaadin.flow.component.template.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.ReflectTools;

/**
 * Collects information of {@link Id @Id} mapped fields in a template class.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 */
public class IdCollector {
    private static final String DEPRECATED_ID = "com.vaadin.flow.component.polymertemplate.Id";
    private final Map<String, String> tagById = new HashMap<>();
    private final Map<Field, String> idByField = new HashMap<>();
    private final Map<String, Map<String, String>> attributesById = new HashMap<>();
    private Element templateRoot;
    private Class<?> templateClass;
    private String templateFile;

    /**
     * Creates a collector the the given template.
     *
     * @param templateClass
     *            the template class, containing the {@code @Id} fields
     * @param templateFile
     *            The name of the file containing the template or
     *            <code>null</code> if not available {@code null}
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
     * {@link com.vaadin.flow.component.template.Id @Id}.
     *
     * @param notInjectableElementIds
     *            ids which cannot be injected
     */
    public void collectInjectedIds(Set<String> notInjectableElementIds) {
        collectInjectedIds(templateClass, notInjectableElementIds);
    }

    private void collectInjectedIds(Class<?> cls,
            Set<String> notInjectableElementIds) {
        if (!Component.class.equals(cls.getSuperclass())) {
            // Parent fields
            collectInjectedIds(cls.getSuperclass(), notInjectableElementIds);
        }

        Stream.of(cls.getDeclaredFields()).filter(field -> !field.isSynthetic())
                .forEach(field -> collectedInjectedId(field,
                        notInjectableElementIds));
    }

    private void collectedInjectedId(Field field,
            Set<String> notInjectableElementIds) {
        String id = getId(field).orElse(null);
        if (id == null) {
            return;
        }
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

        if (!collectElementData(id, field)) {
            throw new IllegalStateException(String.format(
                    "There is no element with "
                            + "id='%s' in the template file '%s'. Cannot map it using @%s",
                    id, templateFile,
                    com.vaadin.flow.component.template.Id.class
                            .getSimpleName()));
        }
    }

    private Optional<String> getId(Field field) {
        Optional<Annotation> deprecatedId = ReflectTools.getAnnotation(field,
                DEPRECATED_ID);
        if (deprecatedId.isPresent()) {
            return Optional.of(ReflectTools
                    .getAnnotationMethodValue(deprecatedId.get(), "value")
                    .toString());
        }
        return AnnotationReader.getAnnotationFor(field, Id.class)
                .map(com.vaadin.flow.component.template.Id::value);
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
    private boolean collectElementData(String id, Field field) {
        idByField.put(field, id);
        if (templateRoot != null) {
            // The template is available for parsing so check up front if the id
            // exists
            Optional<Element> element = Optional
                    .ofNullable(templateRoot.getElementById(id));
            Optional<String> tagName = element
                    .map(org.jsoup.nodes.Element::tagName);
            if (element.isPresent()) {
                Element domElement = element.get();
                tagById.put(id, tagName.get());
                fetchAttributes(id, domElement.attributes());
            }

            return element.isPresent();
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

    /**
     * Gets a map from field ids to their parsed attributes values.
     *
     * @return a map from field ids to their parsed attributes values
     */
    public Map<String, Map<String, String>> getAttributes() {
        return Collections.unmodifiableMap(attributesById);
    }

    private void fetchAttributes(String id, Attributes attributes) {
        if (attributes.size() == 0) {
            return;
        }
        Map<String, String> data = getAttributeData(id);
        attributes.forEach(attr -> setAttributeData(attr, data));
    }

    private void setAttributeData(Attribute attribute,
            Map<String, String> data) {
        if (isBooleanAttribute(attribute)) {
            data.put(attribute.getKey(), Boolean.TRUE.toString());
        } else {
            data.put(attribute.getKey(), attribute.getValue());
        }
    }

    private boolean isBooleanAttribute(Attribute attribute) {
        return attribute.getKey().equals(attribute.toString());
    }

    private Map<String, String> getAttributeData(String id) {
        return attributesById.computeIfAbsent(id, key -> new HashMap<>());
    }

}
