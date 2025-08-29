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
package com.vaadin.flow.component.html;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;

public class ImageTest extends ComponentTest {

    // Actual test methods in super class

    @Override
    protected void addProperties() {
        addStringProperty("src", "");
    }

    @Test
    @Override
    public void testHasAriaLabelIsImplemented() {
        super.testHasAriaLabelIsImplemented();
    }

    @Test
    public void emptyAltKeepsAttribute() {
        Image img = new Image("test.png", "");
        Assert.assertEquals("", img.getAlt().get());
        Assert.assertTrue(img.getElement().hasAttribute("alt"));
        img.setAlt(null);
        Assert.assertEquals(Optional.empty(), img.getAlt());
        Assert.assertFalse(img.getElement().hasAttribute("alt"));
    }

    @Test
    public void downloadHandler_isSetToInline() {
        Element element = Mockito.mock(Element.class);
        class TestImage extends Image {
            public TestImage(DownloadHandler downloadHandler, String alt) {
                super(downloadHandler, alt);
            }

            @Override
            public Element getElement() {
                return element;
            }
        }
        // dummy handler
        InputStreamDownloadHandler handler = DownloadHandler
                .fromInputStream(event -> DownloadResponse.error(500));
        Assert.assertFalse(handler.isInline());
        new TestImage(handler, "test.png");
        Assert.assertTrue(handler.isInline());
    }

    @Test
    public void byteArrayConstructor_setsAltAndSrc() {
        byte[] imageData = "test image data".getBytes();
        String imageName = "test.png";
        
        Image image = new Image(imageData, imageName);
        
        // Verify alt text is set to image name
        Assert.assertEquals(imageName, image.getAlt().get());
        
        // Verify src attribute is set (should be a blob URL or similar)
        String src = image.getSrc();
        Assert.assertNotNull("Source should be set", src);
        Assert.assertFalse("Source should not be empty", src.isEmpty());
    }

    @Test
    public void byteArrayConstructor_handlesNullContentType() {
        byte[] imageData = "test image data".getBytes();
        String imageName = "test"; // No extension to test content type handling
        
        Image image = new Image(imageData, imageName);
        
        // Should still work even without file extension
        Assert.assertEquals(imageName, image.getAlt().get());
        Assert.assertNotNull(image.getSrc());
    }

    @Test
    public void byteArrayConstructor_handlesEmptyArray() {
        byte[] imageData = new byte[0];
        String imageName = "empty.png";
        
        Image image = new Image(imageData, imageName);
        
        // Should handle empty byte array gracefully
        Assert.assertEquals(imageName, image.getAlt().get());
        Assert.assertNotNull(image.getSrc());
    }
}
