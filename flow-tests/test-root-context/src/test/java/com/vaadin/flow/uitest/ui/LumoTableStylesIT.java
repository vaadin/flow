/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

public class LumoTableStylesIT extends AbstractTableStylesIT {

    @Override
    protected String expectedCellPaddingBlock() {
        return "4px";
    }

    @Override
    protected String expectedCellPaddingInline() {
        return "16px";
    }

    @Override
    protected String expectedHeaderFontSize() {
        // Lumo tells the header apart with type: --lumo-font-size-s
        return "14px";
    }

    @Override
    protected String expectedBorderRadius() {
        // A Lumo grid, and so a Lumo table, has square corners
        return "0px";
    }

    @Override
    protected String[] expectedCompactCellPadding() {
        return new String[] { "2px", "8px" };
    }
}
