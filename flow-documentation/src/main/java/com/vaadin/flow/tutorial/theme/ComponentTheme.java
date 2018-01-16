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
package com.vaadin.flow.tutorial.theme;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.shared.VaadinUriResolver;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("theme/tutorial-built-in-themes.asciidoc")
public class ComponentTheme {

    @Route(value = "")
    @Theme(Lumo.class)
    public class Application extends Div {
    }

    @Theme(Lumo.class)
    public class MainLayout extends Div implements RouterLayout {
    }

    @Route(value = "", layout = MainLayout.class)
    public class HomeView extends Div {
    }

    @Route(value = "blog", layout = MainLayout.class)
    public class BlogPost extends Div {
    }

    public class MyTheme implements AbstractTheme {
        @Override
        public String getBaseUrl() {
            return "/src/";
        }

        @Override
        public String getThemeUrl() {
            return "/theme/myTheme/";
        }

        @Override
        public List<String> getHeadInlineContents(VaadinUriResolver resolver) {
            String colorUrl = resolver.resolveVaadinUri("frontend://bower_components/vaadin-lumo-styles/color.html");
            List<String> contents = new ArrayList<>();
            contents.add("<link rel=\"import\" href=\"" + colorUrl + "\">");
            return contents;
        }
    }

}
