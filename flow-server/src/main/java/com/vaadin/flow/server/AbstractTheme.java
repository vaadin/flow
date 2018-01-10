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
package com.vaadin.flow.server;

import java.util.List;
import java.util.stream.Stream;

/**
 * Abstract theme definition class for defining theme variables when in use.
 * 
 * @author Vaadin Ltd
 */
public abstract class AbstractTheme {

    /**
     * The url for the base component implementation.
     * <p>
     * e.g. src/
     *
     * @return the base component path
     */
    public abstract String getBaseUrl();

    /**
     * The url for the components themed version implementation.
     * <p>
     * e.g. theme/lumo/
     * 
     * @return the themed component path
     */
    public abstract String getThemeUrl();

    /**
     * Return a list of contents to inline to the bootstrap head. The contents
     * will be handled as no-wrap as is and will be appended to the initial page
     * head.
     * 
     * @return
     */
    public abstract List<String> getInlineContents();

    /**
     * Get the translated theme path for the given url. If the url beginning
     * doesn't match the string from {@link #getBaseUrl()} the url will be
     * returned.
     * <p>
     * Translation will check if a file is found from the list of available
     * resources for the translated path and return the original url if not.
     * 
     * @param url
     *            url to translate
     * @param availableHtmlResources
     *            .html resources available for this servlet context
     * @return translated url path
     */
    public String getTranslatedUrl(String url,
            Stream<String> availableHtmlResources) {
        if (url.contains(getBaseUrl())) {

            String baseUrl = getBaseUrl();
            String themeUrl = getThemeUrl();
            if (baseUrl.endsWith("/") && !themeUrl.endsWith("/")) {
                themeUrl = themeUrl + "/";
            } else if (!baseUrl.endsWith("/") && themeUrl.endsWith("/")) {
                themeUrl = themeUrl.substring(0, themeUrl.length() - 1);
            }
            String translation = url.replace(baseUrl, themeUrl);
            String substring = translation
                    .substring(translation.indexOf(getThemeUrl()));

            if (!url.equals(translation)
                    && availableHtmlResources.filter(s -> s.endsWith(substring))
                            .findFirst().isPresent()) {
                return translation;
            }
        }
        return url;
    }
}
