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

        String template = getTemplate(productionMode);
        template = template.replace("{{path}}", path);
        template = template.replace("{{additionalInfo}}", additionalInfo);
        if (template.contains("{{routes}}")) {
            template = template.replace("{{routes}}", getRoutes(event));
        }

        getElement().appendChild(new Html(template).getElement());
        return HttpServletResponse.SC_NOT_FOUND;
    }

    private String getTemplate(boolean productionMode) {
        if (productionMode) {
            if (prodModeTemplate == null)
                prodModeTemplate = readTemplate("RouteNotFoundError_prod.html");
            return prodModeTemplate;
        } else {
            return readTemplate("RouteNotFoundError_dev.html");
        }
    }

    private String readTemplate(String templateName) {
        try {
            return IOUtils.toString(
                    RouteNotFoundError.class.getResourceAsStream(templateName),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass())
                    .error("Unable to read " + templateName, e);
            return templateName + " not found. This should not happen";
        }
    }

    private String getRoutes(BeforeEnterEvent event) {
        List<Element> elements = new ArrayList<>();

        List<RouteData> routes = event.getSource().getRegistry()
                .getRegisteredRoutes();
        routes.sort(
                (route1, route2) -> route1.getUrl().compareTo(route2.getUrl()));
        routes.forEach(route -> {
            String text = route.getUrl();
            if (text == null || text.isEmpty()) {
                text = "<root>";
            }
            if (route.getParameters().isEmpty()) {
                Element link = new Element(Tag.A).attr("href", route.getUrl())
                        .text(text);
                elements.add(new Element(Tag.LI).appendChild(link));
            } else {
                Element link = new Element(Tag.LI)
                        .text(text + " (requires parameter)");
                elements.add(link);
            }
        });

        return elements.stream().map(Element::outerHtml)
                .collect(Collectors.joining());
    }
}
