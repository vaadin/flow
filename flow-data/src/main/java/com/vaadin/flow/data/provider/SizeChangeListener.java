/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.data.provider;

/**
 * Listener interface for getting updates on data set size changes.
 *
 * Size changes are mostly due to filtering of the data, but can also be
 * sent for changes in the dataset.
 *
 * @since
 */
@FunctionalInterface
public interface SizeChangeListener {

    /**
     * Invoked for changes in the data size.
     *
     * @param event Component event containing new data size
     */
    void sizeChanged(SizeChangeEvent event);

}
