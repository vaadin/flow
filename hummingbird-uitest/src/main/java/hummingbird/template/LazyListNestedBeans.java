package hummingbird.template;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.annotations.TemplateEventHandler;
import com.vaadin.annotations.TemplateHTML;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.hummingbird.kernel.StateNode.DataProvider;
import com.vaadin.hummingbird.kernel.StateNode.LazyList;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Template;
import com.vaadin.ui.UI;

public class LazyListNestedBeans extends UI {

    public static class ComplexBean {
        private String name;
        private ChildBean child;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ChildBean getChild() {
            return child;
        }

        public void setChild(ChildBean child) {
            this.child = child;
        }

    }

    public static class ChildBean {
        private int age;

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

    }

    public static class ComplexDataProvider
            implements DataProvider<ComplexBean> {

        @Override
        public List<ComplexBean> getValues(int index, int count) {
            List<ComplexBean> ret = new ArrayList<>();
            for (int i = index; i < index + count; i++) {
                ComplexBean cb = new ComplexBean();
                cb.setName("name " + i);
                ChildBean child = new ChildBean();
                child.setAge(i);
                cb.setChild(child);
                ret.add(cb);
            }
            return ret;
        }

        @Override
        public Class<ComplexBean> getType() {
            return ComplexBean.class;
        }

    }

    @TemplateHTML("LazyListNestedBeans.html")
    public static class Tpl extends Template {

        private LazyList<StateNode> lazyList;
        private ComplexDataProvider dataProvider;

        public LazyList<StateNode> getLazyList() {
            return lazyList;
        }

        public Tpl() {
        }

        public interface MyModel extends Template.Model {
            public int getPageSize();

            public void setPageSize(int pageSize);
        }

        @Override
        protected MyModel getModel() {
            return (MyModel) super.getModel();
        }

        @Override
        protected void init() {
            super.init();
            getModel().setPageSize(10);
            dataProvider = new ComplexDataProvider();
            lazyList = getElement().getNode().getLazyMultiValued("items",
                    dataProvider);
            lazyList.setActiveRangeEnd(getModel().getPageSize());
        }

        @TemplateEventHandler
        public void increasePageSize() {
            getModel().setPageSize(getModel().getPageSize() + 10);
            lazyList.increaseActiveRangeEnd(10);
        }

        @TemplateEventHandler
        public void decreasePageSize() {
            getModel().setPageSize(getModel().getPageSize() - 10);
            lazyList.decreaseActiveRangeEnd(10);
        }

        @TemplateEventHandler
        public void next() {
            int pageSize = getModel().getPageSize();
            lazyList.increaseActiveRangeEnd(pageSize);
            lazyList.increaseActiveRangeStart(pageSize);
        }

        @TemplateEventHandler
        public void prev() {
            int pageSize = getModel().getPageSize();
            lazyList.decreaseActiveRangeStart(pageSize);
            lazyList.setActiveRangeEnd(
                    lazyList.getActiveRangeStart() + pageSize);
        }
    }

    @Override
    protected void init(VaadinRequest request) {
        Tpl template = new Tpl();
        addComponent(template);
    }

}
