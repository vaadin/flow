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

package com.vaadin.flow.uitest.ui.template.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.template.collections.ModelListView.MyModel;

@Route(value = "com.vaadin.flow.uitest.ui.template.collections.ModelListView", layout = ViewTestLayout.class)
@Tag("model-list")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/collections/ModelList.html")
@JsModule("ModelList.js")
public class ModelListView extends PolymerTemplate<MyModel> {

    public interface MyModel extends TemplateModel {
        List<Item> getItems();

        List<List<Item>> getLotsOfItems();

        void setLotsOfItems(List<List<Item>> lotsOfItems);

        void setItems(List<Item> items);

        List<Item> getMoreItems();

        ItemWithItems getItemWithItems();

        void setItemWithItems(ItemWithItems itemWithItems);

        ItemWithItem getItemWithItem();

        void setItemWithItem(ItemWithItem itemWithItem);
    }

    public static class Item {
        private String text;
        private boolean clicked;

        public Item() {
        }

        public Item(String text) {
            setText(text);
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public boolean isClicked() {
            return clicked;
        }

        public void setClicked(boolean clicked) {
            this.clicked = clicked;
        }
    }

    public static class ItemWithItems {
        private List<Item> items;

        public List<Item> getItems() {
            return items;
        }

        public void setItems(List<Item> items) {
            this.items = items;
        }
    }

    public static class ItemWithItem {
        private Item item;

        public Item getItem() {
            return item;
        }

        public void setItem(Item item) {
            this.item = item;
        }
    }

    public ModelListView() {
        getModel().getItems().add(new Item("Item 1"));
        getModel().getMoreItems().add(new Item("Item 2"));
        getModel().setLotsOfItems(
                Arrays.asList(new ArrayList<>(), new ArrayList<>()));
        getModel().getLotsOfItems().get(0).add(new Item("Item 3"));
        getModel().getLotsOfItems().get(1).add(new Item("Item 4"));

        getModel().setItemWithItems(new ItemWithItems());
        getModel().getItemWithItems().getItems().add(new Item("Item 5"));

        getModel().setItemWithItem(new ItemWithItem());
        getModel().getItemWithItem().setItem(new Item("Item 6"));
    }

    @ClientCallable
    public void add() {
        getModel().getItems().add(new Item("New item 1"));
        getModel().getMoreItems().add(new Item("New item 2"));

        getModel().getLotsOfItems().get(0).add(new Item("New item 3"));
        getModel().getLotsOfItems().get(1).add(new Item("New item 4"));

        getModel().getItemWithItems().getItems().add(new Item("New item 5"));

        getModel().getItemWithItem().setItem(new Item("New item 6"));
    }

    @ClientCallable
    public void toggle(Item item) {
        item.setClicked(!item.isClicked());
    }

    @ClientCallable
    public void setNullTexts() {
        getModel().getItems().forEach(item -> item.setText(null));
        getModel().getMoreItems().forEach(item -> item.setText(null));
        getModel().getLotsOfItems()
                .forEach(list -> list.forEach(item -> item.setText(null)));

        getModel().getItemWithItems().getItems()
                .forEach(item -> item.setText(null));

        getModel().getItemWithItem().getItem().setText(null);
    }

}
