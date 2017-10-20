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
package com.vaadin.flow.template.angular.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.ParseSettings;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import com.vaadin.flow.template.angular.DelegateResolver;
import com.vaadin.flow.template.angular.TemplateNode;
import com.vaadin.flow.template.angular.TemplateNodeBuilder;
import com.vaadin.flow.template.angular.TemplateParseException;
import com.vaadin.flow.util.MessageDigestUtil;

/**
 * Parser for an Angular 2-like template syntax.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public class TemplateParser {

    private static final String ROOT_CLARIFICATION = "If the template contains <html> and <body> tags,"
            + " then only the contents of the <body> tag will be used.";

    private static final String INCLUDE_PREFIX = "@include ";

    private static final Collection<TemplateNodeBuilderFactory<?>> FACTORIES = loadFactories();
    private static final Collection<TemplateNodeBuilderFactory<?>> DEFAULT_FACTORIES = loadDefaultFactories();

    /**
     * Maps the hash of a template file's outer HTML (after resolving includes)
     * to the corresponding immutable root template node. The primary reason for
     * reusing template nodes is to avoid leaking memory in the global mapping
     * from template node ids to template node instances.
     */
    private static final ConcurrentHashMap<ByteBuffer, TemplateNode> templateCache = new ConcurrentHashMap<>();

    private TemplateParser() {
        // Only static methods
    }

    private String convertStreamToString(java.io.InputStream is) {
        try(java.util.Scanner s = new java.util.Scanner(is, "UTF-8")) {
            return s.useDelimiter("\\A").hasNext() ? s.next() : "";
        } catch (Exception e) {
            throw new TemplateParseException("Error reading template data", e);
        }
    }

    /**
     * Parses the template from the given input stream to a tree of template
     * nodes.
     *
     * @param templateStream
     *            the input stream containing the template to parse, not
     *            <code>null</code>
     * @param templateResolver
     *            the resolver to use to look up included files
     * @return the template node at the root of the parsed template tree
     */
    public static TemplateNode parse(InputStream templateStream,
            TemplateResolver templateResolver) {

        assert templateStream != null;
        String templateString = convertStreamToString(templateStream);
        Parser parser = Parser.htmlParser();
        parser.settings(new ParseSettings(true, true)); // tag, attribute preserve case
        Document document = parser.parseInput(templateString, "");

        return parse(document, templateResolver);
    }

    /**
     * Parses the given template string to a tree of template nodes.
     *
     * @param templateString
     *            the template string to parse, not <code>null</code>
     * @param templateResolver
     *            the resolver to use to look up included files
     * @return the template node at the root of the parsed template tree
     */
    public static TemplateNode parse(String templateString,
            TemplateResolver templateResolver) {
        assert templateString != null;

        Document document = Jsoup.parseBodyFragment(templateString);

        return parse(document, templateResolver);
    }

    private static Collection<TemplateNodeBuilderFactory<?>> loadFactories() {
        Collection<TemplateNodeBuilderFactory<?>> factories = new ArrayList<>();
        factories.add(new ForElementBuilderFactory());
        factories.add(new DataNodeFactory());
        return factories;
    }

    private static Collection<TemplateNodeBuilderFactory<?>> loadDefaultFactories() {
        Collection<TemplateNodeBuilderFactory<?>> factories = new ArrayList<>();
        factories.add(new DefaultTextModelBuilderFactory());
        factories.add(new DefaultElementBuilderFactory());
        return factories;
    }

    private static Element getRootElement(Document bodyFragment,
            TemplateResolver templateResolver) {
        Elements children = bodyFragment.body().children();

        int childNodeSize = children.size();
        if (childNodeSize != 1) {
            if (childNodeSize == 0) {
                throw new TemplateParseException(
                        "AngularTemplate must not be empty. " + ROOT_CLARIFICATION);
            } else {
                throw new TemplateParseException(
                        "AngularTemplate must not have multiple root elements. "
                                + ROOT_CLARIFICATION);
            }
        }

        Element rootElement = children.get(0);

        populateIncludes(rootElement, templateResolver);

        return rootElement;
    }

    private static TemplateNode parse(Document bodyFragment,
            TemplateResolver templateResolver) {
        Element rootElement = getRootElement(bodyFragment, templateResolver);

        byte[] hash = MessageDigestUtil.sha256(rootElement.outerHtml());

        // Identity is based on contents for ByteBuffer, but not for byte[]
        ByteBuffer key = ByteBuffer.wrap(hash);

        return templateCache.computeIfAbsent(key, ignore -> parse(rootElement));
    }

    private static TemplateNode parse(Element rootElement) {
        Optional<TemplateNodeBuilder> templateBuilder = createBuilder(
                rootElement);
        assert templateBuilder.isPresent();
        List<? extends TemplateNode> nodes = templateBuilder.get().build(null);
        assert nodes.size() == 1;
        return nodes.get(0);
    }

    private static void populateIncludes(Element element,
            TemplateResolver resolver) {
        // Explicitly collect since we cannot do replacements while traversing
        List<TextNode> includeNodes = collectIncludeNodes(element);

        includeNodes.forEach(textNode -> splitInclude(textNode, resolver));
    }

    private static void splitInclude(TextNode nodeToSplit,
            TemplateResolver resolver) {
        while (nodeToSplit != null) {
            String text = nodeToSplit.getWholeText();

            int includeStart = text.indexOf(INCLUDE_PREFIX);
            if (includeStart == -1) {
                return;
            }

            int includeEnd = text.indexOf('@',
                    includeStart + INCLUDE_PREFIX.length());
            if (includeEnd == -1) {
                return;
            }

            int includeLength = includeEnd - includeStart;

            String includeFileName = text.substring(
                    includeStart + INCLUDE_PREFIX.length(), includeEnd).trim();

            Element replacement = loadInclude(includeFileName, resolver);

            // Split the original node into an untouched prefix, the actual
            // include statement and (if there's more text) a remainder
            TextNode includeStatement = nodeToSplit.splitText(includeStart);
            TextNode remainder = null;
            if (includeStatement.getWholeText().length() > includeLength + 1) {
                remainder = includeStatement.splitText(includeLength + 1);
            }

            includeStatement.replaceWith(replacement);

            // Continue splitting the rest of the node
            nodeToSplit = remainder;
        }
    }

    private static Element loadInclude(String includeFileName,
            TemplateResolver resolver) {
        // Need a new resolver so that includes from the included file are
        // relative to that file (directory)
        DelegateResolver subResolver = new DelegateResolver(resolver,
                getFolder(includeFileName));
        try (InputStream templateContentStream = resolver
                .resolve(includeFileName)) {
            Document document = Jsoup.parse(templateContentStream, null, "");
            return getRootElement(document, subResolver);
        } catch (IOException e) {
            throw new TemplateParseException(
                    "Unable to read template include for '" + includeFileName
                            + "'",
                    e);
        }
    }

    private static String getFolder(String relativeFilename) {
        String folder = new File(relativeFilename).getParent();
        if (folder != null) {
            return folder;
        } else {
            return ".";
        }
    }

    private static List<TextNode> collectIncludeNodes(Element element) {
        List<TextNode> includeNodes = new ArrayList<>();
        new NodeTraversor(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                // nop
            }

            @Override
            public void tail(Node node, int depth) {
                if (node instanceof TextNode) {
                    TextNode textNode = (TextNode) node;
                    String text = textNode.getWholeText();
                    if (text.contains(INCLUDE_PREFIX)) {
                        includeNodes.add(textNode);
                    }
                }
            }
        }).traverse(element);
        return includeNodes;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Optional<TemplateNodeBuilder> createBuilder(Node node) {
        if (node instanceof Comment) {
            return Optional.empty();
        }
        List<TemplateNodeBuilderFactory<?>> list = filterApplicable(FACTORIES,
                node);
        if (list.isEmpty()) {
            list = filterApplicable(DEFAULT_FACTORIES, node);
            if (list.isEmpty()) {
                throw new IllegalArgumentException(
                        "Unsupported node type: " + node.getClass().getName());
            }
        }
        assert list.size() == 1;

        TemplateNodeBuilderFactory factory = list.get(0);
        return Optional
                .of(factory.createBuilder(node, n -> createBuilder((Node) n)));
    }

    private static List<TemplateNodeBuilderFactory<?>> filterApplicable(
            Collection<TemplateNodeBuilderFactory<?>> factories, Node node) {
        return factories.stream().filter(factory -> factory.isApplicable(node))
                .collect(Collectors.toList());
    }

}
