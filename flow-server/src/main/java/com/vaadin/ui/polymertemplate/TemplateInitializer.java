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
package com.vaadin.ui.polymertemplate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jsoup.select.Elements;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ShadowRoot;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.flow.nodefeature.AttachTemplateChildFeature;
import com.vaadin.flow.nodefeature.NodeProperties;
import com.vaadin.flow.util.ReflectionCache;
import com.vaadin.server.VaadinService;
import com.vaadin.server.startup.CustomElementRegistry;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;
import com.vaadin.util.AnnotationReader;
import com.vaadin.util.ReflectTools;

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
    private static final ReflectionCache<PolymerTemplate<?>, ParserData> CACHE = new ReflectionCache<>(
            clazz -> new ParserData());

    private final PolymerTemplate<?> template;
    private final Class<? extends PolymerTemplate<?>> templateClass;

    private final Map<String, Element> registeredElementIdToCustomElement = new HashMap<>();
    private final boolean useCache;
    private final ParserData parserData;
    private final Set<String> notInjectableElementIds = new HashSet<>();

    private org.jsoup.nodes.Element parsedTemplateRoot;

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

    private static class ParserData
            implements Function<String, Optional<String>> {
        private final Map<String, String> tagById = new HashMap<>();
        private final Collection<SubTemplateData> subTemplates = new ArrayList<>();

        @Override
        public Optional<String> apply(String id) {
            return Optional.ofNullable(tagById.get(id));
        }

        private void addTag(String id, String tag) {
            if (isProduction()) {
                tagById.put(id, tag);
            }
        }

        private void addSubTemplate(String id, String tag, JsonArray path) {
            if (isProduction()) {
                subTemplates.add(new SubTemplateData(id, tag, path));
            }
        }

        private static boolean isProduction() {
            return VaadinService.getCurrent().getDeploymentConfiguration()
                    .isProductionMode();
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
    public TemplateInitializer(PolymerTemplate<?> template,
            TemplateParser parser) {
        this.template = template;
        templateClass = (Class<? extends PolymerTemplate<?>>) template
                .getClass();

        boolean productionMode = VaadinService.getCurrent()
                .getDeploymentConfiguration().isProductionMode();
        useCache = productionMode && CACHE.contains(templateClass);
        parserData = productionMode ? CACHE.get(templateClass)
                : new ParserData();

        if (useCache) {
            createSubTemplates();
        } else {
            parsedTemplateRoot = parser.getTemplateContent(templateClass,
                    getElement().getTag());
            parseTemplate();
        }
    }

    /**
     * Initializes child elements.
     */
    public void initChildElements() {
        mapComponents(templateClass);
    }

    private void inspectCustomElements(org.jsoup.nodes.Element childElement,
            org.jsoup.nodes.Element templateRoot) {
        if (isInsideTemplate(childElement, templateRoot)) {
            storeNotInjectableElementId(childElement);
        }

        requestAttachCustomElement(childElement, templateRoot);
        childElement.children()
                .forEach(child -> inspectCustomElements(child, templateRoot));
    }

    private void storeNotInjectableElementId(org.jsoup.nodes.Element element) {
        String id = element.id();
        if (id != null && !id.isEmpty()) {
            notInjectableElementIds.add(id);
        }
    }

    private void parseTemplate() {
        assert parsedTemplateRoot != null;
        Elements templates = parsedTemplateRoot.getElementsByTag("template");
        for (org.jsoup.nodes.Element element : templates) {
            org.jsoup.nodes.Element parent = element.parent();
            if (parent != null && getElement().getTag().equals(parent.id())) {
                inspectCustomElements(element, element);
            }
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

    private void requestAttachCustomElement(org.jsoup.nodes.Element element,
            org.jsoup.nodes.Element templateRoot) {
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
            assert !useCache;
            parserData.addSubTemplate(id, tag, path);
            doRequestAttachCustomElement(id, tag, path);
        }
    }

    private void doRequestAttachCustomElement(String id, String tag,
            JsonArray path) {
        StateNode customNode = BasicElementStateProvider.createStateNode(tag);
        Element customElement = Element.get(customNode);
        CustomElementRegistry.getInstance().wrapElementIfNeeded(customElement);

        if (id != null) {
            registeredElementIdToCustomElement.put(id, customElement);
        }

        // make sure that shadow root is available
        getShadowRoot();

        StateNode stateNode = getElement().getNode();

        customNode.runWhenAttached(ui -> ui.getPage().executeJavaScript(
                "this.attachCustomElement($0, $1, $2, $3);", getElement(), tag,
                customNode.getId(), path));

        stateNode.getFeature(AttachTemplateChildFeature.class)
                .register(getElement(), customNode);
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

    /* Map declared fields marked @Id */

    private void mapComponents(Class<?> cls) {
        if (!AbstractTemplate.class.equals(cls.getSuperclass())) {
            // Parent fields
            mapComponents(cls.getSuperclass());
        }

        Stream.of(cls.getDeclaredFields()).filter(field -> !field.isSynthetic())
                .forEach(field -> tryMapComponentOrElement(field,
                        registeredElementIdToCustomElement));
    }

    private void tryMapComponentOrElement(Field field,
            Map<String, Element> registeredCustomElements) {
        Optional<Id> idAnnotation = AnnotationReader.getAnnotationFor(field,
                Id.class);
        if (!idAnnotation.isPresent()) {
            return;
        }
        String id = idAnnotation.get().value();
        if (notInjectableElementIds.contains(id)) {
            throw new IllegalStateException(String.format(
                    "Class '%s' contains field '%s' annotated with @Id('%s'). "
                            + "Corresponding element was found in a sub template, for which injection is not supported",
                    templateClass.getName(), field.getName(), id));
        }

        Optional<String> tagName = getTagName(id);
        if (!tagName.isPresent()) {
            throw new IllegalStateException(String.format(
                    "There is no element with "
                            + "id='%s' in the template file. Cannot map it using @%s",
                    id, Id.class.getSimpleName()));
        }

        Element element = getElementById(id).orElse(null);

        if (element == null) {
            injectClientSideElement(tagName.get(), id, field,
                    registeredCustomElements);
        } else {
            injectServerSideElement(element, field);
        }
    }

    private Optional<String> getTagName(String id) {
        if (useCache) {
            return parserData.apply(id);
        } else {
            Optional<String> tag = Optional
                    .ofNullable(parsedTemplateRoot.getElementById(id))
                    .map(org.jsoup.nodes.Element::tagName);
            if (tag.isPresent()) {
                parserData.addTag(id, tag.get());
            }
            return tag;
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

    private void injectClientSideElement(String tagName, String id, Field field,
            Map<String, Element> registeredCustomElements) {
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
        attachExistingElementById(tagName, id, field, registeredCustomElements);
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
            Field field, Map<String, Element> registeredCustomElements) {
        if (tagName == null) {
            throw new IllegalArgumentException(
                    "Tag name parameter cannot be null");
        }

        Element element = registeredCustomElements.get(id);
        if (element == null) {
            /*
             * create a node that should represent the client-side element. This
             * node won't be available anywhere and will be removed if there is
             * no appropriate element on the client-side. This node will be used
             * after client-side roundtrip for the appropriate element.
             */
            StateNode proposedNode = BasicElementStateProvider
                    .createStateNode(tagName);
            element = Element.get(proposedNode);
            element.setAttribute(NodeProperties.ID, id);
            StateNode templateNode = getElement().getNode();

            proposedNode.runWhenAttached(ui -> ui.getPage().executeJavaScript(
                    "this.attachExistingElementById($0, $1, $2, $3);",
                    getElement(), tagName, proposedNode.getId(), id));
            templateNode.getFeature(AttachTemplateChildFeature.class)
                    .register(getElement(), proposedNode);
        }
        injectTemplateElement(element, field);
    }

    private Element getElement() {
        return template.getElement();
    }

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
        parserData.subTemplates
                .forEach(data -> doRequestAttachCustomElement(data.id, data.tag,
                        data.path));
    }

}
