package hummingbird.templatewithcomponent;

import java.util.List;

import com.vaadin.annotations.Bower;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;

import hummingbird.templatewithcomponent.TemplateWCUsingForLoop.Template.Item;

public class TemplateWCUsingForLoop extends UI {

    @Bower("paper-menu")
    public static class Template extends com.vaadin.ui.Template {

        public interface Item {
            public String getFoo();

            public void setFoo(String foo);

            public int getBar();

            public void setBar(int bar);
        }

        public interface Model extends com.vaadin.ui.Template.Model {
            public List<Item> getItems();

            public void setItems(List<Item> items);
        }

        @Override
        protected Model getModel() {
            return (Model) super.getModel();
        }
    }

    @Override
    protected void init(VaadinRequest request) {
        Template t = new Template();
        t.getElement().getNode().getMultiValued("items");
        List<Item> items = t.getModel().getItems();
        Item item1 = com.vaadin.ui.Template.Model.create(Item.class);
        item1.setFoo("Foo1");
        item1.setBar(1);
        Item item2 = com.vaadin.ui.Template.Model.create(Item.class);
        item2.setFoo("Foo2");
        item2.setBar(2);

        items.add(item1);
        items.add(item2);
        addComponent(t);

        addComponent(new Button("Remove last", e -> {
            List<Item> i = t.getModel().getItems();
            i.remove(i.size() - 1);
        }));
        addComponent(new Button("Remove first", e -> {
            List<Item> i = t.getModel().getItems();
            i.remove(0);
        }));
    }

}
