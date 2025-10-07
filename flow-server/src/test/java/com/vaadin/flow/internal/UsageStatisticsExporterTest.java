/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.internal;

import java.util.stream.Collectors;

import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.Assert.assertEquals;

public class UsageStatisticsExporterTest {

    @Test
    public void should_append_script_element_to_the_body() {
        Document document = new Document("");
        Element html = document.appendElement("html");
        html.appendElement("body");

        UsageStatisticsExporter.exportUsageStatisticsToDocument(document);

        String entries = UsageStatistics.getEntries().map(entry -> {
            ObjectNode json = JacksonUtils.createObjectNode();

            json.put("is", entry.getName());
            json.put("version", entry.getVersion());

            return json.toString();
        }).collect(Collectors.joining(","));

        String expected = StringUtil
                .normaliseWhitespace("window.Vaadin = window.Vaadin || {};\n"
                        + "window.Vaadin.registrations = window.Vaadin.registrations || [];\n"
                        + "window.Vaadin.registrations.push(" + entries + ");");

        Elements bodyInlineElements = document.body()
                .getElementsByTag("script");
        String htmlContent = bodyInlineElements.get(0).childNode(0).outerHtml();
        htmlContent = htmlContent.replace("\r", "");
        htmlContent = htmlContent.replace("\n", " ");
        assertEquals(StringUtil.normaliseWhitespace(expected), htmlContent);
    }
}
