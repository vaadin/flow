package hummingbird.communication;

import com.vaadin.annotations.TemplateHTML;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Label;
import com.vaadin.ui.Template;
import com.vaadin.ui.UI;

import hummingbird.communication.PingPong.CounterTemplate.CounterModel;

public class PingPong extends UI {

    @TemplateHTML("Counter.html")
    public static class CounterTemplate extends Template {

        @Override
        protected void init() {
            super.init();
            // FIXME
            // getModel().setCount(0);
            getModel().setCount(1);
        }

        @Override
        protected CounterModel getModel() {
            return (CounterModel) super.getModel();
        }

        public interface CounterModel extends Model {
            public int getCount();

            public void setCount(int counter);
        }
    }

    CounterTemplate counterTemplate = new CounterTemplate();

    long start = 0L;

    @Override
    protected void init(VaadinRequest request) {
        addComponent(counterTemplate);
        counterTemplate.getElement().addEventListener("count", e -> {

            CounterModel model = counterTemplate.getModel();
            if (model.getCount() == 100) {
                start = System.currentTimeMillis();
            } else if (model.getCount() == 1100) {
                long end = System.currentTimeMillis();
                long time = end - start;
                addComponent(new Label("1000 updates in " + time + "ms"));
                return;
            }
            model.setCount(model.getCount() + 1);
        });
    }

}
