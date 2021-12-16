/*
 * Copyright 2000-2022 Vaadin Ltd.
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

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.internal.DefaultErrorHandler;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * This is a basic default error view shown on routing exceptions.
 *
 * @since 1.0
 */
@Tag(Tag.DIV)
@AnonymousAllowed
@DefaultErrorHandler
public class RouteNotFoundError extends Component
        implements HasErrorParameter<NotFoundException> {

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<NotFoundException> parameter) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug(
                    parameter.hasCustomMessage() ? parameter.getCustomMessage()
                            : "Route is not found",
                    parameter.getCaughtException());
        }
        String path = event.getLocation().getPath();
        String additionalInfo = "";
        if (parameter.hasCustomMessage()) {
            additionalInfo = "Reason: " + parameter.getCustomMessage();
        }
        path = Jsoup.clean(path, Whitelist.none());
        additionalInfo = Jsoup.clean(additionalInfo, Whitelist.none());

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

        getElement().setChild(0, new Html(template).getElement());
        return HttpServletResponse.SC_NOT_FOUND;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(RouteNotFoundError.class);
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
            getLogger().error("Unable to read " + templateName, e);
            // Use a very simple error page if the real one could not be found
            return "Could not navigate to '{{path}}'";
        }
    }

    private String getRoutes(BeforeEnterEvent event) {
        List<RouteData> routes = event.getSource().getRegistry()
                .getRegisteredRoutes();

        return routes.stream()
                .sorted((route1, route2) -> route1.getTemplate()
                        .compareTo(route2.getTemplate()))
                .map(this::routeToHtml).map(Element::outerHtml)
                .collect(Collectors.joining());
    }

    private Element routeToHtml(RouteData route) {
        String text = route.getTemplate();
        if (text == null || text.isEmpty()) {
            text = "<root>";
        }

        if (!route.getTemplate().contains(":")) {
            return elementAsLink(route.getTemplate(), text);
        } else {
            Class<? extends Component> target = route.getNavigationTarget();
            if (ParameterDeserializer.isAnnotatedParameter(target,
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
