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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeVisitor;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.polymertemplate.TemplateParser.TemplateData;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.server.VaadinService;

import elemental.json.Json;
import elemental.json.JsonArray;

/**
 * Template data analyzer which produces immutable data required for template
 * initializer using provided template class and a parser.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class TemplateDataAnalyzer {

    // {{propertyName}} or {{propertyName::event}}
    private static final Pattern TWO_WAY_BINDING_PATTERN = Pattern
            .compile("\\s*\\{\\{([^}:]*)(::[^}]*)?\\}\\}\\s*");

    private final Class<? extends PolymerTemplate<?>> templateClass;
    private final TemplateParser parser;
    private final String tag;
    private final VaadinService service;

    private final Collection<SubTemplateData> subTemplates = new ArrayList<>();
    private final Set<String> twoWayBindingPaths = new HashSet<>();
    private final Set<String> notInjectableElementIds = new HashSet<>();

    private String htmlImportUri;

    /**
     * Three argument consumer.
     *
     * @author Vaadin Ltd
     * @since 1.0
     *
     */
    @FunctionalInterface
    public interface InjectableFieldCunsumer {

        /**
         * Performs this operation on the given arguments.
         * <p>
         * The arguments are: the field declared in a template class, the
         * identifier of the element inside the HTML template file, the element
         * tag.
         *
         * @param field
         *            the field declared in a template class
         * @param id
         *            the element id
         * @param tag
         *            the element tag
         */
        void apply(Field field, String id, String tag);
    }

    /**
     * Immutable parser data which may be stored in cache.
     */
    public static class ParserData {

        private final Map<String, String> tagById;
        private final Map<Field, String> idByField;

        private final Set<String> twoWayBindingPaths;

        private final Collection<SubTemplateData> subTemplates;

        /**
         * Constructs an immutable data object with the given information.
         *
         * @param fields
         *            a map of fields to their ids
         * @param tags
         *            a map of ids to their tags
         * @param twoWayBindings
         *            the properties which support two way binding
         * @param subTemplates
         *            data for sub templates
         */
        public ParserData(Map<Field, String> fields, Map<String, String> tags,
                Set<String> twoWayBindings,
                Collection<SubTemplateData> subTemplates) {
            tagById = Collections.unmodifiableMap(tags);
            idByField = Collections.unmodifiableMap(fields);
            twoWayBindingPaths = Collections.unmodifiableSet(twoWayBindings);
            this.subTemplates = Collections
                    .unmodifiableCollection(subTemplates);
        }

        /**
         * Applies the given consumer to each mapped field.
         *
         * @param consumer
         *            the consumer to call for each mapped field
         */
        public void forEachInjectedField(InjectableFieldCunsumer consumer) {
            idByField.forEach(
                    (field, id) -> consumer.apply(field, id, tagById.get(id)));
        }

        Set<String> getTwoWayBindingPaths() {
            return twoWayBindingPaths;
        }

        void forEachSubTemplate(Consumer<SubTemplateData> dataConsumer) {
            subTemplates.forEach(dataConsumer);
        }
    }

    static class SubTemplateData {
        private final String id;
        private final String tag;
        private final JsonArray path;

        SubTemplateData(String id, String tag, JsonArray path) {
            this.id = id;
            this.tag = tag;
            this.path = path;
        }

        String getId() {
            return id;
        }

        String getTag() {
            return tag;
        }

        JsonArray getPath() {
            return path;
        }
    }

    /**
     * Create an instance of the analyzer using the {@code templateClass} and
     * the template {@code parser}.
     *
     * @param templateClass
     *            a template type
     * @param parser
     *            a template parser
     * @param service
     *            the related service instance
     */
    TemplateDataAnalyzer(Class<? extends PolymerTemplate<?>> templateClass,
            TemplateParser parser, VaadinService service) {
        this.templateClass = templateClass;
        this.parser = parser;
        this.service = service;
        tag = getTag(templateClass);
    }

    /**
     * Gets the template data for the template initializer.
     *
     * @return the template data
     */
    ParserData parseTemplate() {
        TemplateData templateData = parser.getTemplateContent(templateClass,
                tag, service);
        Element templateRoot = templateData.getTemplateElement();
        htmlImportUri = templateData.getHtmlImportUri();
        Elements templates = templateRoot.getElementsByTag("template");
        for (org.jsoup.nodes.Element element : templates) {
            org.jsoup.nodes.Element parent = element.parent();
            if (parent != null && tag.equals(parent.id())) {
                inspectCustomElements(element, element);

                inspectTwoWayBindings(element);
            }
        }
        IdCollector idExtractor = new IdCollector(templateClass, htmlImportUri,
                templateRoot);
        idExtractor.collectInjectedIds(notInjectableElementIds);
        return readData(idExtractor);
    }

    private void inspectTwoWayBindings(org.jsoup.nodes.Element element) {
        Matcher matcher = TWO_WAY_BINDING_PATTERN.matcher("");
        element.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                // Two way bindings should only be in property bindings, not
                // inside text content.
                for (Attribute attribute : node.attributes()) {
                    String value = attribute.getValue();

                    // It is legal for attributes in templates not to have
                    // values,
                    // which is a short form for giving the attribute the value
                    // 'true'.
                    // These attributes don't contain bindings (they're just
                    // 'true'), so we
                    // skip them.
                    if (value == null) {
                        continue;
                    }

                    matcher.reset(value);
                    if (matcher.matches()) {
                        String path = matcher.group(1);
                        addTwoWayBindingPath(path);
                    }
                }
            }

            @Override
            public void tail(Node node, int depth) {
                // Nop
            }
        });
    }

    private ParserData readData(IdCollector idExtractor) {
        return new ParserData(idExtractor.getIdByField(),
                idExtractor.getTagById(), twoWayBindingPaths, subTemplates);
    }

    private String getTag(Class<? extends PolymerTemplate<?>> clazz) {
        Optional<String> tagNameAnnotation = AnnotationReader
                .getAnnotationFor(clazz, Tag.class).map(Tag::value);
        assert tagNameAnnotation.isPresent();
        return tagNameAnnotation.get();
    }

    private void inspectCustomElements(org.jsoup.nodes.Element childElement,
            org.jsoup.nodes.Element templateRoot) {
        if (isInsideTemplate(childElement, templateRoot)) {
            storeNotInjectableElementId(childElement);
        }

        collectCustomElement(childElement, templateRoot);
        childElement.children()
                .forEach(child -> inspectCustomElements(child, templateRoot));
    }

    private void collectCustomElement(org.jsoup.nodes.Element element,
            org.jsoup.nodes.Element templateRoot) {
        String tag = element.tagName();

        if (TemplateInitializer.getUsesClass(templateClass, tag).isPresent()) {
            if (isInsideTemplate(element, templateRoot)) {
                throw new IllegalStateException(String.format(
                        "Couldn't parse the template '%s': "
                                + "sub-templates are not supported. Sub-template found: %n'%s'",
                        htmlImportUri, element.toString()));
            }

            String id = element.hasAttr("id") ? element.attr("id") : null;
            JsonArray path = getPath(element, templateRoot);
            addSubTemplate(id, tag, path);
        }
    }

    private JsonArray getPath(org.jsoup.nodes.Element element,
            org.jsoup.nodes.Element templateRoot) {
        List<Integer> path = new ArrayList<>();
        org.jsoup.nodes.Element current = element;
        while (!current.equals(templateRoot)) {
            org.jsoup.nodes.Element parent = current.parent();
            path.add(indexOf(parent, current));
            current = parent;
        }
        JsonArray array = Json.createArray();
        for (int i = 0; i < path.size(); i++) {
            array.set(i, path.get(path.size() - i - 1));
        }
        return array;
    }

    /**
     * Returns the index of the {@code child} in the collection of
     * {@link org.jsoup.nodes.Element} children of the {@code parent} ignoring
     * "style" elements.
     * <p>
     * "style" elements are handled differently depending on ES5/ES6. Also
     * "style" tag can be moved on the top in the resulting client side DOM
     * regardless of its initial position (e.g. Chrome does this).
     *
     * @param parent
     *            the parent of the {@code child}
     * @param child
     *            the child element whose index is calculated
     * @return the index of the {@code child} in the {@code parent}
     */
    private static int indexOf(org.jsoup.nodes.Element parent,
            org.jsoup.nodes.Element child) {
        Elements children = parent.children();
        int index = -1;
        for (org.jsoup.nodes.Element nextChild : children) {
            if (!"style".equals(nextChild.tagName())) {
                index++;
            }
            if (nextChild.equals(child)) {
                break;
            }
        }
        return index;
    }

    private void storeNotInjectableElementId(org.jsoup.nodes.Element element) {
        String id = element.id();
        if (id != null && !id.isEmpty()) {
            addNotInjectableId(id);
        }
    }

    private boolean isInsideTemplate(org.jsoup.nodes.Element element,
            org.jsoup.nodes.Element templateRoot) {
        if (element == templateRoot) {
            return false;
        }
        if ("template".equalsIgnoreCase(element.tagName())) {
            return true;
        }
        return isInsideTemplate(element.parent(), templateRoot);
    }

    private void addSubTemplate(String id, String tag, JsonArray path) {
        subTemplates.add(new SubTemplateData(id, tag, path));
    }

    private void addNotInjectableId(String id) {
        notInjectableElementIds.add(id);
    }

    private void addTwoWayBindingPath(String path) {
        twoWayBindingPaths.add(path);
    }

}
