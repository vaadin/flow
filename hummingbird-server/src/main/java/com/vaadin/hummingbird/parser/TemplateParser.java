package com.vaadin.hummingbird.parser;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import com.vaadin.annotations.TemplateEventHandler;
import com.vaadin.hummingbird.kernel.BoundTemplateBuilder;
import com.vaadin.hummingbird.kernel.ElementTemplate;
import com.vaadin.hummingbird.kernel.ModelAttributeBinding;
import com.vaadin.hummingbird.kernel.ModelPath;
import com.vaadin.hummingbird.kernel.TemplateBuilder;
import com.vaadin.ui.Template;

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
        return parseBuilder(templateString).build();
    }

    private static TemplateBuilder parseBuilder(String templateString) {
        Document bodyFragment = Jsoup.parseBodyFragment(templateString);
        return createElementTemplate(bodyFragment.body().child(0), l -> l);
    }

    private static TemplateBuilder createTemplate(Node node, Context context) {
        if (node instanceof Element) {
            return createElementTemplate((Element) node, context);
        } else if (node instanceof TextNode) {
            return createTextTemplate((TextNode) node, context);
        } else if (node instanceof Comment) {
            return null;
        } else {
            throw new RuntimeException(node.getClass().getName());
        }
    }

    private static TemplateBuilder createTextTemplate(TextNode node,
            Context context) {
        String text = node.text();
        if (text.startsWith("{{")) {
            if (!text.endsWith("}}")) {
                throw new RuntimeException();
            }
            String modelPath = text.substring(2, text.length() - 2);
            return TemplateBuilder.dynamicText(context.getPath(modelPath));
        } else {
            return TemplateBuilder.staticText(text);
        }
    }

    private static TemplateBuilder createElementTemplate(Element element,
            Context context) {

        BoundTemplateBuilder builder = TemplateBuilder
                .withTag(element.tagName());

        for (Attribute a : element.attributes()) {
            String name = a.getKey();
            if (name.startsWith("*")) {
                String value = a.getValue();
                if ("*ng-for".equals(name)) {
                    Matcher matcher = forDefinitionPattern.matcher(value);
                    if (!matcher.matches()) {
                        throw new RuntimeException();
                    }

                    String innerVarName = matcher.group(1);
                    ModelPath listPath = context.getPath(matcher.group(2));

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

                    builder.setForDefinition(listPath, innerVarName);
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
                String attibuteName = name.substring(1, name.length() - 1);
                builder.bindAttribute(new ModelAttributeBinding(attibuteName,
                        context.getPath(value)));
            } else if (name.startsWith("(")) {
                String eventName = name.substring(1, name.length() - 1);

                String eventHandler = value;

                builder.addEventBinding(
                        new EventBinding(eventName, eventHandler));
            } else if (name.startsWith("#")) {
                // Ignore local ids for now
            } else {
                builder.setAttribute(name, value);
            }
        }

        for (Node node : element.childNodes()) {
            TemplateBuilder childTemplate = createTemplate(node, context);
            if (childTemplate != null) {
                builder.addChild(childTemplate);
            }
        }

        return builder;
    }

    private static class ElementTemplateCache {
        private final ElementTemplate template;
        private final long timestamp;

        public ElementTemplateCache(ElementTemplate template, long timestamp) {
            this.template = template;
            this.timestamp = timestamp;
        }
    }

    private static final ConcurrentMap<Class<?>, ElementTemplateCache> templateCache = new ConcurrentHashMap<>();

    public static ElementTemplate parse(Class<?> type) {
        String fileName = type.getSimpleName() + ".html";

        URL resourceUrl = type.getResource(fileName);
        if (resourceUrl == null) {
            throw new RuntimeException("File not found from classpath: "
                    + type.getPackage().getName() + "/" + fileName);
        }

        ElementTemplateCache cacheEntry = templateCache.get(type);

        try {
            URLConnection connection = resourceUrl.openConnection();
            try (InputStream is = connection.getInputStream()) {
                // Check lastModified after accessing the input stream to make
                // sure it is always properly closed
                // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4257700
                long lastModified = connection.getLastModified();
                if (cacheEntry != null
                        && cacheEntry.timestamp == lastModified) {
                    return cacheEntry.template;
                } else {
                    String templateString = IOUtils.toString(is);
                    BoundTemplateBuilder builder = (BoundTemplateBuilder) parseBuilder(
                            templateString);

                    Set<String> methodNames = findEventHandlerMethodNames(type);
                    methodNames.forEach(builder::addEventHandlerMethod);

                    ElementTemplate template = builder.build();

                    templateCache.put(type,
                            new ElementTemplateCache(template, lastModified));
                    return template;
                }
            }
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
    }

    private static Set<String> findEventHandlerMethodNames(Class<?> type) {
        Set<String> names = new HashSet<>();
        while (type != null && type != Template.class) {
            Method[] declaredMethods = type.getDeclaredMethods();
            for (Method method : declaredMethods) {
                if (method.getAnnotation(TemplateEventHandler.class) != null) {
                    names.add(method.getName());
                }
            }

            type = type.getSuperclass();
        }

        return names;
    }

}
