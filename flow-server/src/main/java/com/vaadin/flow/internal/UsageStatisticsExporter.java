/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

import java.io.Serializable;
import java.util.stream.Collectors;

import org.jsoup.nodes.Document;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * A class for exporting {@link UsageStatistics} entries.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 3.0
 */
public class UsageStatisticsExporter implements Serializable {

    /**
     * Export {@link UsageStatistics} entries to a document. It appends a
     * {@code <script>} element to the {@code <body>} element.
     *
     * @param document
     *            the document where the statistic entries to be exported to.
     */
    public static void exportUsageStatisticsToDocument(Document document) {
        String entries = UsageStatistics.getEntries()
                .map(UsageStatisticsExporter::createUsageStatisticsJson)
                .collect(Collectors.joining(","));

        if (!entries.isEmpty()) {
            // Registers the entries in a way that is picked up as a Vaadin
            // WebComponent by the usage stats gatherer
            String builder = "window.Vaadin = window.Vaadin || {};\n"
                    + "window.Vaadin.registrations = window.Vaadin.registrations || [];\n"
                    + "window.Vaadin.registrations.push(" + entries + ");";
            document.body().appendElement("script").text(builder);
        }
    }

    private static String createUsageStatisticsJson(
            UsageStatistics.UsageEntry entry) {
        JsonObject json = Json.createObject();

        json.put("is", entry.getName());
        json.put("version", entry.getVersion());

        return json.toJson();
    }
}
