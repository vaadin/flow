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
package com.vaadin.flow.internal;

import java.io.Serializable;

/**
 * Generic class representing an immutable pair of values.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class Pair<U extends Serializable, V extends Serializable>
        implements Serializable {

    private final U first;
    private final V second;

    /**
     * Creates a new pair.
     * @param u
     *      the value of the first component
     * @param v
     *      the value of the second component
     */
    public Pair(U u, V v) {
        first = u;
        second = v;
    }

    /**
     * Gets the first component of the pair.
     * @return
     *      the first component of the pair
     */
    public U getFirst() {
        return first;
    }

    /**
     * Gets the second component of the pair.
     * @return
     *      the second component of the pair
     */
    public V getSecond() {
        return second;
    }

}
