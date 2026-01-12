/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Test UI demonstrating use of DropEvent offset methods for positioning
 * elements at the drop location within the drop target.
 * <p>
 * Uses getOffsetX/Y for drop position and getDragStartOffsetX/Y to account for
 * where the user grabbed the item, resulting in natural drag behavior.
 */
@Route(value = "com.vaadin.flow.uitest.ui.DnDAttachToDropLocationView", layout = ViewTestLayout.class)
public class DnDAttachToDropLocationView extends Div {

    public DnDAttachToDropLocationView() {
        // Create a palette of draggable items
        Div palette = new Div() {{
            getStyle().setDisplay(Style.Display.FLEX);
            getStyle().setGap("10px");
            getStyle().setPadding("10px");
            add(new DraggableItem("Red", "red"));
            add(new DraggableItem("Green", "green"));
            add(new DraggableItem("Blue", "blue"));
        }};

        // Add instruction text
        Div instructions = new Div(
                "Drag items from the palette and drop them on the canvas below. "
                        + "They will appear at the exact drop location using offsetX/offsetY.");
        instructions.getStyle().setPadding("10px");
        instructions.getStyle().setColor("gray");

        add(instructions, palette, new Canvas());
    }

    /**
     * A colored item that can be dragged from the palette.
     */
    public class DraggableItem extends ColoredItem
            implements DragSource<DraggableItem> {

        public DraggableItem(String label, String color) {
            super(label, color);
            getStyle().setCursor("grab");
            setDragData(color);
            setDraggable(true);
        }
    }

    /**
     * A colored item with consistent styling.
     */
    public class ColoredItem extends Div {

        public ColoredItem(String label, String color) {
            super(label);
            getStyle().setPadding("5px");
            getStyle().setBackgroundColor(color);
            getStyle().setColor("white");
        }
    }

    /**
     * Drop target canvas where items can be placed at exact drop coordinates.
     */
    public class Canvas extends Div implements DropTarget<Canvas> {

        private int itemCounter = 0;

        public Canvas() {
            getStyle().setPosition(Style.Position.RELATIVE);
            getStyle().setBackgroundColor("lightyellow");
            setHeight("400px");
            setWidth("600px");

            setActive(true);
            addDropListener(event -> {
                // Calculate position accounting for where user grabbed the item
                int x = event.getOffsetX()
                        - event.getDragStartOffsetX().orElse(0);
                int y = event.getOffsetY()
                        - event.getDragStartOffsetY().orElse(0);

                String color = (String) event.getDragData().orElse("gray");
                ColoredItem dropped = new ColoredItem(
                        "Item " + (++itemCounter), color);
                dropped.getStyle().setPosition(Style.Position.ABSOLUTE);
                dropped.getStyle().setLeft(x + "px");
                dropped.getStyle().setTop(y + "px");
                add(dropped);
            });
        }
    }
}
