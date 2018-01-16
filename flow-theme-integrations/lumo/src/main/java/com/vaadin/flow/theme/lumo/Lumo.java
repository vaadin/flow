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
package com.vaadin.flow.theme.lumo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.shared.VaadinUriResolver;
import com.vaadin.flow.theme.AbstractTheme;

/**
 * Lumo component theme class implementation.
 */
public class Lumo implements AbstractTheme {

    @Override
    public String getBaseUrl() {
        return "src/";
    }

    @Override
    public String getThemeUrl() {
        return "theme/lumo/";
    }

    @Override
    public List<String> getHeadInlineContents(VaadinUriResolver resolver) {
        List<String> contents = new ArrayList<>();
        contents.add(createImportLink(resolver,
                "frontend://bower_components/vaadin-lumo-styles/color.html"));
        contents.add(createImportLink(resolver,
                "frontend://bower_components/vaadin-lumo-styles/typography.html"));
        contents.add(createImportLink(resolver,
                "frontend://bower_components/vaadin-lumo-styles/sizing.html"));
        contents.add(createImportLink(resolver,
                "frontend://bower_components/vaadin-lumo-styles/spacing.html"));
        contents.add(createImportLink(resolver,
                "frontend://bower_components/vaadin-lumo-styles/style.html"));
        contents.add(createImportLink(resolver,
                "frontend://bower_components/vaadin-lumo-styles/icons.html"));
        return contents;
    }

    @Override
    public List<String> getBodyInlineContents() {
        return Arrays.asList("<custom-style>\n"
                + "    <style include=\"lumo-color lumo-typography\"></style>\n"
                + "</custom-style>");
    }

    private static String createImportLink(VaadinUriResolver resolver,
            String href) {
        String resolvedLink = resolver.resolveVaadinUri(href);
        return "<link rel=\"import\" href=\"" + resolvedLink + "\">";

    }
}
