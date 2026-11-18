/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.test.components;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

@TestFor(ShortcutsView.class)
public class ShortcutsIT extends AbstractDefaultIT {
    private static final Set<Keys> modifiers = Stream
            .of(Keys.SHIFT, Keys.ALT, Keys.CONTROL, Keys.META)
            .collect(Collectors.toSet());

    private static final String DEFAULT_VALUE = "testing...";

    @BeforeEach
    public void before() {
        open();
        resetKeys();
    }

    @BrowserTest
    public void clickShortcutWorks() {
        sendKeys(Keys.ALT, "b");
        assertActualEquals("button");
    }

    @BrowserTest
    public void focusShortcutWorks() {
        sendKeys(Keys.ALT, "f");

        WebElement input = findElement(By.id("input"));

        assertEquals(input, getDriver().switchTo().activeElement());
    }

    @BrowserTest
    public void shortcutsOnlyWorkWhenComponentIsVisible() {
        sendKeys(Keys.ALT, "v");
        assertActualEquals("invisibleP");

        // make the paragraph disappear
        sendKeys(Keys.ALT, "i");
        assertActualEquals("toggled!");

        sendKeys(Keys.ALT, "v");
        assertActualEquals("toggled!"); // did not change

        // make the paragraph appear
        sendKeys(Keys.ALT, "i");
        assertActualEquals("toggled!");

        sendKeys(Keys.ALT, "v");
        assertActualEquals("invisibleP");
    }

    @BrowserTest
    public void shortcutOnlyWorksWhenComponentIsEnabled() {
        sendKeys(Keys.CONTROL, Keys.SHIFT, "u");

        // clicking the button disables it, and clicking again should not have
        // and effect
        assertActualEquals("DISABLED CLICKED");

        resetActual();
        assertActualEquals(DEFAULT_VALUE);

        sendKeys(Keys.CONTROL, Keys.SHIFT, "u");
        assertActualEquals(DEFAULT_VALUE);
    }

    @BrowserTest
    public void shortcutWithDisabledUpdateModeAlwaysWorksWhenComponentIsDisabled() {
        sendKeys(Keys.CONTROL, Keys.SHIFT, "p");

        assertActualEquals("DISABLED CLICKED");

        resetActual();
        assertActualEquals(DEFAULT_VALUE);
    }

    @BrowserTest
    public void listenOnScopesTheShortcut() {
        sendKeys(Keys.ALT, "s");
        assertActualEquals(DEFAULT_VALUE); // nothing happened

        WebElement innerInput = findElement(By.id("focusTarget"));
        innerInput.sendKeys(Keys.ALT, "s");
        assertActualEquals("subview");

        // using the shortcut prevented "s" from being written
        Assertions.assertEquals("", innerInput.getText());
    }

    @BrowserTest
    public void multipleListenOnScopesTheShortcut() {
        sendKeys(Keys.ALT, "h");
        assertActualEquals(DEFAULT_VALUE); // nothing happened

        sendAltHKeysAndVerify("listenOn1");
        sendAltHKeysAndVerify("listenOn2");
        sendAltHKeysAndVerify("listenOn3");
    }

    private void sendAltHKeysAndVerify(String listenOnInputId) {
        resetActual();
        WebElement listenOn = findElement(By.id(listenOnInputId));
        listenOn.sendKeys(Keys.ALT, "h");
        assertActualEquals("Alt+H triggered on one of the listenOns");

        // using the shortcut prevented "h" from being written
        Assertions.assertEquals("", listenOn.getText());
    }

    @BrowserTest
    public void shortcutsOnlyWorkWhenComponentIsAttached() {
        sendKeys(Keys.ALT, "a");
        assertActualEquals(DEFAULT_VALUE); // nothing happens

        // attaches the component
        sendKeys(Keys.ALT, "y");
        assertActualEquals("toggled!");

        sendKeys(Keys.ALT, "a");
        assertActualEquals("attachable");

        // detaches the component
        sendKeys(Keys.ALT, "y");
        assertActualEquals("toggled!");

        sendKeys(Keys.ALT, "a");
        assertActualEquals("toggled!"); // nothing happens
    }

    @BrowserTest
    public void modifyingShortcutShouldChangeShortcutEvent() {
        // the shortcut in this test flips its own modifiers
        sendKeys(Keys.ALT, "g");
        assertActualEquals("Alt");

        sendKeys("G");
        assertActualEquals("Shift");

        // check that things revert back.
        sendKeys(Keys.ALT, "g");
        assertActualEquals("Alt");
    }

