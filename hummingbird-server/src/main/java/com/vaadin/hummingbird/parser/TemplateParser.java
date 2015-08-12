package com.vaadin.hummingbird.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
import com.vaadin.hummingbird.kernel.StaticChildrenElementTemplate;
import com.vaadin.hummingbird.kernel.StaticTextTemplate;

public class TemplateParser {

    public static ElementTemplate parse(String templateString) {
        Document bodyFragment = Jsoup.parseBodyFragment(templateString);
        return createElementTemplate(bodyFragment.body().child(0));
    }

    private static BoundElementTemplate createTemplate(Node node) {
        if (node instanceof Element) {
            return createElementTemplate((Element) node);
        } else if (node instanceof TextNode) {
            return createTextTemplate((TextNode) node);
        } else {
            throw new RuntimeException(node.getClass().getName());
        }
    }

    private static BoundElementTemplate createTextTemplate(TextNode node) {
        String text = node.text();
        if (text.startsWith("{{")) {
            if (!text.endsWith("}}")) {
                throw new RuntimeException();
            }
            return new DynamicTextTemplate(
                    text.substring(2, text.length() - 2));
        } else {
            return new StaticTextTemplate(text);
        }
    }

    private static BoundElementTemplate createElementTemplate(Element element) {
        List<AttributeBinding> bindings = new ArrayList<>();
        Map<String, String> defaultAttributes = new HashMap<>();
        String forDefinition = null;

        for (Attribute a : element.attributes()) {
            String name = a.getKey();
            String value = a.getValue();
            if (name.startsWith("*")) {
                if ("*ng-for".equals(name)) {
                    assert forDefinition == null;
                    forDefinition = value;
                }
            } else if (name.startsWith("[")) {
                bindings.add(new ModelAttributeBinding(
                        name.substring(1, name.length() - 1), value));
            } else {
                defaultAttributes.put(name, value);
            }

        }

        if (forDefinition != null) {
            assert element.childNodeSize() == 1;
            BoundElementTemplate innerTemplate = createTemplate(
                    element.childNode(0));
            return new ForElementTemplate(element.tagName(), bindings,
                    defaultAttributes, forDefinition, innerTemplate);
        } else if (element.childNodeSize() != 0) {
            List<BoundElementTemplate> childTemplates = element.childNodes()
                    .stream().map(TemplateParser::createTemplate)
                    .collect(Collectors.toList());
            return new StaticChildrenElementTemplate(element.tagName(),
                    bindings, defaultAttributes, childTemplates);
        } else {
            return new BoundElementTemplate(element.tagName(), bindings,
                    defaultAttributes);
        }

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
