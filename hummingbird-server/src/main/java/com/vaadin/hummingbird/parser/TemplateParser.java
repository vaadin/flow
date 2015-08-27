package com.vaadin.hummingbird.parser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

import com.vaadin.hummingbird.kernel.BoundTemplateBuilder;
import com.vaadin.hummingbird.kernel.ElementTemplate;
import com.vaadin.hummingbird.kernel.ModelAttributeBinding;
import com.vaadin.hummingbird.kernel.ModelPath;
import com.vaadin.hummingbird.kernel.TemplateBuilder;

public class TemplateParser {
    private static final Pattern forDefinitionPattern = Pattern
            .compile("#([^\\s]+)\\s+of\\s+([^\\s]+)");

    private static final Pattern elementDefinitionPattern = Pattern
            .compile("\\s*([^(\\s]+)\\(([^)]*)\\)\\s*");

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
        return createElementTemplate(bodyFragment.body().child(0), l -> l)
                .build();
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

                Matcher elementMatcher = elementDefinitionPattern
                        .matcher(value);
                if (!elementMatcher.matches()) {
                    throw new RuntimeException(value);
                }

                String methodName = elementMatcher.group(1);
                String[] params = elementMatcher.group(2).split("\\s*,\\s*");
                if (params.length == 1 && params[0].isEmpty()) {
                    params = new String[0];
                }

                builder.addEventBinding(
                        new EventBinding(eventName, methodName, params));
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
                    ElementTemplate template = parse(templateString);

                    templateCache.put(type,
                            new ElementTemplateCache(template, lastModified));
                    return template;
                }
            }
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }
    }
}
