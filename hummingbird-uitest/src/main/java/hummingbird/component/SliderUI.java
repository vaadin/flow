package hummingbird.component;

import com.vaadin.server.VaadinRequest;
import com.vaadin.tests.components.AbstractTestUIWithLog;
import com.vaadin.ui.Label;
import com.vaadin.ui.Slider;

public class SliderUI extends AbstractTestUIWithLog {

    @Override
    protected void setup(VaadinRequest request) {
        Slider s1 = new Slider();

        Slider s2 = new Slider();
        s2.setValue(50.0);

        Slider s3 = new Slider();
        s3.setMin(40.0);
        s2.setValue(50.0);
        s3.setMax(70.0);
        s3.setPin(true);

        Slider s4 = new Slider();
        s4.setValue(90.0);
        s4.setStep(20);

        add(new Label("Default slider"), s1,
                new Label("Slider set to 50"), s2,
                new Label("Slider 40-70 with pin"), s3,
                new Label("Slider with step 20"), s4);
    }

}
