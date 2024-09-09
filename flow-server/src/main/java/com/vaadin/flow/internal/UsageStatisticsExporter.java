/*
 * Copyright 2000-2024 Vaadin Ltd.
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

import org.jsoup.nodes.Document;

/**
 * A class for exporting {@link UsageStatistics} entries.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 3.0
 * @deprecated server side statistics should not be sent to the client. Will be
 *             removed without replacement.
 */
@Deprecated(since = "24.5", forRemoval = true)
public class UsageStatisticsExporter implements Serializable {

    /**
     * Export {@link UsageStatistics} entries to a document. It appends a
     * {@code <script>} element to the {@code <body>} element.
     *
     * @param document
     *            the document where the statistic entries to be exported to.
     * @deprecated server side statistics should not be sent to the client. The
     *             method throws an exception if called. Will be removed without
     *             replacement.
     */
    @Deprecated
    public static void exportUsageStatisticsToDocument(Document document) {
        throw new UnsupportedOperationException(
                "Server side usage statistics must not be exported to the client");
    }

}
