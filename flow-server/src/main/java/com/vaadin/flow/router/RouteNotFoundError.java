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
package com.vaadin.flow.router;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Tag;

/**
 * This is a basic default error view shown on routing exceptions.
 *
 * @since 1.0
 */
@Tag(Tag.DIV)
public class RouteNotFoundError extends Component
        implements HasErrorParameter<NotFoundException> {

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<NotFoundException> parameter) {
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
        template = template.replace("{{path}}", path);
        template = template.replace("{{additionalInfo}}", additionalInfo);
        if (template.contains("{{routes}}")) {
            template = template.replace("{{routes}}", getRoutes(event));
        }

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
        try {
            return IOUtils.toString(
                    RouteNotFoundError.class.getResourceAsStream(templateName),
                    StandardCharsets.UTF_8);
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

        return routes.stream()
                .sorted((route1, route2) -> route1.getUrl()
                        .compareTo(route2.getUrl()))
                .map(this::routeToHtml).map(Element::outerHtml)
                .collect(Collectors.joining());
    }

    private Element routeToHtml(RouteData route) {
        String text = route.getUrl();
        if (text == null || text.isEmpty()) {
            text = "<root>";
        }

        if (route.getParameters().isEmpty()) {
            return elementAsLink(route.getUrl(), text);
        } else {
            Class<? extends Component> target = route.getNavigationTarget();
            if (ParameterDeserializer.isAnnotatedParameter(target,
                    OptionalParameter.class)) {
                text = text + " (supports optional parameter)";
                return elementAsLink(route.getUrl(), text);
            } else {
                return new Element(Tag.LI).text(text + " (requires parameter)");
            }
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
