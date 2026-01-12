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
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.DnDAbsolutePositioning", layout = ViewTestLayout.class)
public class DnDAbsolutePositioning extends Div implements DropTarget<DnDAbsolutePositioning> {

    {
        // Configure styles for absolutely positioned canvas
        getStyle().setPosition(Style.Position.RELATIVE);
        getStyle().setBackgroundColor("lightyellow");
        getStyle().setOverflow(Style.Overflow.HIDDEN);
        setHeight("50vh");
        setWidth("50vw");

        // Add a draggable brick, see that separately
        Brick brick = new Brick();
        add(brick);

        // Make the canvas accept drops
        setActive(true);
        addDropListener(event -> {
            int dx = event.getClientX() - brick.getStartX();
            int dy = event.getClientY() - brick.getStartY();
            brick.moveBy(dx, dy);
            log("Pixels moved x:" + dx + " y:" + dy);
        });
    }

    private void log(String msg) {
        Pre pre = new Pre(msg);
        pre.getStyle().setColor("gray");
        addComponentAsFirst(pre);
    }

    public class Brick extends Div implements DragSource<Brick> {

        private int startX;
        private int startY;

        private int offsetX = 0;
        private int offsetY = 0;

        public Brick() {
            getStyle().setPosition(Style.Position.ABSOLUTE);
            getStyle().setPadding("1em");
            getStyle().setBackgroundColor("pink");
            setDraggable(true);
            setText("DragMe");

            addDragStartListener(e -> {
                startX = e.getClientX();
                startY = e.getClientY();
            });
        }

        public int getStartX() {
            return startX;
        }

        public int getStartY() {
            return startY;
        }

        public void moveBy(int dx, int dy) {
            offsetX = offsetX + dx;
            offsetY = offsetY + dy;
            getStyle().setLeft(offsetX + "px");
            getStyle().setTop(offsetY + "px");
        }
    }

}
