package hummingbird;

import com.vaadin.ui.Template;

public class ButtonTemplate extends Template {

    public ButtonTemplate(String text) {
        getModel().setText(text);
    }

    @Override
    protected ButtonModel getModel() {
        return (ButtonModel) super.getModel();
    }

    public interface ButtonModel extends Model {
        public void setText(String text);
    }
}
