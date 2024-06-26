/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
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
