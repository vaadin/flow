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
package com.vaadin.flow.component.polymertemplate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeVisitor;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ShadowRoot;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.ReflectionCache;
import com.vaadin.flow.internal.nodefeature.NodeProperties;
import com.vaadin.flow.internal.nodefeature.VirtualChildrenList;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.CustomElementRegistry;

import elemental.json.Json;
import elemental.json.JsonArray;

/**
 * Template initialization related logic (parse template, create sub-templates,
 * inject elements by id).
 *
 * @author Vaadin Ltd
 *
 */
public class TemplateInitializer {
    // {{propertyName}} or {{propertyName::event}}
    private static final Pattern TWO_WAY_BINDING_PATTERN = Pattern
            .compile("\\s*\\{\\{([^}:]*)(::[^}]*)?\\}\\}\\s*");

    private static final ConcurrentHashMap<TemplateParser, ReflectionCache<PolymerTemplate<?>, ParserData>> CACHE = new ConcurrentHashMap<>();

    private final PolymerTemplate<?> template;
    private final Class<? extends PolymerTemplate<?>> templateClass;

    private final ParserData parserData;

    private final Map<String, Element> registeredElementIdToInjected = new HashMap<>();

    private static class SubTemplateData {
        private final String id;
        private final String tag;
        private final JsonArray path;

        private SubTemplateData(String id, String tag, JsonArray path) {
            this.id = id;
            this.tag = tag;
            this.path = path;
        }
    }

    private interface InjectableFieldCunsumer {
        void apply(Field field, String id, String tag);
    }

    /**
     * Immutable parser data which may be stored in cache.
     */
    private static class ParserData {

        private final Map<String, String> tagById;
        private final Map<Field, String> idByField;

        private final Set<String> twoWayBindingPaths;

        private final Collection<SubTemplateData> subTemplates;

        private ParserData(Map<Field, String> fields, Map<String, String> tags,
                Set<String> twoWayBindings,
                Collection<SubTemplateData> subTemplates) {
            tagById = Collections.unmodifiableMap(tags);
            idByField = Collections.unmodifiableMap(fields);
            twoWayBindingPaths = Collections.unmodifiableSet(twoWayBindings);
            this.subTemplates = Collections
                    .unmodifiableCollection(subTemplates);
        }

        private void forEachInjectedField(InjectableFieldCunsumer consumer) {
            idByField.forEach(
                    (field, id) -> consumer.apply(field, id, tagById.get(id)));
        }

        private Set<String> getTwoWayBindingPaths() {
            return twoWayBindingPaths;
        }

        private void forEachSubTemplate(
                Consumer<SubTemplateData> dataConsumer) {
            subTemplates.forEach(dataConsumer);
        }
    }

    /**
     * Mutable temporary parser data which is used to collected data only during
     * initialization. The data will be discarded and not stored anywhere.
     */
    private static class ParserDataContext {
        private final Map<String, String> tagById = new HashMap<>();
        private final Map<Field, String> idByField = new HashMap<>();

        private final Collection<SubTemplateData> subTemplates = new ArrayList<>();
        private final Set<String> twoWayBindingPaths = new HashSet<>();
        private final Set<String> notInjectableElementIds = new HashSet<>();

        private org.jsoup.nodes.Element templateRoot;

        private void addSubTemplate(String id, String tag, JsonArray path) {
            subTemplates.add(new SubTemplateData(id, tag, path));
        }

        private void addNotInjectableId(String id) {
            notInjectableElementIds.add(id);
        }

        private void addTwoWayBindingPath(String path) {
            twoWayBindingPaths.add(path);
        }

        private void setTemplateRoot(org.jsoup.nodes.Element root) {
            templateRoot = root;
        }

        private Optional<String> addTagName(String id, Field field) {
            idByField.put(field, id);
            Optional<String> tag = Optional
                    .ofNullable(templateRoot.getElementById(id))
                    .map(org.jsoup.nodes.Element::tagName);
            if (tag.isPresent()) {
                tagById.put(id, tag.get());
            }
            return tag;
        }

    }

    /**
     * Creates a new initializer instance.
     *
     * @param template
     *            a template to initialize
     * @param parser
     *            a template parser instance
     */
    @SuppressWarnings("unchecked")
    public TemplateInitializer(PolymerTemplate<?> template,
            TemplateParser parser) {
        this.template = template;

        boolean productionMode = VaadinService.getCurrent()
                .getDeploymentConfiguration().isProductionMode();

        templateClass = (Class<? extends PolymerTemplate<?>>) template
                .getClass();

        ParserData data = null;
        if (productionMode) {
            ReflectionCache<PolymerTemplate<?>, ParserData> cache = CACHE
                    .computeIfAbsent(parser,
                            analyzer -> new ReflectionCache<PolymerTemplate<?>, ParserData>(
                                    clazz -> parseTemplate(clazz, analyzer)));
            data = cache.get(templateClass);
        }
        if (data == null) {
            data = parseTemplate(templateClass, parser);
        }
        parserData = data;
    }

