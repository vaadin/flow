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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.polymertemplate.TemplateDataAnalyzer.ParserData;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ShadowRoot;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.ReflectionCache;
import com.vaadin.flow.internal.nodefeature.NodeProperties;
import com.vaadin.flow.internal.nodefeature.VirtualChildrenList;
import com.vaadin.flow.server.VaadinService;

import elemental.json.JsonArray;

/**
 * Template initialization related logic (parse template, create sub-templates,
 * inject elements by id).
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class TemplateInitializer {
    private static final ConcurrentHashMap<TemplateParser, ReflectionCache<PolymerTemplate<?>, ParserData>> CACHE = new ConcurrentHashMap<>();
    private static final ReflectionCache<PolymerTemplate<?>, Map<String, Class<? extends Component>>> USES_CACHE = new ReflectionCache<>(
            TemplateInitializer::extractUsesMap);

    private final PolymerTemplate<?> template;
    private final Class<? extends PolymerTemplate<?>> templateClass;

    private final ParserData parserData;

    private final Map<String, Element> registeredElementIdToInjected = new HashMap<>();

    /**
     * Creates a new initializer instance.
     *
     * @param template
     *            a template to initialize
     * @param parser
     *            a template parser instance
     * @param service
     *            the related service
     */
    @SuppressWarnings("unchecked")
    public TemplateInitializer(PolymerTemplate<?> template,
            TemplateParser parser, VaadinService service) {
        this.template = template;

        boolean productionMode = service.getDeploymentConfiguration()
                .isProductionMode();

        templateClass = (Class<? extends PolymerTemplate<?>>) template
                .getClass();

        ParserData data = null;
        if (productionMode) {
            ReflectionCache<PolymerTemplate<?>, ParserData> cache = CACHE
                    .computeIfAbsent(parser, analyzer -> new ReflectionCache<>(
                            clazz -> new TemplateDataAnalyzer(clazz, analyzer,
                                    service).parseTemplate()));
            data = cache.get(templateClass);
        }
        if (data == null) {
            data = new TemplateDataAnalyzer(templateClass, parser, service)
                    .parseTemplate();
        }
        parserData = data;
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
        attachComponentIfUses(element);
    }

    /**
     * Looks for a component class with the given tag name among the classes
     * used by the given polymer template class. Usage is determined based on
     * the {@link Uses @Uses} annotation.
     *
     * @param templateType
     *            the polymer template type
     * @param tagName
     *            the tag name to look for
     * @return an optional component class, or an empty optional if the template
     *         doesn't use any component with the given tag name
     */
    public static Optional<Class<? extends Component>> getUsesClass(
            Class<? extends PolymerTemplate<?>> templateType, String tagName) {
        return Optional.ofNullable(USES_CACHE.get(templateType)
                .get(tagName.toLowerCase(Locale.ROOT)));
    }

    private void attachComponentIfUses(Element element) {
        getUsesClass(templateClass, element.getTag()).ifPresent(
                componentClass -> Component.from(element, componentClass));
    }

    private ShadowRoot getShadowRoot() {
        return getElement().getShadowRoot()
                .orElseGet(() -> getElement().attachShadow());
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
            attachComponentIfUses(element);
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
                data.getId(), data.getTag(), data.getPath()));
    }

    private static Map<String, Class<? extends Component>> extractUsesMap(
            Class<PolymerTemplate<?>> templateType) {
        Map<String, Class<? extends Component>> map = new HashMap<>();

        BiConsumer<String, Class<? extends Component>> add = (tag, type) -> {
            Class<?> previous = map.put(tag, type);

            if (previous != null && previous != type) {
                throw new IllegalStateException(templateType
                        + " has multiple @Uses classes with the tag name " + tag
                        + ": " + type.getName() + " and " + previous.getName());
            }
        };

        AnnotationReader
                .getAnnotationValuesFor(templateType, Uses.class, Uses::value)
                .forEach(usedType -> AnnotationReader
                        .getAnnotationValueFor(usedType, Tag.class, Tag::value)
                        .map(tag -> tag.toLowerCase(Locale.ROOT))
                        .ifPresent(tag -> add.accept(tag, usedType)));

        return map;
    }
}
