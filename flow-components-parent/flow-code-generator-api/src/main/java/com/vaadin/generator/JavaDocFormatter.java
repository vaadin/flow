/*
 * Copyright 2000-2019 Vaadin Ltd.
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

package com.vaadin.generator;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Formats javadocs for the generated component classes.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
class JavaDocFormatter {
    private static final String JAVA_DOC_CODE_SECTION = "{@code $1}";
    private static final String JAVA_DOC_CODE_SECTION_MULTI = "{@code $2}";
    private static final String JAVA_DOC_CLOSE_ESCAPED = "&#42;&#47;";

    private static final Pattern MULTI_LINE_CODE_PARTS = Pattern
            .compile("```(js)?\\s*?(.*?)\\s*?```", Pattern.DOTALL);
    private static final Pattern MULTI_LINE_CODE_PARTS_HTML = Pattern
            .compile("```html\\s*(.*?)\\s*```", Pattern.DOTALL);
    private static final Pattern SINGLE_LINE_CODE_PARTS = Pattern
            .compile("`\\s*(.*?)\\s*`");
    private static final Pattern JAVA_DOC_CLOSE = Pattern.compile("\\*/");

    private final HtmlRenderer renderer;
    private final Parser parser;

    /** Creates a new javadoc formatter instance. */
    JavaDocFormatter() {
        List<Extension> extensions = Collections
                .singletonList(TablesExtension.create());
        parser = Parser.builder().extensions(extensions).build();
        renderer = HtmlRenderer.builder().extensions(extensions).build();
    }

    /**
     * Formats a javadoc string as much as possible.
     *
     * Currently javadocs are based on polymer-analyzer output, which is a
     * markdown text. In order to format them, we parse them first and only then
     * produce the output.
     *
     * @param javaDoc
     *            javadoc string to format
     * @return formatter javadoc string
     */
    String formatJavaDoc(String javaDoc) {
        return postFormat(renderer.render(parser.parse(preFormat(javaDoc))));
    }

    private String preFormat(String javaDoc) {
        return String.format("%s%n%n%s",
                "Description copied from corresponding location in WebComponent:",
                replaceCodeParts(javaDoc));
    }

    private String postFormat(String javaDoc) {
        return escapeCommentCloseSign(javaDoc);
    }

    private String replaceCodeParts(String documentation) {
        return replaceByPattern(
                replaceByPattern(replaceHtml(documentation),
                        MULTI_LINE_CODE_PARTS, JAVA_DOC_CODE_SECTION_MULTI),
                SINGLE_LINE_CODE_PARTS, JAVA_DOC_CODE_SECTION);
    }

    private String escapeCommentCloseSign(String documentation) {
        return replaceByPattern(documentation, JAVA_DOC_CLOSE,
                JAVA_DOC_CLOSE_ESCAPED);
    }

    private String replaceHtml(String documentation) {
        Matcher matcher = MULTI_LINE_CODE_PARTS_HTML.matcher(documentation);
        while (matcher.find()) {
            String html = matcher.group(1);
            html = StringEscapeUtils.escapeHtml4(html);
            documentation = documentation.replace(matcher.group(), html);
        }
        return documentation;
    }

    private String replaceByPattern(String original, Pattern pattern,
            String replacement) {
        return pattern.matcher(original).replaceAll(replacement);
    }
}
