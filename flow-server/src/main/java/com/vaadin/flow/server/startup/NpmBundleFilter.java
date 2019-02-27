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
package com.vaadin.flow.server.startup;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.server.DependencyFilter;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;

/**
 * Filter for serving npm bundle instead of single dependencies for JavaScript
 * modules.
 *
 * @author Vaadin Ltd
 * @since
 */
public class NpmBundleFilter implements DependencyFilter {

    private String bundleUrl;
    private String bundleEs5Url;

    /**
     * Constructor for generating a filter that can return the correct type of
     * bundle import when using JavaScript modules.
     *
     * @param bundleUrl
     *         ES6 bundle url
     * @param bundleEs5Url
     *         ES5 bundle url
     */
    public NpmBundleFilter(String bundleUrl, String bundleEs5Url) {
        this.bundleUrl = bundleUrl;
        this.bundleEs5Url = bundleEs5Url;
    }

    @Override
    public List<Dependency> filter(List<Dependency> dependencies,
            FilterContext filterContext) {
        List<Dependency> newList = new ArrayList<>();

        if (filterContext.getBrowser().isEs6Supported()) {
            newList.add(new Dependency(Dependency.Type.JS_MODULE, bundleUrl,
                    LoadMode.EAGER));
        } else {
            newList.add(new Dependency(Dependency.Type.JAVASCRIPT, bundleEs5Url,
                    LoadMode.EAGER, false));
        }

        dependencies.stream().filter(dependency -> !Dependency.Type.JS_MODULE
                .equals(dependency.getType())).forEach(newList::add);

        return newList;
    }
}
