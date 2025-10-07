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

import java.io.Serializable;
import java.util.stream.Collectors;

import tools.jackson.databind.node.ObjectNode;
import org.jsoup.nodes.Document;

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
        ObjectNode json = JacksonUtils.createObjectNode();

        json.put("is", entry.getName());
        json.put("version", entry.getVersion());

        return json.toString();
    }
}
