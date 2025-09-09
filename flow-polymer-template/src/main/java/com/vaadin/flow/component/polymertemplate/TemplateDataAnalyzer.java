/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeVisitor;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.polymertemplate.TemplateParser.TemplateData;
import com.vaadin.flow.component.template.internal.InjectableFieldConsumer;
import com.vaadin.flow.component.template.internal.ParserData;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.VaadinService;

/**
 * Template data analyzer which produces immutable data required for template
 * initializer using provided template class and a parser.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @deprecated Use {@code LitTemplateDataAnalyzer} for {@code LitTemplate}
 *             components. Polymer template support is deprecated - we recommend
 *             you to use {@code LitTemplate} instead. Read more details from
 *             <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 *
 */
@Deprecated
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

    private String modulePath;

    /**
     * Three argument consumer.
     *
     * @author Vaadin Ltd
     * @since 1.0
     *
     */
    @FunctionalInterface
    public interface InjectableFieldCunsumer extends InjectableFieldConsumer {

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
        @Override
        void apply(Field field, String id, String tag);
    }

    /**
     * Immutable parser data which may be stored in cache.
     *
     * Use {@link ParserData} instead.
     */
    @Deprecated
    public static class PolymerParserData extends ParserData {

        private final Set<String> twoWayBindingPaths;

        private final Collection<SubTemplateData> subTemplates;

        /**
         * Constructs an immutable data object with the given information.
         *
         * @param fields
         *            a map of fields to their ids
         * @param tags
         *            a map of ids to their tags
         * @param attributes
         *            a map of attributes values to the element id
         * @param twoWayBindings
         *            the properties which support two way binding
         * @param subTemplates
         *            data for sub templates
         */
        public PolymerParserData(Map<Field, String> fields,
                Map<String, String> tags,
                Map<String, Map<String, String>> attributes,
                Set<String> twoWayBindings,
                Collection<SubTemplateData> subTemplates) {
            super(fields, tags, attributes);
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
            InjectableFieldConsumer delegate = (field, id, tag) -> consumer
                    .apply(field, id, tag);
            forEachInjectedField(delegate);
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
        private final JsonNode path;

        SubTemplateData(String id, String tag, JsonNode path) {
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

        JsonNode getPath() {
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
    PolymerParserData parseTemplate() {
        TemplateData templateData = parser.getTemplateContent(templateClass,
                tag, service);
        Element templateRoot = templateData.getTemplateElement();
        modulePath = templateData.getModulePath();
        Elements templates = templateRoot.getElementsByTag("template");
        for (org.jsoup.nodes.Element element : templates) {
            org.jsoup.nodes.Element parent = element.parent();
            if (parent != null && tag.equals(parent.id())) {
                inspectCustomElements(element, element);

                inspectTwoWayBindings(element);
            }
        }
        IdCollector idExtractor = new IdCollector(templateClass, modulePath,
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

    private PolymerParserData readData(IdCollector idExtractor) {
        return new PolymerParserData(idExtractor.getIdByField(),
                idExtractor.getTagById(), idExtractor.getAttributes(),
                twoWayBindingPaths, subTemplates);
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
                        modulePath, element.toString()));
            }

            String id = element.hasAttr("id") ? element.attr("id") : null;
            ArrayNode path = getPath(element, templateRoot);
            addSubTemplate(id, tag, path);
        }
    }

    private ArrayNode getPath(org.jsoup.nodes.Element element,
            org.jsoup.nodes.Element templateRoot) {
        List<Integer> path = new ArrayList<>();
        org.jsoup.nodes.Element current = element;
        while (!current.equals(templateRoot)) {
            org.jsoup.nodes.Element parent = current.parent();
            path.add(indexOf(parent, current));
            current = parent;
        }
        ArrayNode array = JacksonUtils.createArray();
        for (int i = 0; i < path.size(); i++) {
            array.add(path.get(path.size() - i - 1));
        }
        return array;
    }

    /**
     * Returns the index of the {@code child} in the collection of
     * {@link org.jsoup.nodes.Element} children of the {@code parent} ignoring
     * "style" elements.
     * <p>
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

    private void addSubTemplate(String id, String tag, ArrayNode path) {
        subTemplates.add(new SubTemplateData(id, tag, path));
    }

    private void addNotInjectableId(String id) {
        notInjectableElementIds.add(id);
    }

    private void addTwoWayBindingPath(String path) {
        twoWayBindingPaths.add(path);
    }

}
