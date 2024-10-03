/*
 * Copyright 2000-2024 Vaadin Ltd.
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

package com.vaadin.flow.server.menu;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Stream;

/**
 * {@link MenuOptions} holds a list of menu options.
 */
public class MenuOptions implements Serializable {

    private final List<MenuOption> menuOptions;

    private MenuOptions(List<MenuOption> menuOptions) {
        this.menuOptions = menuOptions;
    }

    /**
     * Construct a new instance containing all available menu options.
     */
    public static MenuOptions getInstance() {
        return new MenuOptions(
                MenuRegistry.collectMenuItemsList().stream().map(viewInfo -> {
                    if (viewInfo.menu() == null) {
                        return new MenuOption(viewInfo.route(),
                                viewInfo.title(), null, false, null, null);
                    }
                    return new MenuOption(viewInfo.route(),
                            (viewInfo.menu().title() != null
                                    && !viewInfo.menu().title().isBlank()
                                            ? viewInfo.menu().title()
                                            : viewInfo.title()),
                            viewInfo.menu().order(), viewInfo.menu().exclude(),
                            viewInfo.menu().icon(),
                            viewInfo.menu().menuClass());
                }).toList());
    }

    /**
     * Get a stream of the menu options.
     *
     * @return the menu options stream
     */
    public Stream<MenuOption> stream() {
        return menuOptions.stream();
    }

    /**
     * Get the list of the menu options. Returned list is mutable and any
     * changes to it will also change {@link MenuOptions} state.
     *
     * @return the menu options list
     */
    public List<MenuOption> get() {
        return menuOptions;
    }
}
