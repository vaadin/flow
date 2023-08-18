/*
 * Copyright 2000-2023 Vaadin Ltd.
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
package com.vaadin.flow.component.littemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.StringUtil;

/**
 * Parse statistics data provided by webpack.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 *
 * @see LitTemplateParser
 */
public final class BundleLitParser {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(BundleLitParser.class);

    /**
     * Lit template pattern matches the template getter
     *
     * <pre>
     *     render() {
     *       return html`
     *         &lt;style&gt;
     *           .response { margin-top: 10px`; }
     *         &lt;/style&gt;
     *         &lt;paper-checkbox checked=&quot;${liked}&quot;&gt;I like web components.&lt;/paper-checkbox&gt;
     *
     *         &lt;div id=&quot;test&quot; ?hidden=&quot;${liked}&quot; class=&quot;response&quot;&gt;Web components like you, too.&lt;/div&gt;
     *       `;
     *     }
     * </pre>
     *
     * <p>
     * <code>render\(\)[\s]*\{</code> finds the template getter method
     * <p>
     * <code>[\s\S]*return[\s]*html[\s]*(\`)</code> finds the return statement
     * <p>
     * </p>
     * <code>([\s\S]*)</code> captures all text until we encounter the end
     * character with <code>\1;}</code> e.g. <code>';}</code>
     */
    private static final Pattern LIT_TEMPLATE_PATTERN = Pattern.compile(
            "render\\(\\)[\\s]*\\{[\\s\\S]*return[\\s]*html[\\s]*(\\`)([\\s\\S]*?)\\1;[\\s]*\\}");

    private static final String TEMPLATE_TAG_NAME = "template";

    private BundleLitParser() {
    }

    /**
     * Get the Lit template element for the given polymer template source.
     *
     * @param fileName
     *            name of the handled file
     * @param source
     *            source js to get template element from
     * @return template element or {code null} if not found
     */
    public static Element parseLitTemplateElement(String fileName,
            String source) {
        Document templateDocument = null;
        String content = StringUtil.removeComments(source);
        Matcher templateMatcher = LIT_TEMPLATE_PATTERN.matcher(content);

        // GroupCount should be 2 as the first group contains `|'|" depending
        // on what was in template return html' and the second is the
        // template contents.
        if (templateMatcher.find() && templateMatcher.groupCount() == 2) {
            String group = templateMatcher.group(2);
            LOGGER.trace("Found regular Lit template content was {}", group);

            templateDocument = Jsoup.parse(group);
            LOGGER.trace("The parsed template document was {}",
                    templateDocument);
            Element template = templateDocument
                    .createElement(TEMPLATE_TAG_NAME);
            Element body = templateDocument.body();
            templateDocument.body().children().stream()
                    .filter(node -> !node.equals(body))
                    .forEach(template::appendChild);

            return template;
        }
        LOGGER.warn("No lit template data found in {} sources.", fileName);
        return null;
    }

}
