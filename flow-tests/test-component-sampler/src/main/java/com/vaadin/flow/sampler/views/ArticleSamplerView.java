/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.sampler.views;

import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.Aside;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.sampler.SamplerMainLayout;

/**
 * Demonstrates semantic HTML5 container components.
 */
@Route(value = "article", layout = SamplerMainLayout.class)
@PageTitle("Article & Semantic Elements Sampler")
public class ArticleSamplerView extends Div {

    public ArticleSamplerView() {
        setId("article-sampler");

        add(new H1("Semantic HTML5 Elements"));
        add(new Paragraph("HTML5 semantic elements that give meaning to web content."));

        add(createSection("Article",
            "The Article component represents a self-contained composition.",
            createArticleDemo()));

        add(createSection("Aside",
            "The Aside component represents content indirectly related to the main content.",
            createAsideDemo()));

        add(createSection("Header & Footer",
            "Header and Footer components for introductory and closing content.",
            createHeaderFooterDemo()));

        add(createSection("Nav",
            "The Nav component represents a navigation section.",
            createNavDemo()));

        add(createSection("Main",
            "The Main component represents the main content of the document.",
            createMainDemo()));

        add(createSection("Complete Page Layout",
            "A complete page layout using semantic elements.",
            createCompleteLayoutDemo()));
    }

    private Div createSection(String title, String description, Div content) {
        Div section = new Div();
        section.getStyle()
            .set("margin-bottom", "40px")
            .set("padding", "20px")
            .set("border", "1px solid #e0e0e0")
            .set("border-radius", "8px");

        H2 sectionTitle = new H2(title);
        sectionTitle.getStyle().set("margin-top", "0");

        Paragraph desc = new Paragraph(description);
        desc.getStyle().set("color", "#666");

        section.add(sectionTitle, desc, new Hr(), content);
        return section;
    }

    private Div createArticleDemo() {
        Div demo = new Div();
        demo.setId("article-demo");

        Article article = new Article();
        article.setId("sample-article");
        article.getStyle()
            .set("padding", "20px")
            .set("background-color", "#fafafa")
            .set("border-radius", "8px")
            .set("border", "1px solid #e0e0e0");

        H3 articleTitle = new H3("Introduction to Vaadin Flow");
        articleTitle.getStyle().set("margin-top", "0").set("color", "#1976d2");

        Paragraph author = new Paragraph("By Jane Developer | February 26, 2026");
        author.getStyle()
            .set("font-size", "0.9em")
            .set("color", "#666")
            .set("font-style", "italic");

        Paragraph content1 = new Paragraph(
            "Vaadin Flow is a framework for building modern web applications in Java. " +
            "It provides a seamless integration between server-side Java code and " +
            "client-side web technologies.");

        Paragraph content2 = new Paragraph(
            "With Flow, you can build responsive and interactive UIs entirely in Java, " +
            "while the framework handles all the client-server communication automatically.");

        article.add(articleTitle, author, content1, content2);

        demo.add(article);
        return demo;
    }

    private Div createAsideDemo() {
        Div demo = new Div();
        demo.setId("aside-demo");
        demo.getStyle()
            .set("display", "flex")
            .set("gap", "20px");

        Div mainContent = new Div();
        mainContent.getStyle()
            .set("flex", "2")
            .set("padding", "15px")
            .set("background-color", "#f5f5f5")
            .set("border-radius", "8px");

        mainContent.add(new H3("Main Content"));
        mainContent.add(new Paragraph(
            "This is the main content area. The aside panel on the right contains " +
            "supplementary information that's related but not essential to the main content."));

        Aside aside = new Aside();
        aside.setId("sample-aside");
        aside.getStyle()
            .set("flex", "1")
            .set("padding", "15px")
            .set("background-color", "#e3f2fd")
            .set("border-radius", "8px")
            .set("border-left", "4px solid #1976d2");

        H3 asideTitle = new H3("Related Links");
        asideTitle.getStyle().set("margin-top", "0");

        aside.add(asideTitle);
        aside.add(new Paragraph("Vaadin Documentation"));
        aside.add(new Paragraph("Flow API Reference"));
        aside.add(new Paragraph("Component Gallery"));

        demo.add(mainContent, aside);
        return demo;
    }

