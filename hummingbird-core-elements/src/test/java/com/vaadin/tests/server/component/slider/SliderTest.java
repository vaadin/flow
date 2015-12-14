package com.vaadin.tests.server.component.slider;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.hamcrest.number.IsCloseTo;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.ui.Slider;

public class SliderTest {

    @Test
    public void minCannotBeLargerThanMax() {
        Slider slider = new Slider();

        slider.setMax(100);
        slider.setMin(101);

        assertThat(slider.getMin(), is(101.0));
        assertThat(slider.getMax(), is(101.0));
    }

    @Test
    public void maxCannotBeSmallerThanMin() {
        Slider slider = new Slider();

        slider.setMin(50);
        slider.setMax(10);

        assertThat(slider.getMax(), is(10.0));
        assertThat(slider.getMin(), is(10.0));
    }

    @Test
    public void valueOutOfBoundsExceptionMessageContainsBounds() {
        Slider slider = new Slider();

        try {

            slider.setValue(-1.0);
        } catch (Slider.ValueOutOfBoundsException e) {
            assertThat(e.getMessage(), containsString(
                    "Value -1.0 is out of bounds: [0.0, 100.0]"));
        }
    }

    @Test
    public void valueIsSet() {
        Slider slider = new Slider();

        slider.setValue(5.0);

        assertThat(slider.getValue(), is(5.0));
    }

    @Test
    public void valueCannotBeOutOfBounds() {
        Slider s = new Slider(0, 10);

        try {
            s.setValue(20.0);
            Assert.fail("Should throw out of bounds exception");
        } catch (Slider.ValueOutOfBoundsException e) {
            // TODO: handle exception
        }
    }

    @Test
    public void valueCanHaveLargePrecision() {
        Slider slider = new Slider();
        slider.setStep(0.00000000000001);

        slider.setValue(99.012345678912);

        assertThat(slider.getValue(), is(99.012345678912));
    }

    @Test
    public void doublesCanBeUsedAsLimits() {
        Slider slider = new Slider(1.5, 2.5, 0.1);

        assertThat(slider.getMin(), is(1.5));
        assertThat(slider.getValue(), is(1.5));
        assertThat(slider.getMax(), is(2.5));
    }

    @Test
    public void valuesGreaterThanIntMaxValueCanBeUsed() {
        double minValue = (double) Integer.MAX_VALUE + 1;

        Slider s = new Slider(minValue, minValue + 1, 1);
        assertThat(s.getValue(), is(minValue));
    }

    @Test
    public void valueWithSmallerPrecisionCanBeUsed() {
        Slider slider = new Slider(0, 100, 0.1);

        slider.setValue(1.2);

        assertThat(slider.getValue(), IsCloseTo.closeTo(1.2, 0.00001));
    }

    @Test
    public void valueWithLargerPrecisionCanBeUsed() {
        Slider slider = new Slider(0, 100, 0.01);

        slider.setValue(1.2345);

        assertThat(slider.getValue(), is(1.23));
    }
}
