/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.polymertemplate;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.polymertemplate.TemplateDataAnalyzer.PolymerParserData;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.ReflectionCache;
import com.vaadin.flow.internal.nodefeature.NodeProperties;
import com.vaadin.flow.internal.nodefeature.VirtualChildrenList;
import com.vaadin.flow.server.VaadinService;

/**
 * Template initialization related logic (parse template, create sub-templates,
 * inject elements by id).
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @deprecated Use {@code LitTemplateInitializer} for {@code LitTemplate}
 *             components. Polymer template support is deprecated - we recommend
 *             you to use {@code LitTemplate} instead. Read more details from
 *             <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 *
 */
@Deprecated
public class TemplateInitializer {
    private static final ConcurrentHashMap<TemplateParser, ReflectionCache<PolymerTemplate<?>, PolymerParserData>> CACHE = new ConcurrentHashMap<>();
    private static final ReflectionCache<PolymerTemplate<?>, Map<String, Class<? extends Component>>> USES_CACHE = new ReflectionCache<>(
            TemplateInitializer::extractUsesMap);

    private final PolymerTemplate<?> template;
    private final Class<? extends PolymerTemplate<?>> templateClass;

    private final PolymerParserData parserData;

    private IdMapper idMapper;

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
        idMapper = new IdMapper(template);

        boolean productionMode = service.getDeploymentConfiguration()
                .isProductionMode();

        templateClass = (Class<? extends PolymerTemplate<?>>) template
                .getClass();

        PolymerParserData data = null;
        if (productionMode) {
            ReflectionCache<PolymerTemplate<?>, PolymerParserData> cache = CACHE
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
        idMapper.reset();
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
            JsonNode path) {
        if (idMapper.isMapped(id)) {
            return;
        }

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

    /* Map declared fields marked @Id */

    private void mapComponents() {
        parserData.forEachInjectedField((field, id, tag) -> idMapper
                .mapComponentOrElement(field, id, tag, element -> {
                    InjectablePolymerElementInitializer initializer = new InjectablePolymerElementInitializer(
                            element, templateClass);
                    initializer.accept(parserData.getAttributes(id));
                    attachComponentIfUses(element);
                }));
    }

    private Element getElement() {
        return template.getElement();
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