    private ParserData readData(ParserDataContext context) {
        return new ParserData(context.idByField, context.tagById,
                context.twoWayBindingPaths, context.subTemplates);
    }

    private ParserData parseTemplate(Class<? extends PolymerTemplate<?>> clazz,
            TemplateParser parser) {
        org.jsoup.nodes.Element root = parser.getTemplateContent(templateClass,
                getElement().getTag());
        ParserDataContext data = new ParserDataContext();
        data.setTemplateRoot(root);
        Elements templates = root.getElementsByTag("template");
        for (org.jsoup.nodes.Element element : templates) {
            org.jsoup.nodes.Element parent = element.parent();
            if (parent != null && getElement().getTag().equals(parent.id())) {
                inspectCustomElements(element, element, data);

                inspectTwoWayBindings(element, data);
            }
        }
        collectInjectedIds(clazz, data);
        return readData(data);
    }

    /**
     * Initializes child elements.
     */
    public void initChildElements() {
        registeredElementIdToInjected.clear();
        mapComponents();
        createSubTemplates();
    }

    /**
     * Gets a set of two way binding paths encountered in the template.
     *
     * @return an unmodifiable collection of two way binding paths
     */
    public Set<String> getTwoWayBindingPaths() {
        return parserData.getTwoWayBindingPaths();
    }

    private void inspectCustomElements(org.jsoup.nodes.Element childElement,
            org.jsoup.nodes.Element templateRoot, ParserDataContext data) {
        if (isInsideTemplate(childElement, templateRoot)) {
            storeNotInjectableElementId(childElement, data);
        }

        collectCustomElement(childElement, templateRoot, data);
        childElement.children().forEach(
                child -> inspectCustomElements(child, templateRoot, data));
    }

    private void storeNotInjectableElementId(org.jsoup.nodes.Element element,
            ParserDataContext data) {
        String id = element.id();
        if (id != null && !id.isEmpty()) {
            data.addNotInjectableId(id);
        }
    }

    private void inspectTwoWayBindings(org.jsoup.nodes.Element element,
            ParserDataContext data) {
        Matcher matcher = TWO_WAY_BINDING_PATTERN.matcher("");
        element.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                // Two way bindings should only be in property bindings, not
                // inside text content.
                for (Attribute attribute : node.attributes()) {
                    matcher.reset(attribute.getValue());
                    if (matcher.matches()) {
                        String path = matcher.group(1);
                        data.addTwoWayBindingPath(path);
                    }
                }
            }

