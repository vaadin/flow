/*
 * Copyright 2000-2014 Vaadin Ltd.
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

package com.vaadin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If this annotation is present on a ClientConnector class, the framework
 * ensures the referenced HTML files are imported.
 * <p>
 * Absolute URLs including protocol and host are used as is on the client-side.
 * Relative URLs are mapped to APP/PUBLISHED/[url] which are by default served
 * from the classpath relative to the class where the annotation is defined.
 * <p>
 * The file is only loaded if it has not already been loaded, determined as
 * follows:
 * <ul>
 * <li>For absolute URLs, the URL is considered loaded if the same URL has
 * previously been loaded using {@code @HTML} or if a link tag loaded from the
 * same URL was present in the DOM when the Vaadin client-side was initialized.
 * <li>For relative URLs, the URL is considered loaded if another file with the
 * same name has already been loaded using {@code @HTML}, even if that file was
 * loaded from a different folder.
 * </ul>
 * <p>
 * Example: <code>@HTML({"http://host.com/file1.html", "file2.html"})</code> on
 * the class com.example.MyConnector would load the file
 * http://host.com/file1.html as is and file2.html from /com/example/file2.html
 * on the server's classpath using the ClassLoader that was used to load
 * com.example.MyConnector.
 *
 * @author Vaadin Ltd
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HTML {
    /**
     * HTML files to import
     *
     * @return an array of HMTL file urls
     */
    public String[] value();
}
