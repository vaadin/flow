/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.component.internal;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.WebBrowser;
import com.vaadin.flow.server.startup.FakeBrowser;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.util.SharedUtil;

/**
 * Html import dependencies parser.
 * <p>
 * It takes the an HTML import url as a root and parse the content recursively
 * collecting html import dependencies.
 *
 * @deprecated Due for removal because of additional cache layer
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
@Deprecated
public class HtmlDependencyParser implements Serializable {

    private final String root;

    /**
     * Creates a new instance using the given {@code uri} as a root.
     *
     * @param uri
     *            HTML import uri
     */
    public HtmlDependencyParser(String uri) {
        root = uri;
    }

    Collection<String> parseDependencies(VaadinService service) {
        Set<String> dependencies = new HashSet<>();
        String rooUri = SharedUtil.prefixIfRelative(root,
                ApplicationConstants.FRONTEND_PROTOCOL_PREFIX);

        SerializableConsumer<String> dependencyWalker = new SerializableConsumer<String>() {
            private final WebBrowser browser = FakeBrowser.getEs6();

            @Override
            public void accept(String uri) {
                if (dependencies.contains(uri)) {
                    return;
                }
                dependencies.add(uri);
                HtmlImportParser.parseImports(uri,
                        path -> service.getResourceAsStream(path, browser,
                                null),
                        path -> service.resolveResource(path, browser), this);
            }
        };

        dependencyWalker.accept(rooUri);

        return dependencies;
    }
}
