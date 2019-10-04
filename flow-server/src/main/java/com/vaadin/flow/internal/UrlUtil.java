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

package com.vaadin.flow.internal;

import com.vaadin.flow.shared.ApplicationConstants;

/**
 * Internal utility class for URL handling.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public class UrlUtil {

    private UrlUtil() {
    }

    /**
     * checks if the given url is an external URL (e.g. staring with http:// or
     * https://) or not.
     * 
     * @param url
     *            is the url to be checked.
     * @return true if the url is external otherwise false.
     */
    public static boolean isExternal(String url) {
        if (url.startsWith("//")) {
            return true;
        }
        return url.contains("://") && !(url
                .startsWith(ApplicationConstants.CONTEXT_PROTOCOL_PREFIX)
                || url.startsWith(ApplicationConstants.FRONTEND_PROTOCOL_PREFIX)
                || url.startsWith(ApplicationConstants.BASE_PROTOCOL_PREFIX));
    }
}
