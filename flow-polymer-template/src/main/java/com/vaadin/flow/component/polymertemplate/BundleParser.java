/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.polymertemplate;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.StringUtil;

/**
 * Parse statistics data provided by webpack.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 2.0
 *
 * @see NpmTemplateParser
 * @deprecated Use {@code BundleLitParser} to parse Lit template since polymer
 *             template is deprecated, we recommend you to use
 *             {@code LitTemplate} instead. Read more details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 */
@Deprecated
public final class BundleParser {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(BundleParser.class);

    /**
     * Polymer template pattern matches the template getter
     *
     * <pre>
     *     static get template() {
     *       return html`
     *         &lt;style&gt;
     *           .response { margin-top: 10px`; }
     *         &lt;/style&gt;
     *         &lt;paper-checkbox checked=&quot;{{liked}}&quot;&gt;I like web components.&lt;/paper-checkbox&gt;
     *
     *         &lt;div id=&quot;test&quot; hidden$=&quot;[[!liked]]&quot; class=&quot;response&quot;&gt;Web components like you, too.&lt;/div&gt;
     *       `;
     *     }
     * </pre>
     *
     * <p>
     * <code>get[\s]*template\(\)[\s]*\{</code> finds the template getter method
     * <p>
     * <code>[\s]*return[\s]*html([\`|\'|\"])</code> finds the return statement
     * and captures the used string character
     * <p>
     *
     * <code>([\s\S]*)\1;[\s]*\}</code> captures all text until we encounter the
     * end character with <code>;}</code> e.g. <code>';}</code>
     */
    private static final Pattern POLYMER_TEMPLATE_PATTERN = Pattern.compile(
            "get[\\s]*template\\(\\)[\\s]*\\{[\\s]*return[\\s]*html[\\s]*([\\`\\'\\\"])([\\s\\S]*)\\1;[\\s]*\\}");

    private static final Pattern NO_TEMPLATE_PATTERN = Pattern.compile(
            "innerHTML[\\s]*=[\\s]*([\\`\\'\\\"])([\\s]*<dom-module\\s+[\\s\\S]*)\\1;");

    private static final String TEMPLATE_TAG_NAME = "template";

    private BundleParser() {
    }

    /**
     * Get the Polymer template element for the given polymer template source.
     *
     * @param fileName
     *            name of the handled file
     * @param source
     *            source js to get template element from
     * @return template element or {code null} if not found
     */
    public static Element parseTemplateElement(String fileName, String source) {
        Document templateDocument = null;
        String content = StringUtil.removeComments(source);
        Matcher templateMatcher = POLYMER_TEMPLATE_PATTERN.matcher(content);
        Matcher noTemplateMatcher = NO_TEMPLATE_PATTERN.matcher(content);

        // GroupCount should be 2 as the first group contains `|'|" depending
        // on what was in template return html' and the second is the
        // template contents.
        if (templateMatcher.find() && templateMatcher.groupCount() == 2) {
            String group = templateMatcher.group(2);
            LOGGER.trace("Found regular Polymer 3 template content was {}",
                    group);

            templateDocument = Jsoup.parse(group);
            LOGGER.trace("The parsed template document was {}",
                    templateDocument);
        } else {
            Element template = tryParsePolymer2(templateDocument,
                    noTemplateMatcher);
            if (template != null) {
                return template;
            }
        }
        if (templateDocument == null) {
            LOGGER.warn("No polymer template data found in {} sources.",
                    fileName);

            templateDocument = new Document("");
            templateDocument
                    .appendChild(templateDocument.createElement("body"));
        }

        Element template = templateDocument.createElement(TEMPLATE_TAG_NAME);
        Element body = templateDocument.body();
        templateDocument.body().children().stream()
                .filter(node -> !node.equals(body))
                .forEach(template::appendChild);

        return template;
    }

    private static Element tryParsePolymer2(Document templateDocument,
            Matcher noTemplateMatcher) {
        while (noTemplateMatcher.find()
                && noTemplateMatcher.groupCount() == 2) {
            String group = noTemplateMatcher.group(2);
            LOGGER.trace(
                    "Found Polymer 2 style insertion as a Polymer 3 template content {}",
                    group);

            templateDocument = Jsoup.parse(group);
            LOGGER.trace("The parsed template document was {}",
                    templateDocument);
            Optional<Element> domModule = JsoupUtils
                    .getDomModule(templateDocument, null);
            if (!domModule.isPresent()) {
                continue;
            }
            JsoupUtils.removeCommentsRecursively(domModule.get());
            Elements templates = domModule.get()
                    .getElementsByTag(TEMPLATE_TAG_NAME);
            if (templates.isEmpty()) {
                continue;
            }
            return templates.get(0);
        }
        return null;
    }

}
