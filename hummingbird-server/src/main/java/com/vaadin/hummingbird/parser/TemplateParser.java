package com.vaadin.hummingbird.parser;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import com.vaadin.annotations.TemplateEventHandler;
import com.vaadin.hummingbird.kernel.BoundTemplateBuilder;
import com.vaadin.hummingbird.kernel.ElementTemplate;
import com.vaadin.hummingbird.kernel.ModelBinding;
import com.vaadin.hummingbird.kernel.ModelContext;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.hummingbird.kernel.TemplateBuilder;
import com.vaadin.hummingbird.kernel.TemplateScriptHelper;
import com.vaadin.ui.Template;

public class TemplateParser {
    private static final Pattern forDefinitionPattern = Pattern
            .compile("#([^\\s]+)\\s+of\\s+([^\\s]+)");

    public static ElementTemplate parse(String templateString) {
        return parseBuilder(templateString).build();
    }

    private static TemplateBuilder parseBuilder(String templateString) {
        Document bodyFragment = Jsoup.parseBodyFragment(templateString);
        Elements children = bodyFragment.body().children();

        ModelContext context = new ModelContext() {
            @Override
            public Function<String, Supplier<Object>> getBindingFactory(
                    StateNode node) {
                return name -> {
                    if (node.containsKey(name)) {
                        return () -> {
                            return node.get(name);
                        };
                    } else {
                        return null;
                    }
                };
            }
        };

        int childNodeSize = children.size();
        if (childNodeSize != 1) {
            if (childNodeSize == 0) {
                throw new TemplateException("Tempalte is empty");
            } else {
                throw new TemplateException(
                        "Template has multiple root elements");
            }
        }
        BoundTemplateBuilder template = createElementTemplate(children.get(0),
                context);
        return template;
    }

    private static TemplateBuilder createTemplate(Node node,
            ModelContext context) {
        if (node instanceof Element) {
            return createElementTemplate((Element) node, context);
        } else if (node instanceof TextNode) {
            return createTextTemplate((TextNode) node, context);
        } else if (node instanceof DataNode) {
            return crateDataTemplate((DataNode) node, context);
        } else if (node instanceof Comment) {
            return null;
        } else {
            throw new RuntimeException(node.getClass().getName());
        }
    }

    private static TemplateBuilder crateDataTemplate(DataNode node,
            ModelContext context) {
        String data = node.getWholeData();
        return TemplateBuilder.staticText(data);
    }

    private static TemplateBuilder createTextTemplate(TextNode node,
            ModelContext context) {
        String text = node.text();
        if (text.startsWith("{{")) {
            if (!text.endsWith("}}")) {
                throw new RuntimeException(
                        "Invalid text node '" + text + "'. Must end with }}");
            }
            String binding = text.substring(2, text.length() - 2);
            return TemplateBuilder
                    .dynamicText(new ModelBinding(binding, context));
        } else {
            return TemplateBuilder.staticText(text);
        }
    }

    private static BoundTemplateBuilder createElementTemplate(Element element,
            ModelContext context) {
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
                    String outerBinding = matcher.group(2);

                    ModelContext outerContext = context;
                    context = new ModelContext() {
                        @Override
                        public Function<String, Supplier<Object>> getBindingFactory(
                                StateNode node) {
                            StateNode baseNode = node.getParent().getParent();
                            Function<String, Supplier<Object>> baseFactory = outerContext
                                    .getBindingFactory(baseNode);
                            return name -> {
                                if (innerVarName.equals(name)) {
                                    return () -> {
                                        return node;
                                    };
                                } else {
                                    return baseFactory.apply(name);
                                }
                            };
                        }
                    };

                    builder.setForDefinition(
                            new ModelBinding(outerBinding, outerContext),
                            innerVarName);
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
                builder.bindAttribute(attibuteName,
                        new ModelBinding(value, context));
            } else if (name.startsWith("(")) {
                String eventName = name.substring(1, name.length() - 1);

                String eventHandler = value;

                builder.addEventBinding(
                        new EventBinding(eventName, eventHandler));
            } else if (name.startsWith("#")) {
                builder.setAttribute("LOCAL_ID", name.substring(1));
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

    public static ElementTemplate parse(Class<?> type, String fileName) {

        URL resourceUrl = type.getClassLoader().getResource(fileName);
        if (resourceUrl == null) {
            throw new RuntimeException(
                    "File not found from classpath: " + fileName);
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
