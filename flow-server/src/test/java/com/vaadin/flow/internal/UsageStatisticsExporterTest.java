package com.vaadin.flow.internal;

import java.util.stream.Collectors;

import elemental.json.Json;
import elemental.json.JsonObject;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UsageStatisticsExporterTest {

    @Test
    public void should_append_script_element_to_the_body() {
        Document document = new Document("");
        Element html = document.appendElement("html");
        html.appendElement("body");

        UsageStatisticsExporter.exportUsageStatisticsToDocument(document);

        String entries = UsageStatistics.getEntries().map(entry -> {
            JsonObject json = Json.createObject();

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
        assertEquals(StringUtil.normaliseWhitespace(expected),
                bodyInlineElements.get(0).childNode(0).outerHtml());
    }
}