            @Override
            public void tail(Node node, int depth) {
                // Nop
            }
        });
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

    private void collectCustomElement(org.jsoup.nodes.Element element,
            org.jsoup.nodes.Element templateRoot, ParserDataContext data) {
        String tag = element.tagName();

        if (CustomElementRegistry.getInstance()
                .isRegisteredCustomElement(tag)) {
            if (isInsideTemplate(element, templateRoot)) {
                throw new IllegalStateException("Couldn't parse the template: "
                        + "sub-templates are not supported. Sub-template found: \n"
                        + element);
            }

            String id = element.hasAttr("id") ? element.attr("id") : null;
            JsonArray path = getPath(element, templateRoot);
            data.addSubTemplate(id, tag, path);
        }
    }

    private void doRequestAttachCustomElement(String id, String tag,
            JsonArray path) {
        if (registeredElementIdToInjected.containsKey(id)) {
            return;
        }
        // make sure that shadow root is available
        getShadowRoot();

        Element element = new Element(tag);
        VirtualChildrenList list = getElement().getNode()
                .getFeature(VirtualChildrenList.class);
        list.append(element.getNode(), NodeProperties.TEMPLATE_IN_TEMPLATE,
                path);

        // anything else
        CustomElementRegistry.getInstance().wrapElementIfNeeded(element);
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
    private int indexOf(org.jsoup.nodes.Element parent,
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

    private ShadowRoot getShadowRoot() {
        return getElement().getShadowRoot()
                .orElseGet(() -> getElement().attachShadow());
    }

    private void collectInjectedIds(Class<?> cls, ParserDataContext data) {
        if (!AbstractTemplate.class.equals(cls.getSuperclass())) {
            // Parent fields
            collectInjectedIds(cls.getSuperclass(), data);
        }

        Stream.of(cls.getDeclaredFields()).filter(field -> !field.isSynthetic())
                .forEach(field -> collectedInjectedId(field, data));
    }

    private void collectedInjectedId(Field field, ParserDataContext data) {
        Optional<Id> idAnnotation = AnnotationReader.getAnnotationFor(field,
                Id.class);
        if (!idAnnotation.isPresent()) {
            return;
        }
        String id = idAnnotation.get().value();
        if (data.notInjectableElementIds.contains(id)) {
            throw new IllegalStateException(String.format(
                    "Class '%s' contains field '%s' annotated with @Id('%s'). "
                            + "Corresponding element was found in a sub template, for which injection is not supported",
                    templateClass.getName(), field.getName(), id));
        }

        if (!data.addTagName(id, field).isPresent()) {
            throw new IllegalStateException(String.format(
                    "There is no element with "
                            + "id='%s' in the template file. Cannot map it using @%s",
                    id, Id.class.getSimpleName()));
        }
    }

    /* Map declared fields marked @Id */

    private void mapComponents() {
        parserData.forEachInjectedField(this::tryMapComponentOrElement);
    }

    private void tryMapComponentOrElement(Field field, String id, String tag) {
        Element element = getElementById(id).orElse(null);

        if (element == null) {
            injectClientSideElement(tag, id, field);
        } else {
            injectServerSideElement(element, field);
        }
    }

    private void injectServerSideElement(Element element, Field field) {
        if (getElement().equals(element)) {
            throw new IllegalArgumentException(
                    "Cannot map the root element of the template. "
                            + "This is always mapped to the template instance itself ("
                            + templateClass.getName() + ')');
        } else if (element != null) {
            injectTemplateElement(element, field);
        }
    }

    private void injectClientSideElement(String tagName, String id,
            Field field) {
        Class<?> fieldType = field.getType();

        Tag tag = fieldType.getAnnotation(Tag.class);
        if (tag != null && !tagName.equalsIgnoreCase(tag.value())) {
            String msg = String.format(
                    "Class '%s' has field '%s' whose type '%s' is annotated with "
                            + "tag '%s' but the element defined in the HTML "
                            + "template with id '%s' has tag name '%s'",
                    templateClass.getName(), field.getName(),
                    fieldType.getName(), tag.value(), id, tagName);
            throw new IllegalStateException(msg);
        }
        attachExistingElementById(tagName, id, field);
    }

    private Optional<Element> getElementById(String id) {
        return getShadowRoot().getChildren().flatMap(this::flattenChildren)
                .filter(element -> id.equals(element.getAttribute("id")))
                .findFirst();
    }

    private Stream<Element> flattenChildren(Element node) {
        if (node.getChildCount() > 0) {
            return node.getChildren().flatMap(this::flattenChildren);
        }
        return Stream.of(node);
    }

    /**
     * Attaches a child element with the given {@code tagName} and {@code id} to
     * an existing dom element on the client side with matching data.
     *
     * @param tagName
     *            tag name of element, not {@code null}
     * @param id
     *            id of element to attach to
     * @param field
     *            field to attach {@code Element} or {@code Component} to
     */
    private void attachExistingElementById(String tagName, String id,
            Field field) {
        if (tagName == null) {
            throw new IllegalArgumentException(
                    "Tag name parameter cannot be null");
        }

        Element element = registeredElementIdToInjected.get(id);
        if (element == null) {
            element = new Element(tagName);
            VirtualChildrenList list = getElement().getNode()
                    .getFeature(VirtualChildrenList.class);
            list.append(element.getNode(), NodeProperties.INJECT_BY_ID, id);
            registeredElementIdToInjected.put(id, element);
        }
        injectTemplateElement(element, field);
    }

    private Element getElement() {
        return template.getElement();
    }

    @SuppressWarnings("unchecked")
    private void injectTemplateElement(Element element, Field field) {
        Class<?> fieldType = field.getType();
        if (Component.class.isAssignableFrom(fieldType)) {
            CustomElementRegistry.getInstance().wrapElementIfNeeded(element);
            Component component;

            Optional<Component> wrappedComponent = element.getComponent();
            if (wrappedComponent.isPresent()) {
                component = wrappedComponent.get();
            } else {
                Class<? extends Component> componentType = (Class<? extends Component>) fieldType;
                component = Component.from(element, componentType);
            }

            ReflectTools.setJavaFieldValue(template, field, component);
        } else if (Element.class.isAssignableFrom(fieldType)) {
            ReflectTools.setJavaFieldValue(template, field, element);
        } else {
            String msg = String.format(
                    "The field '%s' in '%s' has an @'%s' "
                            + "annotation but the field type '%s' "
                            + "does not extend neither '%s' nor '%s'",
                    field.getName(), templateClass.getName(),
                    Id.class.getSimpleName(), fieldType.getName(),
                    Component.class.getSimpleName(),
                    Element.class.getSimpleName());

            throw new IllegalArgumentException(msg);
        }
    }

    private void createSubTemplates() {
        parserData.forEachSubTemplate(data -> doRequestAttachCustomElement(
                data.id, data.tag, data.path));
    }

}
