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
package com.vaadin.flow.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.ReconnectDialogConfiguration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.LoadingIndicatorConfiguration;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;
import elemental.json.Json;
import elemental.json.JsonObject;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

/**
 * Initial page settings class for modifying the bootstrap page.
 *
 * @since 1.0
 */
public class InitialPageSettings implements Serializable {

    /**
     * Append position enum.
     */
    public enum Position {
        PREPEND, APPEND
    }

    /**
     * Content wrapping mode enum.
     */
    public enum WrapMode {
        NONE, JAVASCRIPT, STYLESHEET
    }

    private final VaadinRequest request;
    private final UI ui;
    private final AfterNavigationEvent afterNavigationEvent;
    private final WebBrowser browser;

    /* Initial page values */
    private String viewport;

    private final Map<Position, List<JsonObject>> inline = new EnumMap<>(
            Position.class);
    private final Map<Position, List<Element>> elements = new EnumMap<>(
            Position.class);

    /**
     * Create new initial page settings object.
     *
     * @param request
     *         initial request
     * @param ui
     *         target ui
     * @param afterNavigationEvent
     *         after navigation event
     * @param browser
     *         browser information
     */
    public InitialPageSettings(VaadinRequest request, UI ui,
                               AfterNavigationEvent afterNavigationEvent, WebBrowser browser) {
        this.request = request;
        this.ui = ui;
        this.afterNavigationEvent = afterNavigationEvent;
        this.browser = browser;
    }

    /**
     * Get the initial request for the settings.
     *
     * @return used request
     */
    public VaadinRequest getRequest() {
        return request;
    }

    /**
     * Get the target UI instance.
     *
     * @return ui instance
     */
    public UI getUi() {
        return ui;
    }

    /**
     * Get the after navigation event.
     *
     * @return the after navigation event
     */
    public AfterNavigationEvent getAfterNavigationEvent() {
        return afterNavigationEvent;
    }

    /**
     * Get the web browser used for the request used for these settings.
     *
     * @return browser information
     */
    public WebBrowser getBrowser() {
        return browser;
    }

    /**
     * Set the viewport value.
     *
     * @param viewport
     *         viewport value to set
     */
    public void setViewport(String viewport) {
        this.viewport = viewport;
    }

    /**
     * Get the currently set viewport setting for this settings object.
     * <p>
     * Note! this will not reflect any setting made using
     * {@link com.vaadin.flow.component.page.Viewport}
     *
     * @return current viewport setting or null if nothing setﬁ
     */
    protected String getViewport() {
        return viewport;
    }

    /**
     * Inline contents from classpath file to append to head of initial page.
     *
     * @param file
     *         dependency file to read and write to head
     * @param type
     *         dependency type
     */
    public void addInlineFromFile(String file, WrapMode type) {
        addInlineFromFile(Position.APPEND, file, type);
    }

    /**
     * Inline contents from classpath file to head of initial page.
     *
     * @param position
     *         prepend or append
     * @param file
     *         dependency file to read and write to head
     * @param type
     *         dependency type
     */
    public void addInlineFromFile(Position position, String file,
                                  WrapMode type) {
        JsonObject prepend = createInlineObject(type);
        prepend.put(Dependency.KEY_CONTENTS,
                BootstrapUtils.getDependencyContents(request, file));
        getInline(position).add(prepend);
    }

    /**
     * Add content to append to head of initial page.
     *
     * @param contents
     *         dependency content
     * @param type
     *         dependency type
     */
    public void addInlineWithContents(String contents, WrapMode type) {
        addInlineWithContents(Position.APPEND, contents, type);
    }

    /**
     * Add content to head of initial page.
     *
     * @param position
     *         prepend or append
     * @param contents
     *         dependency content
     * @param type
     *         dependency type
     */
    public void addInlineWithContents(Position position, String contents,
                                      WrapMode type) {
        JsonObject prepend = createInlineObject(type);
        prepend.put(Dependency.KEY_CONTENTS, contents);
        getInline(position).add(prepend);
    }

    /**
     * Get the list of inline objects to append to head.
     *
     * @param position
     *         prepend or append
     * @return current list of inline objects
     */
    protected List<JsonObject> getInline(Position position) {
        return inline.computeIfAbsent(position, key -> new ArrayList<>());
    }

