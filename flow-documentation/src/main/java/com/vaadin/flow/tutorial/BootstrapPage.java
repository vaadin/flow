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
package com.vaadin.flow.tutorial;

import java.util.HashMap;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.BootstrapListener;
import com.vaadin.flow.server.BootstrapPageResponse;
import com.vaadin.flow.server.InitialPageSettings;
import com.vaadin.flow.server.PageConfigurator;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("application-structure/tutorial-bootstrap.asciidoc")
public class BootstrapPage {

    public class CustomBootstrapListener implements BootstrapListener {

        @Override
        public void modifyBootstrapPage(BootstrapPageResponse response) {
            Document document = response.getDocument();

            Element head = document.head();

            // @formetter:off
            head.appendChild(createMeta(document, "og:title", "The Rock"));
            head.appendChild(createMeta(document, "og:type", "video.movie"));
            head.appendChild(createMeta(document, "og:url",
                    "http://www.imdb.com/title/tt0117500/"));
            head.appendChild(createMeta(document, "og:image",
                    "http://ia.media-imdb.com/images/rock.jpg"));
            // @formetter:on
        }

        private Element createMeta(Document document, String property,
                String content) {
            Element meta = document.createElement("meta");
            meta.attr("property", property);
            meta.attr("content", content);
            return meta;
        }
    }

    @Route("")
    @Viewport("width=device-width")
    public class MyApp extends Div {
        public MyApp() {
            setText("Hello world");
        }
    }

    @Route(value = "", layout = MyLayout.class)
    public class MyView extends Div {
        public MyView() {
            setText("Hello world");
        }
    }

    @Viewport("width=device-width")
    public class MyLayout extends Div implements RouterLayout {
    }

    @Route(value = "", layout = MainLayout.class)
    public class Root extends Div {
    }

    public class MainLayout extends Div
            implements RouterLayout, PageConfigurator {

        @Override
        public void configurePage(InitialPageSettings settings) {
            settings.addInlineFromFile(InitialPageSettings.Position.PREPEND,
                    "inline.js", Dependency.Type.JAVASCRIPT);

            settings.addMetaTag("og:title", "The Rock");
            settings.addMetaTag("og:type", "video.movie");
            settings.addMetaTag("og:url",
                    "http://www.imdb.com/title/tt0117500/");
            settings.addMetaTag("og:image",
                    "http://ia.media-imdb.com/images/rock.jpg");
        }
    }

    public class Layout1 extends Div implements RouterLayout, PageConfigurator {

        @Override
        public void configurePage(InitialPageSettings settings) {
            HashMap<String, String> attributes = new HashMap<>();
            attributes.put("rel", "shortcut icon");
            settings.addLink("icons/favicon.ico", attributes);
        }
    }

    public class Layout2 extends Div implements RouterLayout, PageConfigurator {

        @Override
        public void configurePage(InitialPageSettings settings) {
            settings.addInlineWithContents(
                    "<link rel=\"shortcut icon\" href=\"icons/favicon.ico\">",
                    Dependency.Type.HTML_IMPORT);
        }
    }

    public class Layout3 extends Div
            implements RouterLayout, BootstrapListener {

        @Override
        public void modifyBootstrapPage(BootstrapPageResponse response) {
            final Element head = response.getDocument().head();
            head.append(
                    "<link rel=\"shortcut icon\" href=\"icons/favicon.ico\">");
        }
    }

    public class Layout4 extends Div implements RouterLayout, PageConfigurator {

        @Override
        public void configurePage(InitialPageSettings settings) {
            settings.addInlineFromFile("favicon.html",
                    Dependency.Type.HTML_IMPORT);
        }
    }


    @Route("")
    @BodySize(height = "100vh", width = "100vw")
    public static class BodySizeAnnotatedRoute extends Div {
    }

    @Route("")
    public static class InitialPageConfiguratorBodyStyle extends Div
            implements PageConfigurator {
        @Override
        public void configurePage(InitialPageSettings settings) {
            settings.addInlineWithContents("body {width: 100vw; height:100vh;}",
                    Dependency.Type.STYLESHEET);
        }
    }

    @Route(value = "", layout = MyInline.class)
    public class MyRoot extends Div {
        public MyRoot() {
        }
    }

    @Inline("initialization.js")
    @Inline("initial_style.css")
    @Inline(value = "important_styles", wrapping = Inline.Wrapping.CSS)
    public class MyInline extends Div implements RouterLayout {
    }
}
