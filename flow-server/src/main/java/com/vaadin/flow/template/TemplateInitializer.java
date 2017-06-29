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
package com.vaadin.flow.template;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.vaadin.annotations.AnnotationReader;
import com.vaadin.annotations.Id;
import com.vaadin.annotations.Tag;
import com.vaadin.external.jsoup.nodes.Node;
import com.vaadin.external.jsoup.select.Elements;
import com.vaadin.flow.StateNode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ShadowRoot;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.flow.nodefeature.AttachTemplateChildFeature;
import com.vaadin.flow.util.ReflectionCache;
import com.vaadin.server.CustomElementRegistry;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.Component;
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

    private final PolymerTemplate<?> template;

    // id to Element map
    private Map<String, Element> registeredCustomElements = new HashMap<String, Element>();

    @SuppressWarnings("rawtypes")
    private static final ReflectionCache<PolymerTemplate, ParserData> CACHE = new ReflectionCache<>(
            clazz -> new ParserData());

    private boolean useCache;

    private ParserData parseData;

    private com.vaadin.external.jsoup.nodes.Element contentElement;

    private static class SubTemplateData {
        private String id;
        private String tag;
        private JsonArray path;

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

        private boolean isProduction() {
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

        boolean productionMode = VaadinService.getCurrent()
                .getDeploymentConfiguration().isProductionMode();
        useCache = CACHE.contains(template.getClass()) && productionMode;
        if (productionMode) {
            parseData = CACHE.get(template.getClass());
        } else {
            // always initialize parseData to avoid check against null
            parseData = new ParserData();
        }

        if (useCache) {
            createSubTemplates();
        } else {
            contentElement = parser.getTemplateContent(template.getClass(),
                    getElement().getTag());
            parseTemplate();
        }
    }

    /**
     * Initializes child elements.
     */
    public void initChildElements() {
        mapComponents(template.getClass());
    }

    private void inspectCustomElements(Node node,
            com.vaadin.external.jsoup.nodes.Element templateRoot,
            Map<String, Element> registeredCustomElements) {
        if (node instanceof com.vaadin.external.jsoup.nodes.Element) {
            com.vaadin.external.jsoup.nodes.Element element = (com.vaadin.external.jsoup.nodes.Element) node;

            requestAttachCustomElement(element, templateRoot);
            element.children().forEach(child -> inspectCustomElements(child,
                    templateRoot, registeredCustomElements));
        }
    }

    private void parseTemplate() {
        assert contentElement != null;
        Elements templates = contentElement.getElementsByTag("template");
        if (!templates.isEmpty()) {
            inspectCustomElements(templates.get(0), templates.get(0),
                    registeredCustomElements);
        }
    }

    private boolean isInsideTemplate(
            com.vaadin.external.jsoup.nodes.Element element,
            com.vaadin.external.jsoup.nodes.Element templateRoot) {
        if (element == templateRoot) {
            return false;
        }
        if ("template".equalsIgnoreCase(element.tagName())) {
            return true;
        }
        return isInsideTemplate(element.parent(), templateRoot);
    }

    private void requestAttachCustomElement(
            com.vaadin.external.jsoup.nodes.Element element,
            com.vaadin.external.jsoup.nodes.Element templateRoot) {
        String tag = element.tagName();

        if (CustomElementRegistry.getInstance()
                .isRegisteredCustomElement(tag)) {
            if (isInsideTemplate(element, templateRoot)) {
                throw new IllegalStateException("Couldn't parse the tempalte: "
                        + "sub-templates are not supported. Sub-template found: \n"
                        + element);
            }

            String id = element.hasAttr("id") ? element.attr("id") : null;
            JsonArray path = getPath(element, templateRoot);
            assert !useCache;
            parseData.addSubTemplate(id, tag, path);
            doRequestAttachCustomElement(id, tag, path);
        }
    }

    private void doRequestAttachCustomElement(String id, String tag,
            JsonArray path) {
        StateNode customNode = BasicElementStateProvider.createStateNode(tag);
        Element customElement = Element.get(customNode);
        CustomElementRegistry.getInstance().wrapElementIfNeeded(customElement);

        if (id != null) {
            registeredCustomElements.put(id, customElement);
        }

        // make sure that shadow root is available
        getShadowRoot();

        StateNode stateNode = getElement().getNode();

        stateNode.runWhenAttached(ui -> {
            stateNode.getFeature(AttachTemplateChildFeature.class)
                    .register(getElement(), customNode);
            ui.getPage().executeJavaScript(
                    "this.attachCustomElement($0, $1, $2, $3);", getElement(),
                    tag, customNode.getId(), path);
        });
    }

    private JsonArray getPath(com.vaadin.external.jsoup.nodes.Element element,
            com.vaadin.external.jsoup.nodes.Element templateRoot) {
        List<Integer> path = new ArrayList<>();
        com.vaadin.external.jsoup.nodes.Element current = element;
        while (!current.equals(templateRoot)) {
            com.vaadin.external.jsoup.nodes.Element parent = current.parent();
            path.add(parent.children().indexOf(current));
            current = parent;
        }
        JsonArray array = Json.createArray();
        for (int i = 0; i < path.size(); i++) {
            array.set(i, path.get(path.size() - i - 1));
        }
        return array;
    }

    private ShadowRoot getShadowRoot() {
        Optional<ShadowRoot> shadowRootOptional = getElement().getShadowRoot();

        ShadowRoot shadowRoot;

        if (shadowRootOptional.isPresent()) {
            shadowRoot = shadowRootOptional.get();
        } else {
            shadowRoot = getElement().attachShadow();
        }
        return shadowRoot;
    }

    /* Map declared fields marked @Id */

    private void mapComponents(Class<?> cls) {
        if (!AbstractTemplate.class.equals(cls.getSuperclass())) {
            // Parent fields
            mapComponents(cls.getSuperclass());
        }

        Stream<Field> annotatedComponentFields = Stream
                .of(cls.getDeclaredFields())
                .filter(field -> !field.isSynthetic());

        annotatedComponentFields
                .forEach(field -> tryMapComponentOrElement(field,
                        registeredCustomElements));
    }

    private void tryMapComponentOrElement(Field field,
            Map<String, Element> registeredCustomElements) {
        Optional<Id> idAnnotation = AnnotationReader.getAnnotationFor(field,
                Id.class);
        if (!idAnnotation.isPresent()) {
            return;
        }
        String id = idAnnotation.get().value();

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
            return parseData.apply(id);
        } else {
            Optional<String> tag = Optional
                    .ofNullable(contentElement.getElementById(id))
                    .map(com.vaadin.external.jsoup.nodes.Element::tagName);
            if (tag.isPresent()) {
                parseData.addTag(id, tag.get());
            }
            return tag;
        }
    }

    private void injectServerSideElement(Element element, Field field) {
        if (getElement().equals(element)) {
            throw new IllegalArgumentException(
                    "Cannot map the root element of the template. "
                            + "This is always mapped to the template instance itself ("
                            + template.getClass().getName() + ")");
        } else if (element != null) {
            handleAttach(element, field);
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
                    template.getClass().getName(), field.getName(),
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
            Field field, Map<String, Element> registeredCustomElementss) {
        if (tagName == null) {
            throw new IllegalArgumentException(
                    "Tag name parameter cannot be null");
        }

        Element element = registeredCustomElementss.get(id);

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

            StateNode node = getElement().getNode();
            node.runWhenAttached(ui -> {
                node.getFeature(AttachTemplateChildFeature.class)
                        .register(getElement(), proposedNode);
                ui.getPage().executeJavaScript(
                        "this.attachExistingElementById($0, $1, $2, $3);",
                        getElement(), tagName, proposedNode.getId(), id);
            });
        }
        handleAttach(element, field);

    }

    private Element getElement() {
        return template.getElement();
    }

    private void handleAttach(Element element, Field field) {
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
                    field.getName(), template.getClass().getName(),
                    Id.class.getSimpleName(), fieldType.getName(),
                    Component.class.getSimpleName(),
                    Element.class.getSimpleName());

            throw new IllegalArgumentException(msg);
        }
    }

    private void createSubTemplates() {
        parseData.subTemplates
                .forEach(data -> doRequestAttachCustomElement(data.id, data.tag,
                        data.path));
    }

}
