package hummingbird.template;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.annotations.TemplateEventHandler;
import com.vaadin.annotations.TemplateHTML;
import com.vaadin.hummingbird.kernel.StateNode.DataProvider;
import com.vaadin.hummingbird.kernel.StateNode.LazyList;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Template;
import com.vaadin.ui.UI;

public class LazyListScrolling extends UI {

    @TemplateHTML("LazyListScrolling.html")
    public static class LazyListInStateNodeTemplate extends Template {

        private MyDataProvider dataProvider;

        public LazyListInStateNodeTemplate() {
            dataProvider = new MyDataProvider();
            getModel().setItems(LazyList.create(dataProvider));
        }

        public interface MyModel extends Template.Model {
            public void setItems(LazyList items);

            public LazyList getItems();
        }

        @Override
        protected MyModel getModel() {
            return (MyModel) super.getModel();
        }

        @Override
        protected void init() {
            super.init();
        }

        @TemplateEventHandler
        public void extend() {
            extend(50);
        }

        public void extend(int count) {
            getModel().getItems().increaseActiveRangeEnd(count);
        }
    }

    public static class Data {
        private String string;
        private int integer;

        public Data(String string, int integer) {
            this.string = string;
            this.integer = integer;
        }

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        public int getInteger() {
            return integer;
        }

        public void setInteger(int integer) {
            this.integer = integer;
        }

    }

    public static class MyDataProvider implements DataProvider<Data> {

        @Override
        public List<Data> getValues(int index, int count) {
            List<Data> ret = new ArrayList<>();
            for (int i = index; i < index + count; i++) {
                ret.add(new Data("Value " + (i), i));
            }
            return ret;
        }

        @Override
        public Class<Data> getType() {
            return Data.class;
        }

    }

    @Override
    protected void init(VaadinRequest request) {
        LazyListInStateNodeTemplate template = new LazyListInStateNodeTemplate();
        addComponent(template);
        template.extend(100);
    }

}
