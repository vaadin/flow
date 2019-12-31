/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;

import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.ReconnectDialogConfiguration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Inline.Position;
import com.vaadin.flow.component.page.Inline.Wrapping;
import com.vaadin.flow.component.page.LoadingIndicatorConfiguration;
import com.vaadin.flow.component.page.TargetElement;
import com.vaadin.flow.component.page.Viewport;

/**
 * Initial page settings class for modifying the application shell.
 *
 * @since 3.0
 */
public class AppShellSettings implements Serializable {

    /**
     * A class representing an InlineElement.
     */
    private static final class InlineElement implements Serializable {
        private Position position;
        private Wrapping type;
        private TargetElement target;
        private String content;
        private String file;

        private InlineElement(TargetElement target, Position position,
                Wrapping type, String file, String content) {
            this.target = target;
            this.position = position;
            this.content = content;
            this.file = file;
            this.type = type;
        }

        private InlineElement(Inline ann) {
            this(ann.target(), ann.position(), ann.wrapping(), ann.value(),
                    null);
        }

        private Element element(VaadinRequest request) {
            if (content == null) {
                content = BootstrapUtils.getDependencyContents(request, file);
            }

            if (type == Wrapping.AUTOMATIC && file != null) {
                if (file.toLowerCase().endsWith(".css")) {
                    type = Wrapping.STYLESHEET;
                } else if (file.toLowerCase().endsWith(".js")) {
                    type = Wrapping.JAVASCRIPT;
                }
            }
            if (type == Wrapping.STYLESHEET) {
                return createElement("style", content, "type", "text/css");
            }
            if (type == Wrapping.JAVASCRIPT) {
                return createElement("script", content, "type",
                        "text/javascript");
            }
            return Jsoup.parse(content, "", Parser.xmlParser());
        }
    }

    private final List<InlineElement> inlines = new ArrayList<>();

    private final transient Map<Position, List<Element>> elements = new EnumMap<>(
            Position.class);

    /**
     * Get the current request.
     *
     * @return used request
     */
    public VaadinRequest getRequest() {
        return VaadinRequest.getCurrent();
    }

    /**
     * Get the target UI instance.
     *
     * @return ui instance
     */
    public Optional<UI> getUi() {
        return Optional.ofNullable(UI.getCurrent());
    }

    /**
     * Get the web browser used for the request used for these settings.
     *
     * @return browser information
     */
    public Optional<WebBrowser> getBrowser() {
        return getUi().map(ui -> ui.getSession().getBrowser());
    }

    /**
     * Set the viewport value. Since viewport can be set only once per page,
     * call to this method will have preference over the {@link Viewport}
     * annotation. If the method is called multiple times, the last one will be
     * used.
     *
     * @param viewport
     *            viewport value to set
     */
    public void setViewport(String viewport) {
        addMetaTag(Position.APPEND, "viewport", viewport);
    }

    /**
     * Set the body size. If the method is called multiple times, the last one
     * will be used.
     *
     * @param width
     *            body with
     * @param height
     *            body height
     */
    public void setBodySize(String width, String height) {
        addInline(TargetElement.HEAD, Position.APPEND, Wrapping.STYLESHEET,
                null, "body,#outlet{" + "width:" + width + ";" + "height:"
                        + height + ";}");
    }

    /**
     * Set the page title. If the method is called multiple times, the last one
     * will be used.
     *
     * @param title
     *            title
     */
    public void setPageTitle(String title) {
        getHeadElements(Position.APPEND).add(createElement("title", title));
    }

    /**
     * Inline contents from classpath file to append to head of initial page.
     *
     * @param file
     *            dependency file to read and write to head
     * @param type
     *            dependency type
     */
    public void addInlineFromFile(String file, Wrapping type) {
        addInlineFromFile(Position.APPEND, file, type);
    }

    /**
     * Inline contents from classpath file to head of initial page.
     *
     * @param position
     *            prepend or append
     * @param file
     *            dependency file to read and write to head
     * @param type
     *            dependency type
     */
    public void addInlineFromFile(Position position, String file,
            Wrapping type) {
        addInlineFromFile(TargetElement.HEAD, position, file, type);
    }

    /**
     * Inline contents from classpath file to head of initial page.
     *
     * @param target
     *            head of body
     * @param position
     *            prepend or append
     * @param file
     *            dependency file to read and write to head
     * @param type
     *            dependency type
     */
    public void addInlineFromFile(TargetElement target, Position position, String file,
            Wrapping type) {
        addInline(target, position, type, file, null);
    }

    /**
     * Add content to append to head of initial page.
     *
     * @param contents
     *            inline content to be added to the page
     * @param type
     *            type of content which can be JavaScript or Stylesheet (CSS)
     */
    public void addInlineWithContents(String contents, Wrapping type) {
        addInlineWithContents(Position.APPEND, contents, type);
    }

    /**
     * Add content to head of initial page.
     *
     * @param position
     *            prepend or append
     * @param contents
     *            inline content to be added to the page
     * @param type
     *            type of content which can be JavaScript or Stylesheet (CSS)
     */
    public void addInlineWithContents(Position position, String contents,
            Wrapping type) {
        addInlineWithContents(TargetElement.HEAD, position, contents, type);
    }

