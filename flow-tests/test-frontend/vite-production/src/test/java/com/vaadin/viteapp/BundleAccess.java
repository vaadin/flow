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

import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

/**
 * Reads the files of the production bundle from the running application, for
 * the tests that assert on what the frontend build produced.
 */
public class BundleAccess {

    protected static final String BUILD_PATH = "/VAADIN/build/";

    private static final Pattern ENTRY_BUNDLE = Pattern
            .compile(".* src=\"\\./VAADIN/build/([^\"]*)\".*", Pattern.DOTALL);

    protected String getRootURL() {
        return "http://localhost:8888";
    }

    /**
     * Returns the name of the bundle that index.html loads.
     */
    protected String getJsBundleName() throws Exception {
        Matcher matcher = ENTRY_BUNDLE.matcher(download("/index.html"));
        if (!matcher.matches()) {
            throw new IllegalStateException("No script found");
        }
        return matcher.group(1);
    }

    protected String download(String path) throws Exception {
        return IOUtils.toString(new URL(getRootURL() + path),
                StandardCharsets.UTF_8);
    }

    /**
     * Downloads the given path, or returns {@code null} when the application
     * does not serve it.
     */
    protected String downloadIfAvailable(String path) throws Exception {
        try {
            return download(path);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

}
