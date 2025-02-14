/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.client.bootstrap;

/**
 * Utility methods for parsing the document URL.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class LocationParser {

    private LocationParser() {
        // Util methods only
    }

    /**
     * Gets the value of the given parameter using the given search (query)
     * string.
     *
     * @param search
     *            the search string
     * @param parameter
     *            the parameter to retrieve
     * @return the value of the parameter or null if the parameter was not
     *         included in the search string
     */
    public static String getParameter(String search, String parameter) {
        String[] keyValues = search.substring(1).split("&");
        for (String keyValue : keyValues) {
            String[] param = keyValue.split("=", 2);
            if (param[0].equals(parameter)) {
                if (param.length != 2) {
                    return "";
                } else {
                    return param[1];
                }
            }
        }
        return null;
    }
}
