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

package com.vaadin.generator;

import java.util.Collections;
import java.util.List;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * A class that is used to format javadocs for the generated component classes.
 * 
 * @author Vaadin Ltd.
 */
class JavaDocFormatter {
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
     * markdown text. In order to format them, we parse them first and only them
     * produce the output.
     *
     * @param javaDoc
     *            javadoc string to format
     * @return formatter javadoc string
     */
    String formatJavaDoc(String javaDoc) {
        return renderer.render(parser.parse(javaDoc));
    }

}
