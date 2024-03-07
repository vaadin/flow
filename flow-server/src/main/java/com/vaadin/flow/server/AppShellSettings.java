/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
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
public class AppShellSettings {

    private static final String MSG_UNSUPPORTED_NO_UI = "It only works when "
            + "useDeprecatedV14Bootstrapping is enabled. "
            + "Use a UIInitListener instead if there are server-side views.";

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

        private Element element(VaadinService service) {
            if (content == null) {
                content = BootstrapUtils.getDependencyContents(service, file);
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

    private final Map<Position, List<Element>> elements = new EnumMap<>(
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
        ListIterator<Element> iter = getHeadElements(Position.APPEND)
                .listIterator();
        while (iter.hasNext()) {
            if ("title".equals(iter.next().normalName())) {
                iter.remove();
            }
        }
        iter.add(createElement("title", title));
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
    public void addInlineFromFile(TargetElement target, Position position,
            String file, Wrapping type) {
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
     * @return An optional instance used for configuring the loading indicator
     *         or an empty optional if UI is not available.
     *
     * @throws UnsupportedOperationException
     *             If UI is not avaialble, for example, when using client-side
     *             bootstrapping
     *
     * @deprecated It only works when useDeprecatedV14Bootstrapping is enabled.
     *             Use a {@link UIInitListener} instead if there are server-side
     *             views.
     */
    @Deprecated
    public Optional<LoadingIndicatorConfiguration> getLoadingIndicatorConfiguration() {
        if (getUi().isPresent()) {
            return getUi().map(UI::getLoadingIndicatorConfiguration);
        } else {
            throw new UnsupportedOperationException(MSG_UNSUPPORTED_NO_UI);
        }
    }

    /**
     * Returns the configuration object for reconnect dialog.
     *
     * @return An optional instance used for reconnect dialog configuration or
     *         an empty optional if UI is not available.
     *
     * @throws UnsupportedOperationException
     *             If UI is not avaialble, for example, when using the
     *             client-side bootstrapping
     *
     * @deprecated It only works when useDeprecatedV14Bootstrapping is enabled.
     *             Use a {@link UIInitListener} instead if there are server-side
     *             views.
     */
    @Deprecated
    public Optional<ReconnectDialogConfiguration> getReconnectDialogConfiguration() {
        if (getUi().isPresent()) {
            return getUi().map(UI::getReconnectDialogConfiguration);
        } else {
            throw new UnsupportedOperationException(MSG_UNSUPPORTED_NO_UI);
        }
    }

    /**
     * Returns the object used for configuring the push channel.
     *
     * @return An optional instance used for push channel configuration or an
     *         empty optional if UI is not available.
     *
     * @throws UnsupportedOperationException
     *             If UI is not avaialble, for example, when using the
     *             client-side bootstrapping
     *
     * @deprecated It only works when useDeprecatedV14Bootstrapping is enabled.
     *             Use a {@link UIInitListener} instead if there are server-side
     *             views.
     */
    @Deprecated
    public Optional<PushConfiguration> getPushConfiguration() {
        if (getUi().isPresent()) {
            return getUi().map(UI::getPushConfiguration);
        } else {
            throw new UnsupportedOperationException(MSG_UNSUPPORTED_NO_UI);
        }
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
    List<Element> getInlineElements(VaadinService service, TargetElement target,
            Position position) {
        return inlines.stream()
                .filter(inline -> inline.target == target
                        && inline.position == position)
                .map(inline -> inline.element(service))
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
