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
package com.vaadin.flow.theme;

import javax.servlet.ServletContext;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import com.vaadin.flow.shared.VaadinUriResolver;

/**
 * Abstract theme definition class for defining theme variables when in use.
 *
 * @author Vaadin Ltd
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
     * Return a list of contents to inline to the bootstrap head. The contents
     * will be handled as no-wrap as is and will be appended to the initial page
     * head.
     *
     * @return list of string content to inline or empty list if nothing to
     *         inline
     */
    default List<String> getHeadInlineContents() {

        return Collections.emptyList();
    }

    /**
     * Return a list of contents to inline to the bootstrap head. The contents
     * will be handled as no-wrap as is and will be appended to the initial page
     * head.
     *
     * @param resolver
     *            vaadin uri resolver to for resolving a URI scheme like
     *            "frontend://"
     * @return list of string content to inline or empty list if nothing to
     *         inline
     */
    default List<String> getHeadInlineContents(VaadinUriResolver resolver) {
        return Collections.emptyList();
    }

    /**
     * Return a list of contents to inline to the bootstrap body. The contents
     * will be handled as no-wrap as is and will be appended to the initial page
     * body.
     * <p>
     * This will usually be the any {@code <custom-style>} declarations, see
     * <a href=
     * "https://www.polymer-project.org/2.0/docs/api/elements/Polymer.CustomStyle">CustomStyle</a>
     *
     * @return list of string content to inline or empty list if nothing to
     *         inline
     */
    default List<String> getBodyInlineContents() {
        return Collections.emptyList();
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
            return url.replace(baseUrl, themeUrl);
        }
        return url;
    }
}
