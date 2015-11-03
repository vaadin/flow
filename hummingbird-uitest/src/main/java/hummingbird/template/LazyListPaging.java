package hummingbird.template;

import com.vaadin.annotations.TemplateEventHandler;
import com.vaadin.annotations.TemplateHTML;
import com.vaadin.hummingbird.kernel.StateNode.LazyList;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Template;
import com.vaadin.ui.UI;

import hummingbird.template.LazyListScrolling.MyDataProvider;

public class LazyListPaging extends UI {

    @TemplateHTML("LazyListPaging.html")
    public static class Tpl extends Template {

        private MyDataProvider dataProvider;

        public Tpl() {
        }

        public interface MyModel extends Template.Model {
            public int getPageSize();

            public void setPageSize(int pageSize);

            public LazyList getItems();

            public void setItems(LazyList items);
        }

        @Override
        protected MyModel getModel() {
            return (MyModel) super.getModel();
        }

        @Override
        protected void init() {
            super.init();
            getModel().setPageSize(10);
            dataProvider = new MyDataProvider();
            getModel().setItems(LazyList.create(dataProvider));

            getModel().getItems().setActiveRangeEnd(getModel().getPageSize());
        }

        @TemplateEventHandler
        public void increasePageSize() {
            getModel().setPageSize(getModel().getPageSize() + 10);
            getModel().getItems().increaseActiveRangeEnd(10);
        }

        @TemplateEventHandler
        public void decreasePageSize() {
            getModel().setPageSize(getModel().getPageSize() - 10);
            getModel().getItems().decreaseActiveRangeEnd(10);
        }

        @TemplateEventHandler
        public void next() {
            int pageSize = getModel().getPageSize();
            getModel().getItems().increaseActiveRangeEnd(pageSize);
            getModel().getItems().increaseActiveRangeStart(pageSize);
        }

        @TemplateEventHandler
        public void prev() {
            int pageSize = getModel().getPageSize();
            LazyList items = getModel().getItems();
            items.decreaseActiveRangeStart(pageSize);
            items.setActiveRangeEnd(items.getActiveRangeStart() + pageSize);
        }
    }

    @Override
    protected void init(VaadinRequest request) {
        Tpl template = new Tpl();
        addComponent(template);
    }

}
