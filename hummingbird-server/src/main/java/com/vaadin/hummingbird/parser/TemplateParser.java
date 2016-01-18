package com.vaadin.hummingbird.parser;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Objects;
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
import com.vaadin.hummingbird.kernel.ListNode;
import com.vaadin.hummingbird.kernel.ModelBinding;
import com.vaadin.hummingbird.kernel.ModelContext;
import com.vaadin.hummingbird.kernel.ScriptModelBinding;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.hummingbird.kernel.StaticModelBinding;
import com.vaadin.hummingbird.kernel.TemplateBuilder;
import com.vaadin.hummingbird.kernel.TemplateScriptHelper;
import com.vaadin.ui.Template;

public class TemplateParser {
    private static final Pattern forDefinitionPattern = Pattern
            .compile("#([^\\s]+)\\s+of\\s+([^\\s]+)");

    /**
     * #variable = (index|odd|even|last)
     */
    private static final Pattern forLoopVariablesMappingPattern = Pattern
            .compile("#([^\\s]+)\\s*=\\s*(index|last|even|odd)\\s*");

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
                return new TemplateScriptHelper.NodeBindingFactory(node);
            }
        };

        int childNodeSize = children.size();
        if (childNodeSize != 1) {
            if (childNodeSize == 0) {
                throw new TemplateException("Template is empty");
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
            throw new RuntimeException(
                    "Unknown node type: " + node.getClass().getName());
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
            return TemplateBuilder.dynamicText(createBinding(binding, context));
        } else {
            return TemplateBuilder.staticText(text);
        }
    }

    private static ModelBinding createBinding(String expression,
            ModelContext context) {
        // Use static binding instead of passing through Nashorn for simple
        // foo.bar bindings
        if (StaticModelBinding.isStaticExpression(expression)) {
            return new StaticModelBinding(expression, context);
        } else {
            return new ScriptModelBinding(expression, context);
        }
    }

    private static BoundTemplateBuilder createElementTemplate(Element element,
            ModelContext context) {
        BoundTemplateBuilder builder;
        String is = element.attr("is");
        if (is.isEmpty()) {
            builder = TemplateBuilder.withTag(element.tagName());
        } else {
            builder = TemplateBuilder.withTag(element.tagName(), is);
        }

        for (Attribute a : element.attributes()) {
            String name = a.getKey();
            if (name.startsWith("*")) {
                String value = a.getValue();
                if ("*ng-for".equals(name)) {
                    String[] parts = value.split(";");

                    // First part must be the loop definition
                    String loopDefinition = parts[0].trim();
                    Matcher matcher = forDefinitionPattern
                            .matcher(loopDefinition);
                    if (!matcher.matches()) {
                        throw new RuntimeException(
                                "Unable to parse the loop part of the ng-for statement: '"
                                        + loopDefinition + "'");
                    }

                    String indexV = null;
                    String lastV = null;
                    String evenV = null;
                    String oddV = null;

                    for (int i = 1; i < parts.length; i++) {
                        String part = parts[i].trim();
                        if (part.isEmpty()) {
                            continue;
                        }

                        Matcher indexMatcher = forLoopVariablesMappingPattern
                                .matcher(part);
                        if (indexMatcher.matches()) {
                            String variable = indexMatcher.group(1);
                            String type = indexMatcher.group(2); // index,even,odd,last
                            if (type.equals("index")) {
                                indexV = variable;
                            } else if (type.equals("odd")) {
                                oddV = variable;
                            } else if (type.equals("even")) {
                                evenV = variable;
                            } else if (type.equals("last")) {
                                lastV = variable;
                            } else {
                                throw new RuntimeException(
                                        type + " not yet implemented");
                            }
                        } else {
                            throw new RuntimeException(
                                    "Unable to parse the additional part of the ng-for statement: '"
                                            + part + "'");

                        }
                    }

                    final String indexVariable = indexV;
                    final String lastVariable = lastV;
                    final String oddVariable = oddV;
                    final String evenVariable = evenV;
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
                                } else if (Objects.equals(name,
                                        indexVariable)) {
                                    ListNode listNode = ((ListNode) node
                                            .getParent());
                                    return () -> {
                                        return listNode.indexOf(node);
                                    };
                                } else if (Objects.equals(name, evenVariable)) {
                                    ListNode listNode = ((ListNode) node
                                            .getParent());
                                    return () -> {
                                        return listNode.indexOf(node) % 2 == 0;
                                    };
                                } else if (Objects.equals(name, oddVariable)) {
                                    ListNode listNode = ((ListNode) node
                                            .getParent());
                                    return () -> {
                                        return listNode.indexOf(node) % 2 != 0;
                                    };
                                } else if (Objects.equals(name, lastVariable)) {
                                    return () -> {
                                        ListNode listNode = ((ListNode) node
                                                .getParent());
                                        return listNode.indexOf(
                                                node) == (listNode.size() - 1);
                                    };
                                } else {
                                    return baseFactory.apply(name);
                                }
                            };
                        }
                    };

                    builder.setForDefinition(
                            createBinding(outerBinding, outerContext),
                            innerVarName, indexVariable, evenVariable,
                            oddVariable, lastVariable);
                } else {
                    throw new RuntimeException(
                            "Unsupported * attribute: " + name);
                }
            }
        }

        for (Attribute a : element.attributes()) {
            String name = a.getKey();
            String value = a.getValue();
            if (name.equals("is")) {
                // Handled above
            } else if (name.startsWith("*")) {
                // Handled above
                continue;
            } else if (name.startsWith("[")) {
                String attibuteName = name.substring(1, name.length() - 1);
                builder.bindAttribute(attibuteName,
                        createBinding(value, context));
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
