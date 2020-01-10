package com.vaadin.flow.internal;

import java.util.stream.Collectors;

import elemental.json.Json;
import elemental.json.JsonObject;
import org.jsoup.nodes.Document;

/**
 * A class for exporting {@link UsageStatistics} entries.
 *
 * @author Vaadin Ltd
 * @since 3.0
 */
public class UsageStatisticsExporter {

    private static final String SCRIPT_TAG = "script";

    /**
     * Export {@link UsageStatistics} entries to a document. It appends a
     * <code><script></code> element to the <code><body></code>
     *
     * @param document the document where the statistic entries to be exported to.
     */
    public static void exportUsageStatisticsToDocument(Document document) {
        String entries = UsageStatistics.getEntries()
                .map(UsageStatisticsExporter::createUsageStatisticsJson)
                .collect(Collectors.joining(","));

        if (!entries.isEmpty()) {
            // Registers the entries in a way that is picked up as a Vaadin
            // WebComponent by the usage stats gatherer
            StringBuilder builder = new StringBuilder();
            builder.append("window.Vaadin = window.Vaadin || {};\n")
                    .append("window.Vaadin.registrations = window.Vaadin.registrations || [];\n")
                    .append("window.Vaadin.registrations.push(")
                    .append(entries)
                    .append(");");
            document.body().appendElement(SCRIPT_TAG).text(builder.toString());
        }
    }

    private static String createUsageStatisticsJson(UsageStatistics.UsageEntry entry) {
        JsonObject json = Json.createObject();

        json.put("is", entry.getName());
        json.put("version", entry.getVersion());

        return json.toJson();
    }
}
