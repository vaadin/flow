/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.template;

/**
 * Handles conversions/formatting of values from Java to JavaScript.
 *
 * @author Vaadin Ltd
 */
public class JavaScriptNumberFormatter {

    private JavaScriptNumberFormatter() {
        // Only util methods
    }

    /**
     * Converts the given value to a string in the same way as a browser would.
     *
     * @param value
     *            the value to convert to a string
     * @return a string representation matching the JS
     */
    public static String toString(double value) {
        double d = value;
        int i = (int) d;
        if (d == i) {
            // JS strips ".0" from integers
            return String.valueOf(i);
        } else if (Double.isInfinite(d)) {
            return "";
        }
        String doubleAsString = String.valueOf(d);

        if (doubleAsString.contains("E")) {

            doubleAsString = doubleAsString.replace("E-", "e-");
            doubleAsString = doubleAsString.replace("E", "e+");
            // 1.0e+10 should be 1e+10
            int ePosition = doubleAsString.indexOf('e');
            String base = doubleAsString.substring(0, ePosition);
            if (base.endsWith(".0")) {
                return base.substring(0, base.length() - 2)
                        + doubleAsString.substring(ePosition);
            }
            return doubleAsString;
        }
        return Double.toString(d);
    }

}
