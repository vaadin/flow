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

import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.AbstractTheme;
import com.vaadin.flow.server.Theme;
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

    public static class Lumo implements AbstractTheme {
        @Override
        public String getBaseUrl() {
            return "/src/";
        }

        @Override
        public String getThemeUrl() {
            return "/theme/lumo/";
        }

        @Override
        public List<String> getInlineContents() {
            return Arrays.asList(
                    "<custom-style><style include=\"lumo-typography\"></style></custom-style>");
        }
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
    }
}
