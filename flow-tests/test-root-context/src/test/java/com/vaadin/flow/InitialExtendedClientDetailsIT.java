/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.not;

public class InitialExtendedClientDetailsIT extends ChromeBrowserTest {

    private final TypeSafeMatcher<String> isParseableAsInteger() {
        return new TypeSafeMatcher<String>() {

            @Override
            protected boolean matchesSafely(String s) {
                try {
                    Integer.parseInt(s);
                    return true;
                } catch (NumberFormatException nfe) {
                    return false;
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("only digits");
            }
        };
    }

    @Test
    public void verifyClientDetails() {
        open();

        Assert.assertThat(findElement(By.id("screenWidth")).getText(),
                isParseableAsInteger());
        Assert.assertThat(findElement(By.id("screenHeight")).getText(),
                isParseableAsInteger());
        Assert.assertThat(findElement(By.id("windowInnerWidth")).getText(),
                isParseableAsInteger());
        Assert.assertThat(findElement(By.id("windowInnerHeight")).getText(),
                isParseableAsInteger());
        Assert.assertThat(findElement(By.id("bodyClientWidth")).getText(),
                isParseableAsInteger());
        Assert.assertThat(findElement(By.id("bodyClientHeight")).getText(),
                isParseableAsInteger());
        Assert.assertThat(findElement(By.id("timezoneOffset")).getText(),
                isParseableAsInteger());
        Assert.assertThat(findElement(By.id("timeZoneId")).getText(),
                not(isEmptyString()));
        Assert.assertThat(findElement(By.id("rawTimezoneOffset")).getText(),
                isParseableAsInteger());
        Assert.assertThat(findElement(By.id("DSTSavings")).getText(),
                isParseableAsInteger());
        Assert.assertThat(findElement(By.id("DSTInEffect")).getText(),
                isOneOf("true", "false"));
        Assert.assertThat(findElement(By.id("currentDate")).getText(),
                not(isEmptyString()));
        Assert.assertThat(findElement(By.id("touchDevice")).getText(),
                isOneOf("true", "false"));
        Assert.assertThat(findElement(By.id("windowName")).getText(),
                not(isEmptyString()));
    }

}
