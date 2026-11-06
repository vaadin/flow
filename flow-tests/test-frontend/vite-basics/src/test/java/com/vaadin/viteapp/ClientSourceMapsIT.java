/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.viteapp;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.testutil.SourceMapTestUtil;

import static org.junit.Assert.assertTrue;

/**
 * Verifies that the Flow client can be debugged in development mode. The client
 * is published as compiled JavaScript with a sourcemap beside it, so the dev
 * server has to chain that sourcemap onto the one it emits itself for the
 * browser to show the TypeScript the client was written in.
 */
public class ClientSourceMapsIT extends ViteDevModeIT {

    private static final String CLIENT_FOLDER = FrontendUtils.DEFAULT_FRONTEND_DIR
            + FrontendUtils.GENERATED + FrontendUtils.JAR_RESOURCES_FOLDER
            + "/internal/client/bootstrap/";
    private static final String CLIENT_MODULE = "Bootstrapper.js";
    private static final String CLIENT_SOURCE = "Bootstrapper.ts";

    private static final Pattern SOURCE_MAPPING_URL = Pattern
            .compile("//# sourceMappingURL=(\\S+)");
    private static final String INLINE_SOURCE_MAP = "data:application/json;base64,";

    @Test
    public void clientSourceMapPointsToTypeScriptSources() throws Exception {
        String client = download(CLIENT_FOLDER + CLIENT_MODULE);

        Matcher matcher = SOURCE_MAPPING_URL.matcher(client);
        assertTrue(CLIENT_MODULE
                + " should be served with a sourcemap, the dev server "
                + "returned " + client, matcher.find());

        List<String> sources = SourceMapTestUtil.assertSourceMapUsable(
                CLIENT_MODULE, getSourceMap(matcher.group(1)));

        assertTrue(
                CLIENT_MODULE + " should have a sourcemap referring to "
                        + CLIENT_SOURCE + ", only found " + sources,
                sources.stream().anyMatch(
                        source -> source.endsWith("/" + CLIENT_SOURCE)));
    }

    /**
     * Returns the contents of the sourcemap the given sourcemap url refers to.
     * The dev server inlines the sourcemap into the module it serves, while a
     * plain file is served with the sourcemap in a file next to it.
     */
    private String getSourceMap(String sourceMappingUrl) throws Exception {
        if (sourceMappingUrl.startsWith(INLINE_SOURCE_MAP)) {
            byte[] sourceMap = Base64.getDecoder().decode(
                    sourceMappingUrl.substring(INLINE_SOURCE_MAP.length()));
            return new String(sourceMap, StandardCharsets.UTF_8);
        }
        return download(CLIENT_FOLDER + sourceMappingUrl);
    }

    private String download(String fileInProject) throws Exception {
        return IOUtils.toString(getFsUrl(fileInProject),
                StandardCharsets.UTF_8);
    }

}
