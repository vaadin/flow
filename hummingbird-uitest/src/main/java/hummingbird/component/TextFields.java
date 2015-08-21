package hummingbird.component;

import com.vaadin.event.FieldEvents.BlurNotifier;
import com.vaadin.event.FieldEvents.FocusNotifier;
import com.vaadin.server.VaadinRequest;
import com.vaadin.tests.components.AbstractTestUIWithLog;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;
import com.vaadin.ui.HasComponents;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

public class TextFields extends AbstractTestUIWithLog {

    private TextField tf1;
    private TextField tf2;
    private TextField tf3;
    private TextArea ta1;
    private TextArea ta2;
    private TextArea ta3;
    private PasswordField pf1;
    private PasswordField pf2;
    private PasswordField pf3;

    @Override
    protected void setup(VaadinRequest request) {
        getLayout().setSpacing(true);

        CheckBox cb = new CheckBox("Maxlength: 10");
        cb.addValueChangeListener(e -> {
            for (Component c : this) {
                setMaxLength(c, cb.getValue() ? 10 : -1);
            }
        });

        Button focusFirst = new Button("Focus first", e -> {
            tf1.focus();
        });

        Button selectInSecond = new Button("Select 2,5 in first", e -> {
            tf1.setSelectionRange(2, 5);
        });

        Label spacer = new Label("");
        HorizontalLayout hl = new HorizontalLayout(withCaption(cb), focusFirst,
                selectInSecond, spacer);
        hl.setSpacing(true);
        hl.setExpandRatio(spacer, 1);
        addComponent(hl);
        addComponent(createTextFields());
        addComponent(createPasswordFields());
        addComponent(createTextAreas());
    }

    private Component withCaption(Component c) {
        if (c instanceof CheckBox) {
            Label l = new Label(c.getCaption());
            HorizontalLayout hl = new HorizontalLayout(c, l);
            hl.setExpandRatio(l, 1);
            return hl;
        } else {
            VerticalLayout vl = new VerticalLayout(new Label(c.getCaption()),
                    c);
            vl.setSizeUndefined();
            return vl;
        }
    }

    private void setMaxLength(Component c, int maxLength) {
        if (c instanceof AbstractTextField) {
            ((AbstractTextField) c).setMaxLength(maxLength);
        }
        if (c instanceof HasComponents) {
            for (Component cc : (HasComponents) c) {
                setMaxLength(cc, maxLength);
            }
        }
    }

    private Component createTextFields() {
        HorizontalLayout hl = new HorizontalLayout();
        hl.setSpacing(true);
        tf1 = new TextField();
        tf1.setId("tf1");
        tf1.setValue("Initial value");
        tf1.addValueChangeListener(e -> {
            logValue(tf1);
        });
        tf2 = new TextField();
        tf2.setId("tf2");
        tf2.addValueChangeListener(e -> {
            logValue(tf2);
        });

        tf3 = new TextField();
        tf3.setId("tf3");
        tf3.setPlaceHolder("Please enter some text");
        tf3.addValueChangeListener(e -> {
            logValue(tf3);
        });

        addFocusBlurListeners(tf1, tf2, tf3);
        hl.addComponents(tf1, tf2, tf3);

        return hl;
    }

    private <T extends FocusNotifier & BlurNotifier> void addFocusBlurListeners(
            T... fs) {
        for (FocusNotifier f : fs) {
            f.addFocusListener(e -> {
                log("Focused " + ((Component) f).getId());
            });
        }
        for (BlurNotifier f : fs) {
            f.addBlurListener(e -> {
                log("Blur " + ((Component) f).getId());
            });
        }
    }

    private Component createTextAreas() {
        HorizontalLayout hl = new HorizontalLayout();
        hl.setSpacing(true);
        ta1 = new TextArea();
        ta1.setId("ta1");
        ta1.setValue("Initial value");
        ta1.addValueChangeListener(e -> {
            logValue(ta1);
        });
        ta2 = new TextArea();
        ta2.setId("ta2");
        ta2.addValueChangeListener(e -> {
            logValue(ta2);
        });

        ta3 = new TextArea();
        ta3.setId("ta3");
        ta3.setPlaceHolder("Please enter some text");
        ta3.addValueChangeListener(e -> {
            logValue(ta3);
        });

        addFocusBlurListeners(ta1, ta2, ta3);
        hl.addComponents(ta1, ta2, ta3);

        return hl;
    }

    private Component createPasswordFields() {
        HorizontalLayout hl = new HorizontalLayout();
        hl.setSpacing(true);
        pf1 = new PasswordField();
        pf1.setId("pf1");
        pf1.setValue("Initial value");
        pf1.addValueChangeListener(e -> {
            logValue(pf1);
        });
        pf2 = new PasswordField();
        pf2.setId("pf2");
        pf2.addValueChangeListener(e -> {
            logValue(pf2);
        });

        pf3 = new PasswordField();
        pf3.setId("pf3");
        pf3.setPlaceHolder("Please enter some text");
        pf3.addValueChangeListener(e -> {
            logValue(pf3);
        });

        addFocusBlurListeners(pf1, pf2, pf3);

        hl.addComponents(pf1, pf2, pf3);

        return hl;
    }

    private void logValue(Field<String> tf) {
        log("Value of " + tf.getId() + " changed to " + tf.getValue());
    }

}
