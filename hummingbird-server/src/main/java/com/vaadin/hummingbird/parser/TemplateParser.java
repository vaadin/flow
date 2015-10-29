package com.vaadin.hummingbird.parser;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
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
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

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

    private static class Context {
        private Element element;
        private String outerName;
        private String innerVar;

        public Context(Element element, String outerName, String innerVar) {
            this.element = element;
            this.outerName = outerName;
            this.innerVar = innerVar;
        }
    }

    private static class Scope {
        private LinkedList<Context> contexts = new LinkedList<>();

        private Set<List<String>> seenPaths = new HashSet<>();

        public ModelPath getPath(String definition) {

            List<String> globalPath = new ArrayList<>();
            List<String> path = Arrays.asList(definition.split("\\."));
            int depth = 0;
            Iterator<Context> iterator = contexts.iterator();
            while (iterator.hasNext()) {
                Context context = iterator.next();
                if (path.isEmpty()) {
                    throw new RuntimeException(definition);
                }

                if (context.innerVar.equals(path.get(0))) {
                    path = path.subList(1, path.size());
                    globalPath.add(0, context.outerName);
                    break;
                } else {
                    depth++;
                }
            }

            while (iterator.hasNext()) {
                Context context = iterator.next();
                globalPath.add(0, context.outerName);
            }
            globalPath.addAll(path);

            seenPaths.add(globalPath);

            path = new ArrayList<>(path);
            for (int i = 0; i < depth; i++) {
                path.add(0, "..");
            }

            return new ModelPath(definition, path);
        }

        public void enterContext(Element element, String outerName,
                String innerName) {
            contexts.addFirst(new Context(element, outerName, innerName));
        }

        public void endContext(Element element) {
            if (!contexts.isEmpty() && contexts.getFirst().element == element) {
                contexts.removeFirst();
            }
        }
    }

    public static ElementTemplate parse(String templateString) {
        return parseBuilder(templateString).build();
    }

    private static TemplateBuilder parseBuilder(String templateString) {
        Document bodyFragment = Jsoup.parseBodyFragment(templateString);
        Elements children = bodyFragment.body().children();
        Scope scope = new Scope();

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
                scope);
        return template;
    }

    private static TemplateBuilder createTemplate(Node node, Scope scope) {
        if (node instanceof Element) {
            return createElementTemplate((Element) node, scope);
        } else if (node instanceof TextNode) {
            return createTextTemplate((TextNode) node, scope);
        } else if (node instanceof DataNode) {
            return crateDataTemplate((DataNode) node, scope);
        } else if (node instanceof Comment) {
            return null;
        } else {
            throw new RuntimeException(node.getClass().getName());
        }
    }

    private static TemplateBuilder crateDataTemplate(DataNode node,
            Scope scope) {
        String data = node.getWholeData();
        return TemplateBuilder.staticText(data);
    }

    private static TemplateBuilder createTextTemplate(TextNode node,
            Scope scope) {
        String text = node.text();
        if (text.startsWith("{{")) {
            if (!text.endsWith("}}")) {
                throw new RuntimeException();
            }
            String modelPath = text.substring(2, text.length() - 2);
            return TemplateBuilder.dynamicText(scope.getPath(modelPath));
        } else {
            return TemplateBuilder.staticText(text);
        }
    }

    private static BoundTemplateBuilder createElementTemplate(Element element,
            Scope scope) {
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
                    String outerVarName = matcher.group(2);
                    ModelPath listPath = scope.getPath(outerVarName);

                    scope.enterContext(element, listPath.getNodeProperty(),
                            innerVarName);

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
                        scope.getPath(value)));
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
            TemplateBuilder childTemplate = createTemplate(node, scope);
            if (childTemplate != null) {
                builder.addChild(childTemplate);
            }
        }

        scope.endContext(element);
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
