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
package com.vaadin.flow.server.startup.testdata;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import com.vaadin.flow.di.ResourceProvider;

public class TestResourceProvider implements ResourceProvider {

    @Override
    public URL getApplicationResource(String path) {
        return null;
    }

    @Override
    public List<URL> getApplicationResources(String path) throws IOException {
        return null;
    }

    @Override
    public URL getClientResource(String path) {
        return null;
    }

    @Override
    public InputStream getClientResourceAsStream(String path)
            throws IOException {
        return null;
    }

}
