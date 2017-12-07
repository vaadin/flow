/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.data.provider;

import java.io.Serializable;
import java.util.Set;

/**
 * Listener for when items are passivated on a {@link DataCommunicator}.
 * 
 * @author Vaadin Ltd.
 * @see DataCommunicator#addPassivationListener(PassivationListener)
 */
@FunctionalInterface
public interface PassivationListener extends Serializable {

    /**
     * Called when items are passivated.
     * 
     * @param itemKeys
     *            the keys of the passivated items
     */
    void itemsPassivated(Set<String> itemKeys);

}
