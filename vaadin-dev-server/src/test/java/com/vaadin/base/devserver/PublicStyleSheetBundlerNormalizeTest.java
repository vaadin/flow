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
package com.vaadin.base.devserver;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PublicStyleSheetBundlerNormalizeTest {

    @Test
    public void normalize_contextProtocol_isStripped() {
        assertEquals("css/app.css",
                PublicStyleSheetBundler.normalizeUrl("context://css/app.css"));
    }

    @Test
    public void normalize_leadingSlash_isRemoved() {
        assertEquals("css/app.css",
                PublicStyleSheetBundler.normalizeUrl("/css/app.css"));
    }

    @Test
    public void normalize_relativeDotSlash_isRemoved() {
        assertEquals("css/app.css",
                PublicStyleSheetBundler.normalizeUrl("./css/app.css"));
    }

    @Test
    public void normalize_queryAndHash_areRemoved() {
        assertEquals("css/app.css", PublicStyleSheetBundler
                .normalizeUrl("/css/app.css?v=123#hash"));
    }

    @Test
    public void normalize_backslashes_areConverted() {
        assertEquals("/css/app.css",
                PublicStyleSheetBundler.normalizeUrl("\\css\\app.css"));
    }
}
