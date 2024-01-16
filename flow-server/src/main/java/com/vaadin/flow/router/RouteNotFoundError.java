/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.router;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.internal.DefaultErrorHandler;

/**
 * This is a basic default error view shown on routing exceptions.
 *
 * @since 1.0
 */
@Tag(Tag.DIV)
@DefaultErrorHandler
public class RouteNotFoundError extends Component
        implements HasErrorParameter<NotFoundException> {

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<NotFoundException> parameter) {
        LoggerFactory.getLogger(RouteNotFoundError.class)
                .info(parameter.hasCustomMessage()
                        ? parameter.getCustomMessage()
                        : "Route is not found", parameter.getCaughtException());
        String path = event.getLocation().getPath();
        String additionalInfo = "";
        if (parameter.hasCustomMessage()) {
            additionalInfo = "Reason: " + parameter.getCustomMessage();
        }
        path = Jsoup.clean(path, Safelist.none());
        additionalInfo = Jsoup.clean(additionalInfo, Safelist.none());

        boolean productionMode = event.getUI().getSession().getConfiguration()
                .isProductionMode();

        String template = getErrorHtml(productionMode);
        // {{routes}} should be replaced first so that it's not possible to
        // insert {{routes}} snippet via other template values which may result
        // in the listing of all available routes when this shouldn't not happen
        if (template.contains("{{routes}}")) {
            template = template.replace("{{routes}}", getRoutes(event));
        }
        template = template.replace("{{additionalInfo}}", additionalInfo);
        template = template.replace("{{path}}", path);

        getElement().appendChild(new Html(template).getElement());
        return HttpServletResponse.SC_NOT_FOUND;
    }

    private static String getErrorHtml(boolean productionMode) {
        if (productionMode) {
            return LazyInit.PRODUCTION_MODE_TEMPLATE;
        } else {
            return readHtmlFile("RouteNotFoundError_dev.html");
        }
    }

    private static String readHtmlFile(String templateName) {
        try (InputStream stream = RouteNotFoundError.class
                .getResourceAsStream(templateName)) {
            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LoggerFactory.getLogger(RouteNotFoundError.class)
                    .error("Unable to read " + templateName, e);
            // Use a very simple error page if the real one could not be found
            return "Could not navigate to '{{path}}'";
        }
    }

    private String getRoutes(BeforeEnterEvent event) {
        List<RouteData> routes = event.getSource().getRegistry()
                .getRegisteredRoutes();
        Map<String, Class<? extends Component>> routeTemplates = new TreeMap<>();

        for (RouteData route : routes) {
            routeTemplates.put(route.getTemplate(),
                    route.getNavigationTarget());
            route.getRouteAliases().forEach(alias -> routeTemplates
                    .put(alias.getTemplate(), alias.getNavigationTarget()));
        }

        List<Element> routeElements = new ArrayList<>();
        routeTemplates.forEach(
                (k, v) -> routeElements.add(routeTemplateToHtml(k, v)));

        return routeElements.stream().map(Element::outerHtml)
                .collect(Collectors.joining());
    }

    private Element routeTemplateToHtml(String routeTemplate,
            Class<? extends Component> navigationTarget) {
        String text = routeTemplate;
        if (text == null || text.isEmpty()) {
            text = "<root>";
        }

        if (!routeTemplate.contains(":")) {
            return elementAsLink(routeTemplate, text);
        } else {
            if (ParameterDeserializer.isAnnotatedParameter(navigationTarget,
                    OptionalParameter.class)) {
                text += " (supports optional parameter)";
            } else {
                text += " (requires parameter)";
            }

            return new Element(Tag.LI).text(text);
        }
    }

    private Element elementAsLink(String url, String text) {
        Element link = new Element(Tag.A).attr("href", url).text(text);
        return new Element(Tag.LI).appendChild(link);
    }

    private static class LazyInit {

        private static final String PRODUCTION_MODE_TEMPLATE = readHtmlFile(
                "RouteNotFoundError_prod.html");
    }
}
