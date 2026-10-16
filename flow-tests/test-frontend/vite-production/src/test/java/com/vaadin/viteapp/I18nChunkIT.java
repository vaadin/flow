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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the chunk rewriting of the Vaadin i18n build plugin, which the
 * application triggers with the Hilla translations in {@code i18n-chunk.js}.
 */
public class I18nChunkIT extends BundleAccess {

    private static final String CHUNK_NAME_MARKER = "__VAADIN_I18n_chunkName__";

    private static final Pattern REGISTER_CHUNK = Pattern
            .compile("registerChunk\\(\\s*[\"'`]([^\"'`]+)[\"'`]\\s*\\)");

    @Test
    public void chunkRegistersItselfUnderItsOwnName() throws Exception {
        String bundle = getBundleWithTranslations();
        Matcher matcher = REGISTER_CHUNK.matcher(download(BUILD_PATH + bundle));

        Assert.assertTrue(bundle + " should register itself for translations",
                matcher.find());
        Assert.assertEquals(
                "The chunk should be registered under the name it is served "
                        + "with, which is the name the i18n metadata uses",
                BUILD_PATH.substring(1) + bundle, matcher.group(1));
        Assert.assertFalse(
                bundle + " should not have more than one registerChunk call",
                matcher.find());
    }

    /**
     * Finds the bundle that has the translations of the application, by the
     * marker the i18n plugin replaces with the name of the chunk.
     */
    private String getBundleWithTranslations() throws Exception {
        String entryBundle = getJsBundleName();
        Matcher matcher = Pattern.compile("[\"']\\./([^\"']+\\.js)[\"']")
                .matcher(download(BUILD_PATH + entryBundle));
        while (matcher.find()) {
            String bundle = matcher.group(1);
            String contents = downloadIfAvailable(BUILD_PATH + bundle);
            if (contents != null && contents.contains("i18n.chunk.test")) {
                Assert.assertFalse(
                        bundle + " should have the chunk name marker replaced",
                        contents.contains(CHUNK_NAME_MARKER));
                return bundle;
            }
        }
        throw new IllegalStateException(
                "No bundle with the translations of the application found");
    }

}
