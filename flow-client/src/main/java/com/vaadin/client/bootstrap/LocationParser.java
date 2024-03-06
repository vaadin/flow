/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