    /**
     * Get the list of links to append to head.
     *
     * @param position
     *         prepend or append
     * @return current list of links
     */
    protected List<Element> getElement(Position position) {
        return elements.computeIfAbsent(position, key -> new ArrayList<>());
    }

    /**
     * Add a link to be appended to initial page head.
     *
     * @param href
     *         link href
     */
    public void addLink(String href) {
        addLink(Position.APPEND, href);
    }

    /**
     * Add a link to initial page head.
     *
     * @param position
     *         prepend or append
     * @param href
     *         link href
     */
    public void addLink(Position position, String href) {
        addLink(position, href, new HashMap<>());
    }

    /**
     * Append a link to initial page head.
     *
     * @param href
     *         location of the linked document
     * @param attributes
     *         map of attributes for link element
     */
    public void addLink(String href, Map<String, String> attributes) {
        addLink(Position.APPEND, href, attributes);
    }

    /**
     * Add a link to initial page head.
     *
     * @param position
     *         prepend or append
     * @param href
     *         location of the linked document
     * @param attributes
     *         map of attributes for link element
     */
    public void addLink(Position position, String href,
                        Map<String, String> attributes) {
        Element link = new Element(Tag.valueOf("link"), "").attr("href", href);
        attributes.forEach((key, value) -> link.attr(key, value));
        getElement(position).add(link);
    }

    /**
     * Append a link to initial page head.
     *
     * @param rel
     *         link relationship
     * @param href
     *         location of the linked document
     */
    public void addLink(String rel, String href) {
        addLink(Position.APPEND, rel, href);
    }

    /**
     * Add a link to initial page head.
     *
     * @param position
     *         prepend or append
     * @param rel
     *         link relationship
     * @param href
     *         location of the linked document
     */
    public void addLink(Position position, String rel, String href) {
        Element link = new Element(Tag.valueOf("link"), "").attr("href", href);
        link.attr("rel", rel);
        getElement(position).add(link);
    }

    /**
     * Append a fav icon link to initial page head.
     *
     * @param rel
     *         link relationship
     * @param href
     *         location of the fav icon
     * @param sizes
     *         size of the linked fav icon
     */
    public void addFavIcon(String rel, String href, String sizes) {
        addFavIcon(Position.APPEND, rel, href, sizes);
    }

    /**
     * Append a fav icon link to initial page head.
     *
     * @param position
     *         prepend or append
     * @param rel
     *         link relationship
     * @param href
     *         location of the fav icon
     * @param sizes
     *         size of the linked fav icon
     */
    public void addFavIcon(Position position, String rel, String href,
                           String sizes) {
        Element link = new Element(Tag.valueOf("link"), "").attr("href", href);
        link.attr("rel", rel);
        link.attr("sizes", sizes);
        getElement(position).add(link);
    }

    /**
     * Add a meta tag to be appended to initial page head.
     *
     * @param name
     *         meta tag name
     * @param content
     *         meta tag content
     */
    public void addMetaTag(String name, String content) {
        addMetaTag(Position.APPEND, name, content);
    }

    /**
     * Add a meta tag to initial page head.
     *
     * @param position
     *         prepend or append
     * @param name
     *         meta tag name
     * @param content
     *         meta tag content
     */
    public void addMetaTag(Position position, String name, String content) {
        Element meta = new Element(Tag.valueOf("meta"), "").attr("name", name)
                .attr("content", content);
        getElement(position).add(meta);
    }

    /**
     * Returns the configuration object for loading indicator.
     *
     * @return the instance used for configuring the loading indicator
     */
    public LoadingIndicatorConfiguration getLoadingIndicatorConfiguration() {
        return getUi().getLoadingIndicatorConfiguration();
    }

    /**
     * Returns the configuration object for reconnect dialog.
     *
     * @return The instance used for reconnect dialog configuration
     */
    public ReconnectDialogConfiguration getReconnectDialogConfiguration() {
        return getUi().getReconnectDialogConfiguration();
    }

    /**
     * Returns the object used for configuring the push channel.
     *
     * @return the instance used for push channel configuration
     */
    public PushConfiguration getPushConfiguration() {
        return getUi().getPushConfiguration();
    }

    private JsonObject createInlineObject(WrapMode type) {
        JsonObject prepend = Json.createObject();
        prepend.put(Dependency.KEY_TYPE, type.toString());
        prepend.put("LoadMode", LoadMode.INLINE.toString());
        return prepend;
    }
}
