package com.vaadin.hummingbird.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import com.vaadin.hummingbird.kernel.AttributeBinding;
import com.vaadin.hummingbird.kernel.BoundElementTemplate;
import com.vaadin.hummingbird.kernel.DynamicTextTemplate;
import com.vaadin.hummingbird.kernel.ElementTemplate;
import com.vaadin.hummingbird.kernel.ForElementTemplate;
import com.vaadin.hummingbird.kernel.ModelAttributeBinding;
import com.vaadin.hummingbird.kernel.ModelPath;
import com.vaadin.hummingbird.kernel.StaticTextTemplate;

public class TemplateParser {
    private static final Pattern forDefinitionPattern = Pattern
            .compile("#([^\\s]+)\\s+of\\s+([^\\s]+)");

    @FunctionalInterface
    private interface Context {
        public List<String> resolve(List<String> path);

        public default ModelPath getPath(String definition) {
            List<String> path = resolve(Arrays.asList(definition.split("\\.")));
            return new ModelPath(definition, path);
        }
    }

    public static ElementTemplate parse(String templateString) {
        Document bodyFragment = Jsoup.parseBodyFragment(templateString);
        return createElementTemplate(bodyFragment.body().child(0), l -> l);
    }

    private static BoundElementTemplate createTemplate(Node node,
            Context context) {
        if (node instanceof Element) {
            return createElementTemplate((Element) node, context);
        } else if (node instanceof TextNode) {
            return createTextTemplate((TextNode) node, context);
        } else {
            throw new RuntimeException(node.getClass().getName());
        }
    }

    private static BoundElementTemplate createTextTemplate(TextNode node,
            Context context) {
        String text = node.text();
        if (text.startsWith("{{")) {
            if (!text.endsWith("}}")) {
                throw new RuntimeException();
            }
            String modelPath = text.substring(2, text.length() - 2);
            return new DynamicTextTemplate(context.getPath(modelPath));
        } else {
            return new StaticTextTemplate(text);
        }
    }

    @FunctionalInterface
    private interface TemplateCreator {
        public BoundElementTemplate create(String tag,
                Collection<AttributeBinding> attributeBindings,
                Map<String, String> defaultAttributeValues,
                List<BoundElementTemplate> childTemplates);
    }

    private static BoundElementTemplate createElementTemplate(Element element,
            Context context) {
        List<AttributeBinding> bindings = new ArrayList<>();
        Map<String, String> defaultAttributes = new HashMap<>();
        TemplateCreator creator = null;

        for (Attribute a : element.attributes()) {
            String name = a.getKey();
            if (name.startsWith("*")) {
                String value = a.getValue();
                if ("*ng-for".equals(name)) {
                    assert creator == null;

                    Matcher matcher = forDefinitionPattern.matcher(value);
                    if (!matcher.matches()) {
                        throw new RuntimeException();
                    }

                    String innerVarName = matcher.group(1);
                    ModelPath listPath = context.getPath(matcher.group(2));

                    Context outerContext = context;
                    context = new Context() {
                        @Override
                        public List<String> resolve(List<String> path) {
                            if (path.isEmpty()) {
                                throw new IndexOutOfBoundsException();
                            } else if (innerVarName.equals(path.get(0))) {
                                return path.subList(1, path.size());
                            } else {
                                path = new ArrayList<>(path);
                                path.add(0, "..");
                                return path;
                            }
                        }
                    };

                    creator = new TemplateCreator() {
                        @Override
                        public BoundElementTemplate create(String tag,
                                Collection<AttributeBinding> attributeBindings,
                                Map<String, String> defaultAttributeValues,
                                List<BoundElementTemplate> childTemplates) {

                            return new ForElementTemplate(tag,
                                    attributeBindings, defaultAttributes,
                                    listPath, innerVarName, childTemplates);
                        }
                    };
                } else {
                    throw new RuntimeException(
                            "Unsupported * attribute: " + name);
                }
            }
        }

        for (Attribute a : element.attributes()) {
            String name = a.getKey();
            String value = a.getValue();
            if (name.startsWith("*")) {
                // Handled above
                continue;
            } else if (name.startsWith("[")) {
                bindings.add(new ModelAttributeBinding(
                        name.substring(1, name.length() - 1),
                        context.getPath(value)));
            } else {
                defaultAttributes.put(name, value);
            }
        }

        final Context finalContext = context;
        List<BoundElementTemplate> childTemplates = element.childNodes()
                .stream().map(c -> createTemplate(c, finalContext))
                .collect(Collectors.toList());
        if (childTemplates.isEmpty()) {
            childTemplates = null;
        }

        if (creator == null) {
            creator = BoundElementTemplate::new;
        }

        return creator.create(element.tagName(), bindings, defaultAttributes,
                childTemplates);
    }

    private static final ConcurrentMap<Class<?>, ElementTemplate> templateCache = new ConcurrentHashMap<>();

    public static ElementTemplate parse(Class<?> type) {
        return templateCache.computeIfAbsent(type,
                TemplateParser::getParsedTemplate);
    }

    private static ElementTemplate getParsedTemplate(Class<?> type) {
        String fileName = type.getSimpleName() + ".html";
        InputStream resource = type.getResourceAsStream(fileName);
        if (resource == null) {
            throw new RuntimeException("File not found from classpath: "
                    + type.getPackage().getName() + "/" + fileName);
        }

        try {
            String templateString = IOUtils.toString(resource);
            return parse(templateString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
