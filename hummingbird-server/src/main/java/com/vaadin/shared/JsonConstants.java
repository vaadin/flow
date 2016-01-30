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
package com.vaadin.shared;

import java.io.Serializable;

public class JsonConstants implements Serializable {
    public static final String VTYPE_CONNECTOR = "c";
    public static final String VTYPE_BOOLEAN = "b";
    public static final String VTYPE_DOUBLE = "d";
    public static final String VTYPE_FLOAT = "f";
    public static final String VTYPE_LONG = "l";
    public static final String VTYPE_INTEGER = "i";
    public static final String VTYPE_STRING = "s";
    public static final String VTYPE_ARRAY = "a";
    public static final String VTYPE_STRINGARRAY = "S";
    public static final String VTYPE_MAP = "m";
    public static final String VTYPE_LIST = "L";
    public static final String VTYPE_SET = "q";
    public static final String VTYPE_NULL = "n";

    public static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";

    /**
     * Key holding id of the node affected by a change.
     *
     * @since
     */
    public static final String CHANGE_NODE = "node";

    /**
     * Key holding the type of a change.
     *
     * @since
     */
    public static final String CHANGE_TYPE = "type";

    /**
     * Change type for attaching nodes.
     *
     * @since
     */
    public static final String CHANGE_TYPE_ATTACH = "attach";

    /**
     * Change type for detaching nodes.
     *
     * @since
     */
    public static final String CHANGE_TYPE_DETACH = "detach";

    /**
     * Change type for list splice changes.
     *
     * @since
     */
    public static final String CHANGE_TYPE_SPLICE = "splice";

    /**
     * Change type for map put changes.
     *
     * @since
     */
    public static final String CHANGE_TYPE_PUT = "put";

    /**
     * Change type for map remove changes.
     *
     * @since
     */
    public static final String CHANGE_TYPE_REMOVE = "remove";

    /**
     * Key holding the namespace of a change.
     *
     * @since
     */
    public static final String CHANGE_NAMESPACE = "ns";

    /**
     * Key holding the map key of the change.
     *
     * @since
     */
    public static final String CHANGE_MAP_KEY = "key";

    /**
     * Key holding nodes to add for a splice.
     *
     * @since
     */
    public static final String CHANGE_SPLICE_ADD_NODES = "addNodes";

    /**
     * Key holding values to add for a splice.
     *
     * @since
     */
    public static final String CHANGE_SPLICE_ADD = "add";

    /**
     * Key holding the number of items to remove for a splice.
     *
     * @since
     */
    public static final String CHANGE_SPLICE_REMOVE = "remove";

    /**
     * Key holding the index of a splice.
     *
     * @since
     */
    public static final String CHANGE_SPLICE_INDEX = "index";

    /**
     * Key holding the value of a put change.
     *
     * @since
     */
    public static final String CHANGE_PUT_VALUE = "value";

    /**
     * Key holder the node value of a put change.
     *
     * @since
     */
    public static final String CHANGE_PUT_NODE_VALUE = "nodeValue";
}
