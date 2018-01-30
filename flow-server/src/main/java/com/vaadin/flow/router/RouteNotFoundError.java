/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.jsoup.nodes.Element;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.theme.NoTheme;

/**
 * This is a basic default error view shown on routing exceptions.
 */
@Tag(Tag.DIV)
@NoTheme
public class RouteNotFoundError extends Component
        implements HasErrorParameter<NotFoundException> {

    private static String prodModeTemplate;

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<NotFoundException> parameter) {
        String path = event.getLocation().getPath();
        String additionalInfo = "";
        if (parameter.hasCustomMessage()) {
            additionalInfo = "Reason: " + parameter.getCustomMessage();
        }

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
            // Note that this is no thread safe and it's possible that multiple
            // threads will read the template at the same time. With the current
            // code it does not really matter BUT if this code is changed to
            // something else, then threadsafety might become a problem.
            if (prodModeTemplate == null) {
                prodModeTemplate = readHtmlFile("RouteNotFoundError_prod.html");
            }

            return prodModeTemplate;
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
        routes.sort(
                (route1, route2) -> route1.getUrl().compareTo(route2.getUrl()));

        return routes.stream().map(this::routeToHtml).map(Element::outerHtml)
                .collect(Collectors.joining());
    }

    private Element routeToHtml(RouteData route) {
        String text = route.getUrl();
        if (text == null || text.isEmpty()) {
            text = "<root>";
        }

        if (route.getParameters().isEmpty()) {
            Element link = new Element(Tag.A).attr("href", route.getUrl())
                    .text(text);
            return new Element(Tag.LI).appendChild(link);
        } else {
            Element link = new Element(Tag.LI)
                    .text(text + " (requires parameter)");
            return link;
        }

    }
}
