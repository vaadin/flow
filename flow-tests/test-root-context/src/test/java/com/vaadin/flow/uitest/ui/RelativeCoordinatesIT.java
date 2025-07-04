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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class RelativeCoordinatesIT extends ChromeBrowserTest {

    @Test
    public void clickShowsRelativeCoordinates() {
        open();

        WebElement clickArea = findElement(By.id(RelativeCoordinatesView.CLICK_AREA_ID));
        WebElement output = findElement(By.id(RelativeCoordinatesView.OUTPUT_ID));

        // Verify initial state
        Assert.assertEquals("Click on the area above to see coordinates", output.getText());

        // Click on the click area
        clickArea.click();

        // Verify output contains expected coordinate format
        String outputText = output.getText();
        Assert.assertTrue("Output should contain coordinate information", 
            outputText.contains("Screen:") && outputText.contains("Client:") && outputText.contains("Relative:"));

        // Verify the format matches the expected pattern
        String expectedPattern = "Screen: \\(\\d+, \\d+\\), Client: \\(\\d+, \\d+\\), Relative: \\(\\d+, \\d+\\)";
        Assert.assertTrue("Output should match coordinate pattern: " + outputText, 
            outputText.matches(expectedPattern));

        // Verify relative coordinates are reasonable (non-negative and within bounds)
        String[] parts = outputText.split(", Relative: \\(");
        if (parts.length >= 2) {
            String relativePart = parts[1].replace(")", "");
            String[] coords = relativePart.split(", ");
            if (coords.length == 2) {
                int relativeX = Integer.parseInt(coords[0]);
                int relativeY = Integer.parseInt(coords[1]);
                
                Assert.assertTrue("Relative X should be non-negative", relativeX >= 0);
                Assert.assertTrue("Relative Y should be non-negative", relativeY >= 0);
                // The click area has width 400px and height 200px + padding, so coordinates should be reasonable
                Assert.assertTrue("Relative X should be within reasonable bounds", relativeX < 600);
                Assert.assertTrue("Relative Y should be within reasonable bounds", relativeY < 400);
            }
        }
    }
}