    /**
     * Add content to the specified target of initial page.
     *
     * @param target
     *            head of body
     * @param position
     *            prepend or append
     * @param contents
     *            inline content to be added to the page
     * @param type
     *            type of content which can be JavaScript or Stylesheet (CSS)
     */
    public void addInlineWithContents(TargetElement target, Position position,
            String contents, Wrapping type) {
        addInline(target, position, type, null, contents);
    }

    void addInline(Inline inline) {
        inlines.add(new InlineElement(inline));
    }

    private void addInline(TargetElement target, Position position,
            Wrapping type, String file, String content) {
        inlines.add(new InlineElement(target, position, type, file, content));
    }

    /**
     * Add a link to be appended to initial page head.
     *
     * @param href
     *            link href
     */
    public void addLink(String href) {
        addLink(Position.APPEND, href);
    }

    /**
     * Add a link to initial page head.
     *
     * @param position
     *            prepend or append
     * @param href
     *            link href
     */
    public void addLink(Position position, String href) {
        addLink(position, href, new HashMap<>());
    }

    /**
     * Append a link to initial page head.
     *
     * @param href
     *            location of the linked document
     * @param attributes
     *            map of attributes for link element
     */
    public void addLink(String href, Map<String, String> attributes) {
        addLink(Position.APPEND, href, attributes);
    }

    /**
     * Add a link to initial page head.
     *
     * @param position
     *            prepend or append
     * @param href
     *            location of the linked document
     * @param attributes
     *            map of attributes for link element
     */
    public void addLink(Position position, String href,
            Map<String, String> attributes) {
        Element link = createElement("link", null, "href", href);
        attributes.forEach(link::attr);
        getHeadElements(position).add(link);
    }

    /**
     * Append a link to initial page head.
     *
     * @param rel
     *            link relationship
     * @param href
     *            location of the linked document
     */
    public void addLink(String rel, String href) {
        addLink(Position.APPEND, rel, href);
    }

    /**
     * Add a link to initial page head.
     *
     * @param position
     *            prepend or append
     * @param rel
     *            link relationship
     * @param href
     *            location of the linked document
     */
    public void addLink(Position position, String rel, String href) {
        getHeadElements(position)
                .add(createElement("link", null, "href", href, "rel", rel));
    }

    /**
     * Append a fav icon link to initial page head.
     *
     * @param rel
     *            link relationship
     * @param href
     *            location of the fav icon
     * @param sizes
     *            size of the linked fav icon
     */
    public void addFavIcon(String rel, String href, String sizes) {
        addFavIcon(Position.APPEND, rel, href, sizes);
    }

    /**
     * Append a fav icon link to initial page head.
     *
     * @param position
     *            prepend or append
     * @param rel
     *            link relationship
     * @param href
     *            location of the fav icon
     * @param sizes
     *            size of the linked fav icon
     */
    public void addFavIcon(Position position, String rel, String href,
            String sizes) {
        getHeadElements(position).add(createElement("link", null, "href", href,
                "rel", rel, "sizes", sizes));
    }

    /**
     * Add a meta tag to be appended to initial page head.
     *
     * @param name
     *            meta tag name
     * @param content
     *            meta tag content
     */
    public void addMetaTag(String name, String content) {
        addMetaTag(Position.APPEND, name, content);
    }

    /**
     * Add a meta tag to initial page head.
     *
     * @param position
     *            prepend or append
     * @param name
     *            meta tag name
     * @param content
     *            meta tag content
     */
    public void addMetaTag(Position position, String name, String content) {
        Element meta = createElement("meta", null, "name", name, "content",
                content);
        getHeadElements(position).add(meta);
    }

    /**
     * Returns the configuration object for loading indicator.
     *
     * @return the instance used for configuring the loading indicator
     */
    public Optional<LoadingIndicatorConfiguration> getLoadingIndicatorConfiguration() {
        return getUi().map(UI::getLoadingIndicatorConfiguration);
    }

    /**
     * Returns the configuration object for reconnect dialog.
     *
     * @return The instance used for reconnect dialog configuration
     */
    public Optional<ReconnectDialogConfiguration> getReconnectDialogConfiguration() {
        return getUi().map(UI::getReconnectDialogConfiguration);
    }

    /**
     * Returns the object used for configuring the push channel.
     *
     * @return the instance used for push channel configuration
     */
    public Optional<PushConfiguration> getPushConfiguration() {
        return getUi().map(UI::getPushConfiguration);
    }

    /**
     * Get the list of elements excluding inline ones to add to the head in the
     * given position.
     *
     * @param position
     *            prepend or append
     * @return a list of dom elements to add
     */
    List<Element> getHeadElements(Position position) {
        return elements.computeIfAbsent(position, key -> new ArrayList<>());
    }

    /**
     * Get the list of inline elements to add to a specific target and position.
     *
     * @param target
     *            target element
     * @param position
     *            position in the target
     * @return the list of dom elements to add.
     */
    List<Element> getInlineElements(VaadinRequest request,
            TargetElement target, Position position) {
        return inlines.stream()
                .filter(inline -> inline.target == target
                        && inline.position == position)
                .map(inline -> inline.element(request))
                .collect(Collectors.toList());
    }

    private static Element createElement(String tag, String content,
            String... attrs) {
        Element elm = new Element(Tag.valueOf(tag), "");
        if (content != null && !content.isEmpty()) {
            elm.appendChild(new DataNode(content));
        }
        for (int i = 0; i < attrs.length - 1; i += 2) {
            elm.attr(attrs[i], attrs[i + 1]);
        }
        return elm;
    }
}
