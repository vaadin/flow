/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.internal.DefaultErrorHandler;
import com.vaadin.flow.server.HttpStatusCode;
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

        getElement().setChild(0, new Html(template).getElement());
        return HttpStatusCode.NOT_FOUND.getCode();
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
