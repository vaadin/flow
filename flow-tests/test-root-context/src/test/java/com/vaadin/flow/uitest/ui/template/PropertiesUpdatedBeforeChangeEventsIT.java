package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class PropertiesUpdatedBeforeChangeEventsIT extends ChromeBrowserTest {

    private WebElement firstPropInput;
    private WebElement secondPropDiv;
    private WebElement serverSetTextDiv;

    @Before
    public void init() {
        open();
        firstPropInput = getElementById("first-prop-input");
        secondPropDiv = getElementById("second-prop-div");
        serverSetTextDiv = getElementById("text-div");
    }

    @Test
    public void all_properties_update_before_change_event_handlers_are_called() {
        assertTextsCorrect("");
        String textToSet = "abcdefg";
        firstPropInput.sendKeys(textToSet);
        assertTextsCorrect(textToSet);
    }

    private void assertTextsCorrect(String expected) {
        Assert.assertEquals(expected, secondPropDiv.getText());
        Assert.assertEquals(secondPropDiv.getText(),
                serverSetTextDiv.getText());
    }

    private WebElement getElementById(String id) {
        return $("properties-updated-before-change-events").first()
                .$(TestBenchElement.class).id(id);
    }
}
