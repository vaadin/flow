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
package com.vaadin.flow.i18n;

import java.io.Serializable;

/**
 * Any {@code com.vaadin.ui.Component} implementing this interface will be
 * informed when the UI locale is changed and on attach.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface LocaleChangeObserver extends Serializable {

    /**
     * Notifies when the UI locale is changed.
     *
     * @param event
     *            locale change event with event details
     */
    void localeChange(LocaleChangeEvent event);
}
