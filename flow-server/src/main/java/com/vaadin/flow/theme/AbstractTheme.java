/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.theme;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Abstract theme definition class for defining theme variables when in use.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface AbstractTheme extends Serializable {

    /**
     * The url for the base component implementation.
     * <p>
     * e.g. src/
     *
     * @return the base component path
     */
    String getBaseUrl();

    /**
     * The url for the components themed version implementation.
     * <p>
     * e.g. theme/lumo/
     *
     * @return the themed component path
     */
    String getThemeUrl();

    /**
     * Return a list of contents to inline to the bootstrap header. The contents
     * will be handled as no-wrap as is and will be inserted to the initial page
     * head tag.
     * <p>
     * This will usually be the any {@code <custom-style>} declarations, see
     * <a href=
     * "https://www.polymer-project.org/2.0/docs/api/elements/Polymer.CustomStyle">CustomStyle</a>
     * <p>
     * For importing theme files, use
     * {@link com.vaadin.flow.component.dependency.JsModule} on the
     * corresponding theme subclass.
     *
     * @return list of string content to inline or empty list if nothing to
     *         inline
     */
    default List<String> getHeaderInlineContents() {
        return Collections.emptyList();
    }

    /**
     * Gets the attributes that should be set on the {@code <html>} element when
     * the Theme variant is applied.
     *
     * @param variant
     *            the variant defined in the {@link Theme} annotation, not
     *            <code>null</code>
     * @return a Map with the attributes (keys and values) that should be set in
     *         the body, or an empty Map if nothing should be set for the given
     *         variant.
     */
    default Map<String, String> getHtmlAttributes(String variant) {
        return Collections.emptyMap();
    }

    /**
     * Translates the given {@code url} using the result of the
     * {@link #getThemeUrl()} theme method.
     * <p>
     * If translation is possible then translated URL is returned. Otherwise the
     * {@code url} is returned.
     *
     * @param url
     *            the URL to translate using the theme
     * @return translated URL if possible or the same given {@code url} if not.
     */
    default String translateUrl(String url) {
        if (url.contains(getBaseUrl())) {

            String baseUrl = getBaseUrl();
            String themeUrl = getThemeUrl();
            if (baseUrl.endsWith("/") && !themeUrl.endsWith("/")) {
                themeUrl = themeUrl + "/";
            } else if (!baseUrl.endsWith("/") && themeUrl.endsWith("/")) {
                themeUrl = themeUrl.substring(0, themeUrl.length() - 1);
            }
            int start = url.lastIndexOf(baseUrl);
            StringBuilder builder = new StringBuilder();
            builder.append(url.substring(0, start));
            builder.append(themeUrl);
            builder.append(url.substring(start + baseUrl.length()));
            return builder.toString();
        }
        return url;
    }
}
