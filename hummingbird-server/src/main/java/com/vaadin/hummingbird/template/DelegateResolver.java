/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.template;

import java.io.IOException;
import java.io.InputStream;

import com.vaadin.hummingbird.template.parser.TemplateResolver;

public class DelegateResolver implements TemplateResolver {
    private TemplateResolver parentResolver;
    private String relativeFolder;

    /**
     * Creates a new resolver which transforms the include to be relative to the
     * given directory before delegating to the parent resolver.
     *
     * @param parentResolver
     *            the resolver to delegate to
     * @param relativeFolder
     *            the directory (relative to the parent resolver) requests
     *            should be relative to
     */
    public DelegateResolver(TemplateResolver parentResolver,
            String relativeFolder) {
        assert relativeFolder != null;
        assert !relativeFolder.endsWith(".html");
        this.parentResolver = parentResolver;
        this.relativeFolder = relativeFolder;
    }

    @Override
    public InputStream resolve(String filename) throws IOException {
        return parentResolver.resolve(relativeFolder + "/" + filename);
    }

}