    @BrowserTest
    public void clickShortcutAllowsKeyDefaults() {
        WebElement textField1 = findElement(By.id("click-input-1"));
        WebElement textField2 = findElement(By.id("click-input-2"));

        // ClickButton1: allows browser's default behavior
        textField1.sendKeys("value 1");
        // using sendKeys(...) to send the ENTER instead of textField1
        // .sendKeys(...) since that causes the test to become flaky for some
        // reason
        sendKeys(Keys.ENTER);

        assertActualEquals("click: value 1");

        // ClickButton2: prevents browser's default behavior
        textField2.sendKeys("value 2");
        sendKeys(Keys.ENTER);

        assertActualEquals("click: ");
    }

    @BrowserTest
    public void clickShortcutWithMultipleListenOnAllowsKeyDefaults() {
        WebElement textField3 = findElement(By.id("click-input-3"));
        WebElement textField4 = findElement(By.id("click-input-4"));
        WebElement textField5 = findElement(By.id("click-input-5"));
        WebElement textField6 = findElement(By.id("click-input-6"));
        WebElement textField7 = findElement(By.id("click-input-7"));

        // ClickButton3: allows browser's default behavior
        textField3.sendKeys("value 3");
        // using sendKeys(...) to send the ENTER instead of textField3
        // .sendKeys(...) since that causes the test to become flaky for some
        // reason
        sendKeys(Keys.ENTER);

        assertActualEquals("click3: value 3,");

        textField4.sendKeys("value 4");
        sendKeys(Keys.ENTER);

        assertActualEquals("click3: value 3,value 4");

        // ClickButton4: prevents browser's default behavior
        textField5.sendKeys("value 5");
        sendKeys(Keys.ENTER);

        assertActualEquals("click4: value 5,");

        textField6.sendKeys("value 6");
        sendKeys(Keys.ENTER);

        assertActualEquals("click4: value 5,value 6");

        textField7.sendKeys("value 7");
        sendKeys(Keys.ENTER);

        assertActualEquals("click5: ");
    }

    @BrowserTest
    public void removingShortcutCleansJavascriptEventSettingsItUsed() {
        WebElement removalInput = findElement(By.id("removal-input"));

        Assertions.assertEquals("", removalInput.getAttribute("value"),
                "removalInput should be empty");

        // the removalInput has a shortcut bound on 'd'. When 'd' is typed,
        // instead of printing the letter, the contents are capitalized instead.
        // The shortcut is removed at the same time, so another 'd' should be
        // printed out.

        removalInput.sendKeys("abcd");
        Assertions.assertEquals("ABC", removalInput.getAttribute("value"),
                "removalInput should have 'ABC' and no 'd'");

        removalInput.sendKeys("abcd");
        Assertions.assertEquals("ABCabcd", removalInput.getAttribute("value"),
                "removalInput 'ABCabcd'. Since shortcut was removed, 'd' can "
                        + "be typed.");
    }

    @BrowserTest
    public void bindingShortcutToSameKeyWithDifferentModifiers_shouldNot_triggerTwice() {
        // they bindings are "o", "shift+o", and "alt+o"

        assertActualEquals(DEFAULT_VALUE);

        // bug #5454:
        // if the shortcut is without modifiers, bindings on that
        // key with modifiers also trigger

        // each shortcut has its own counter. each shortcut increments its
        // respective counter and then all the counters are concatenated into
        // "actual" text field. Should shortcuts cross-trigger, number two
        // will be part of the string
        // string order: [o][shift+o][alt+o]
        sendKeys("o");
        assertActualEquals("100");

        sendKeys("O"); // shift+o
        assertActualEquals("110");

        sendKeys(Keys.ALT, "o");
        assertActualEquals("111");
    }

    private void assertActualEquals(String expected) {
        Assertions.assertEquals(expected,
                findElement(By.id("actual")).getAttribute("value"));
    }

    private void resetActual() {
        WebElement actual = findElement(By.id("actual"));
        WebElement blur = findElement(By.id("blur"));
        actual.clear();
        actual.sendKeys("testing...");
        blur.click();
    }

    private void sendKeys(CharSequence... keys) {
        Actions actions = new Actions(getDriver());
        for (CharSequence keySeq : keys) {
            if (modifiers.contains(keySeq)) {
                actions.keyDown(keySeq);
            } else {
                actions.sendKeys(keySeq);
            }
        }
        actions.build().perform();
        // Implementation that worked for driver < 75.beta:
        // new Actions(driver).sendKeys(keys).build().perform();
        // if keys are not reset, alt will remain down and start flip-flopping
        resetKeys();
    }

    private void resetKeys() {
        Actions actions = new Actions(getDriver());
        modifiers.forEach(actions::keyUp);
        actions.build().perform();
        // Implementation that worked for driver < 75.beta:
        // new Actions(driver).sendKeys(Keys.NULL).build().perform();
    }
}