    private Div createHeaderFooterDemo() {
        Div demo = new Div();
        demo.setId("header-footer-demo");

        Div container = new Div();
        container.getStyle()
            .set("border", "1px solid #e0e0e0")
            .set("border-radius", "8px")
            .set("overflow", "hidden");

        Header header = new Header();
        header.setId("sample-header");
        header.getStyle()
            .set("padding", "20px")
            .set("background-color", "#1976d2")
            .set("color", "white");

        H3 headerTitle = new H3("Website Header");
        headerTitle.getStyle()
            .set("margin", "0")
            .set("color", "white");
        Paragraph headerSubtitle = new Paragraph("Welcome to our site");
        headerSubtitle.getStyle()
            .set("margin", "5px 0 0 0")
            .set("opacity", "0.9");
        header.add(headerTitle, headerSubtitle);

        Div content = new Div();
        content.getStyle()
            .set("padding", "20px")
            .set("min-height", "100px")
            .set("background-color", "#f5f5f5");
        content.add(new Paragraph("Main content goes here between header and footer."));

        Footer footer = new Footer();
        footer.setId("sample-footer");
        footer.getStyle()
            .set("padding", "15px 20px")
            .set("background-color", "#424242")
            .set("color", "white")
            .set("text-align", "center");
        footer.add(new Span("Copyright 2026 - All rights reserved"));

        container.add(header, content, footer);
        demo.add(container);
        return demo;
    }

    private Div createNavDemo() {
        Div demo = new Div();
        demo.setId("nav-demo");

        Nav nav = new Nav();
        nav.setId("sample-nav");
        nav.getStyle()
            .set("display", "flex")
            .set("gap", "5px")
            .set("padding", "10px")
            .set("background-color", "#37474f")
            .set("border-radius", "8px");

        String[] menuItems = {"Home", "Products", "Services", "About", "Contact"};

        for (String item : menuItems) {
            Span navItem = new Span(item);
            navItem.getStyle()
                .set("padding", "10px 20px")
                .set("color", "white")
                .set("cursor", "pointer")
                .set("border-radius", "4px")
                .set("transition", "background-color 0.2s");
            nav.add(navItem);
        }

        demo.add(nav);
        return demo;
    }

    private Div createMainDemo() {
        Div demo = new Div();
        demo.setId("main-demo");

        Main main = new Main();
        main.setId("sample-main");
        main.getStyle()
            .set("padding", "20px")
            .set("background-color", "#f5f5f5")
            .set("border-radius", "8px");

        H3 mainTitle = new H3("Main Content Area");
        mainTitle.getStyle().set("margin-top", "0");

        Paragraph mainContent = new Paragraph(
            "The Main element represents the dominant content of the body of a document. " +
            "It should be unique to the document, excluding content that is repeated " +
            "across documents such as sidebars, navigation links, copyright information, " +
            "site logos, and search forms.");

        main.add(mainTitle, mainContent);
        demo.add(main);
        return demo;
    }

    private Div createCompleteLayoutDemo() {
        Div demo = new Div();
        demo.setId("complete-layout");

        Div page = new Div();
        page.getStyle()
            .set("border", "2px solid #e0e0e0")
            .set("border-radius", "8px")
            .set("overflow", "hidden");

        // Header
        Header pageHeader = new Header();
        pageHeader.getStyle()
            .set("padding", "15px 20px")
            .set("background-color", "#1565c0")
            .set("color", "white")
            .set("display", "flex")
            .set("justify-content", "space-between")
            .set("align-items", "center");

        Span logo = new Span("MyWebsite");
        logo.getStyle().set("font-size", "1.3em").set("font-weight", "bold");

        Nav headerNav = new Nav();
        headerNav.getStyle().set("display", "flex").set("gap", "20px");
        for (String link : new String[]{"Home", "Blog", "Contact"}) {
            Span navLink = new Span(link);
            navLink.getStyle().set("color", "white").set("cursor", "pointer");
            headerNav.add(navLink);
        }

        pageHeader.add(logo, headerNav);

        // Body with Main and Aside
        Div pageBody = new Div();
        pageBody.getStyle()
            .set("display", "flex")
            .set("min-height", "200px");

        Main pageMain = new Main();
        pageMain.getStyle()
            .set("flex", "3")
            .set("padding", "20px")
            .set("background-color", "#fafafa");

        Article blogPost = new Article();
        H3 postTitle = new H3("Latest Blog Post");
        postTitle.getStyle().set("margin-top", "0");
        Paragraph postContent = new Paragraph(
            "This is an example of a complete page layout using semantic HTML5 elements. " +
            "Notice how each section has a specific purpose and meaning.");
        blogPost.add(postTitle, postContent);
        pageMain.add(blogPost);

        Aside pageSidebar = new Aside();
        pageSidebar.getStyle()
            .set("flex", "1")
            .set("padding", "20px")
            .set("background-color", "#e8eaf6");

        H3 sidebarTitle = new H3("Sidebar");
        sidebarTitle.getStyle().set("margin-top", "0");
        pageSidebar.add(sidebarTitle, new Paragraph("Recent Posts"), new Paragraph("Categories"));

        pageBody.add(pageMain, pageSidebar);

        // Footer
        Footer pageFooter = new Footer();
        pageFooter.getStyle()
            .set("padding", "15px 20px")
            .set("background-color", "#37474f")
            .set("color", "white")
            .set("text-align", "center");
        pageFooter.add(new Span("Copyright 2026 MyWebsite"));

        page.add(pageHeader, pageBody, pageFooter);
        demo.add(page);
        return demo;
    }
}
