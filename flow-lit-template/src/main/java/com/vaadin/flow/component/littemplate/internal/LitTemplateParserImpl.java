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
package com.vaadin.flow.component.littemplate.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.littemplate.BundleLitParser;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.littemplate.LitTemplateParser;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.Pair;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.DependencyFilter;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;

/**
 * Lit template parser implementation.
 * <p>
 * The implementation scans all JsModule annotations for the given template
 * class and tries to find the one that contains template definition using the
 * tag name.
 * <p>
 * The class is Singleton. Use {@link LitTemplateParserImpl#getInstance()} to
 * get its instance.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 *
 * @author Vaadin Ltd
 *
 * @see BundleLitParser
 */
public class LitTemplateParserImpl implements LitTemplateParser {

    private static final LitTemplateParser INSTANCE = new LitTemplateParserImpl();

    /**
     * The default constructor. Protected in order to prevent direct
     * instantiation, but not private in order to allow mocking/overrides for
     * testing purposes.
     */
    protected LitTemplateParserImpl() {
    }

    public static LitTemplateParser getInstance() {
        return INSTANCE;
    }

    @Override
    public TemplateData getTemplateContent(Class<? extends LitTemplate> clazz,
            String tag, VaadinService service) {

        List<Dependency> dependencies = AnnotationReader
                .getAnnotationsFor(clazz, JsModule.class).stream()
                .map(jsModule -> new Dependency(Dependency.Type.JS_MODULE,
                        jsModule.value(), LoadMode.EAGER)) // load mode doesn't
                                                           // matter here
                .collect(Collectors.toList());

        for (DependencyFilter filter : service.getDependencyFilters()) {
            dependencies = filter.filter(new ArrayList<>(dependencies),
                    service);
        }

        Pair<Dependency, String> chosenDep = null;

        for (Dependency dependency : dependencies) {
            if (dependency.getType() != Dependency.Type.JS_MODULE) {
                continue;
            }

            String url = dependency.getUrl();
            String source = getSourcesFromTemplate(service, tag, url);
            if (source == null) {
                continue;
            }
            if (chosenDep == null) {
                chosenDep = new Pair<>(dependency, source);
            }
            if (dependencyHasTagName(dependency, tag)) {
                chosenDep = new Pair<>(dependency, source);
                break;
            }
        }

        Element templateElement = null;
        if (chosenDep != null) {
            templateElement = BundleLitParser.parseLitTemplateElement(
                    chosenDep.getFirst().getUrl(), chosenDep.getSecond());
        }
        if (templateElement != null) {
            // Template needs to be wrapped in an element with id, to look
            // like a P2 template
            Element parent = new Element(tag);
            parent.attr("id", tag);
            templateElement.appendTo(parent);

            return new TemplateData(chosenDep.getFirst().getUrl(),
                    templateElement);
        }

        getLogger().info("Couldn't find the "
                + "definition of the element with tag '{}' "
                + "in any lit template file declared using '@{}' annotations. "
                + "In a Spring Boot project, please ensure that the template's "
                + "groupId is added to the vaadin.allowed-packages "
                + "property. Otherwise, please Check the availability of the "
                + "template files in your WAR file or provide alternative "
                + "implementation of the method "
                + "LitTemplateParser.getTemplateContent() which should return "
                + "an element representing the content of the template file",
                tag, JsModule.class.getSimpleName());

        return null;
    }

    /**
     * Dependency should match the tag name ignoring the extension of the file.
     *
     * @param dependency
     *            dependency to check
     * @param tag
     *            tag name for element
     * @return true if dependency file matches the tag name.
     */
    private boolean dependencyHasTagName(Dependency dependency, String tag) {
        String url = FilenameUtils.removeExtension(dependency.getUrl())
                .toLowerCase(Locale.ENGLISH);
        return url.endsWith("/" + tag);
    }

    /**
     * Finds the JavaScript sources for given tag.
     *
     * @param service
     *            the Vaadin service
     * @param tag
     *            the value of the {@link com.vaadin.flow.component.Tag}
     *            annotation, e.g. `my-component`
     * @param url
     *            the URL resolved according to the
     *            {@link com.vaadin.flow.component.dependency.JsModule} spec,
     *            for example {@code ./view/my-view.js} or
     *            {@code @vaadin/vaadin-button.js}.
     * @return the .js source which declares given custom element, or null if no
     *         such source can be found.
     */
    protected String getSourcesFromTemplate(VaadinService service, String tag,
            String url) {
        InputStream content = getResourceStream(service, url);
        if (content == null) {
            // Attempt to get the sources from dev server, if available
            content = FrontendUtils.getFrontendFileFromDevModeHandler(service,
                    url);
        }
        if (content == null) {
            // In production builds, template sources are stored in
            // META-INF/VAADIN/config/templates
            String pathWithoutPrefix = url.replaceFirst("^\\./", "");
            String vaadinDirectory = Constants.VAADIN_SERVLET_RESOURCES
                    + Constants.TEMPLATE_DIRECTORY;
            String resourceUrl = vaadinDirectory + pathWithoutPrefix;
            content = getResourceStream(service, resourceUrl);
        }
        if (content == null) {
            // In dev bundle mode, template sources are stored in
            // target/dev-bundle/config/templates
            String pathWithoutPrefix = url.replaceFirst("^\\./", "");
            Path subFolder = Path.of(Constants.DEV_BUNDLE_LOCATION, "config",
                    "templates", pathWithoutPrefix);
            File templateFile = new File(new File(
                    service.getDeploymentConfiguration().getProjectFolder(),
                    service.getDeploymentConfiguration().getBuildFolder()),
                    subFolder.toString());
            try {
                content = new FileInputStream(templateFile);
            } catch (FileNotFoundException e) {
                // If it ain't there, it ain't there
            }
        }
        if (content != null) {
            getLogger().debug(
                    "Found sources from the tag '{}' in the template '{}'", tag,
                    url);
            return FrontendUtils.streamToString(content);
        }
        return null;
    }

    private InputStream getResourceStream(VaadinService service, String url) {
        ResourceProvider resourceProvider = service.getContext()
                .getAttribute(Lookup.class).lookup(ResourceProvider.class);
        URL resourceUrl = resourceProvider.getApplicationResource(url);
        if (resourceUrl != null) {
            try {
                return resourceUrl.openStream();
            } catch (IOException e) {
                getLogger().warn("Exception accessing resource " + resourceUrl,
                        e);
            }
        }
        return null;
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(LitTemplateParserImpl.class.getName());
    }
}
