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
package com.vaadin.hummingbird.router;

import com.vaadin.annotations.Title;

/**
 * The default page title generator. This is a singleton, an instance can be
 * acquired with {@link #getInstance()}.
 * <p>
 * Uses {@link View}'s {@link Title#value()} or
 * {@link View#getTitle(LocationChangeEvent)}.
 * <p>
 * Can be replaced with
 * {@link ModifiableRouterConfiguration#setPageTitleGenerator(PageTitleGenerator)}
 * .
 */
public class DefaultPageTitleGenerator implements PageTitleGenerator {

    private static final DefaultPageTitleGenerator INSTANCE = new DefaultPageTitleGenerator();

    private DefaultPageTitleGenerator() {
        // singleton
    }

    /**
     * Gets the singleton instance of this class.
     *
     * @return the singleton instance of this class
     */
    public static DefaultPageTitleGenerator getInstance() {
        return INSTANCE;
    }

    @Override
    public String getPageTitle(LocationChangeEvent event,
            ViewRenderer viewRenderer) {
        return event.getViewChain().get(0).getTitle(event);
    }

}
