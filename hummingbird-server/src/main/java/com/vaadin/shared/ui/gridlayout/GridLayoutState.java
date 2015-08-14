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
package com.vaadin.shared.ui.gridlayout;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.vaadin.shared.Connector;
import com.vaadin.shared.ui.JavaScriptComponentState;
import com.vaadin.ui.Alignment;

public class GridLayoutState extends JavaScriptComponentState {
    public static Alignment ALIGNMENT_DEFAULT = Alignment.TOP_LEFT;

    {
        primaryStyleName = "v-gridlayout";
    }

    public boolean spacing = false;
    public int rows = 0;
    public int columns = 0;
    // Set of indexes of implicitly Ratios rows and columns
    public Set<Integer> explicitRowRatios = new HashSet<Integer>();;
    public Set<Integer> explicitColRatios = new HashSet<Integer>();
    public Map<Connector, ChildComponentData> childData = new HashMap<Connector, GridLayoutState.ChildComponentData>();
    public boolean hideEmptyRowsAndColumns = false;
    public boolean margin = false;

    public static class ChildComponentData implements Serializable {
        public int column1;
        public int row1;
        public int column2;
        public int row2;
        public Alignment alignment = ALIGNMENT_DEFAULT;

    }
}
