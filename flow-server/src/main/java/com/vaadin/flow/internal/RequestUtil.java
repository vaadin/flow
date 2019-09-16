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

import javax.servlet.http.HttpServletRequest;

public class RequestUtil {

    private RequestUtil() {}

    /**
     * Returns whether it is ok to serve a gzipped version of the given
     * resource.
     * <p>
     * If this method returns true, the browser is ok with receiving a gzipped
     * version of the resource. In other cases, an uncompressed file must be
     * sent.
     *
     * @param request
     *            the request for the resource
     * @return true if the servlet should attempt to serve a gzipped version of
     *         the resource, false otherwise
     */
    public static boolean acceptsGzippedResource(HttpServletRequest request) {
        return acceptsEncoding(request, "gzip");
    }

    /**
     * Returns whether it is ok to serve a Brotli version of the given resource.
     * <p>
     * If this method returns true, the browser is ok with receiving a Brotli
     * version of the resource. In other cases, an uncompressed or gzipped file
     * must be sent.
     *
     * @param request
     *            the request for the resource
     * @return true if the servlet should attempt to serve a Brotli version of
     *         the resource, false otherwise
     */
    public static boolean acceptsBrotliResource(HttpServletRequest request) {
        return acceptsEncoding(request, "br");
    }

    public static boolean acceptsEncoding(HttpServletRequest request,
            String encodingName) {
        String accept = request.getHeader("Accept-Encoding");
        if (accept == null) {
            return false;
        }

        accept = accept.replace(" ", "");
        // Browser denies gzip compression if it reports
        // gzip;q=0
        //
        // Browser accepts gzip compression if it reports
        // "gzip"
        // "gzip;q=[notzero]"
        // "*"
        // "*;q=[not zero]"
        if (accept.contains(encodingName)) {
            return !isQualityValueZero(accept, encodingName);
        }
        return accept.contains("*") && !isQualityValueZero(accept, "*");
    }

    /**
     * Check the quality value of the encoding. If the value is zero the
     * encoding is disabled and not accepted.
     *
     * @param acceptEncoding
     *         Accept-Encoding header from request
     * @param encoding
     *         encoding to check
     * @return true if quality value is Zero
     */
    private static boolean isQualityValueZero(String acceptEncoding,
            String encoding) {
        String qPrefix = encoding + ";q=";
        int qValueIndex = acceptEncoding.indexOf(qPrefix);
        if (qValueIndex == -1) {
            return false;
        }

        // gzip;q=0.123 or gzip;q=0.123,compress...
        String qValue = acceptEncoding
                .substring(qValueIndex + qPrefix.length());
        int endOfQValue = qValue.indexOf(',');
        if (endOfQValue != -1) {
            qValue = qValue.substring(0, endOfQValue);
        }
        return Double.valueOf(0.000).equals(Double.valueOf(qValue));
    }
}
