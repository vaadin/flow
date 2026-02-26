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
package com.vaadin.flow.sampler.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.sampler.SamplerMainLayout;

/**
 * Demonstrates the Image component and its features.
 */
@Route(value = "image", layout = SamplerMainLayout.class)
@PageTitle("Image Sampler")
public class ImageSamplerView extends Div {

    public ImageSamplerView() {
        setId("image-sampler");

        add(new H1("Image Component"));
        add(new Paragraph("The Image component displays images with various configurations."));

        add(createSection("Basic Images",
            "Simple image with source and alt text.",
            createBasicImagesDemo()));

        add(createSection("Image Sizing",
            "Images with different size configurations.",
            createSizingDemo()));

        add(createSection("Placeholder Images",
            "Using placeholder images for demonstrations.",
            createPlaceholderDemo()));

        add(createSection("Image Gallery",
            "Grid layout of images.",
            createGalleryDemo()));

        add(createSection("Dynamic Image Source",
            "Change image source dynamically.",
            createDynamicDemo()));
    }

    private Div createSection(String title, String description, Div content) {
        Div section = new Div();
        section.getStyle()
            .set("margin-bottom", "40px")
            .set("padding", "20px")
            .set("border", "1px solid #e0e0e0")
            .set("border-radius", "8px");

        H2 sectionTitle = new H2(title);
        sectionTitle.getStyle().set("margin-top", "0");

        Paragraph desc = new Paragraph(description);
        desc.getStyle().set("color", "#666");

        section.add(sectionTitle, desc, new Hr(), content);
        return section;
    }

    private Div createBasicImagesDemo() {
        Div demo = new Div();
        demo.setId("basic-images");

        // Using a data URL for a simple colored rectangle
        String redSquare = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='150' height='100'%3E%3Crect width='150' height='100' fill='%234CAF50'/%3E%3Ctext x='75' y='55' text-anchor='middle' fill='white' font-family='Arial' font-size='14'%3ESample Image%3C/text%3E%3C/svg%3E";

        Image basicImage = new Image(redSquare, "Sample image");
        basicImage.setId("basic-image");
        basicImage.getStyle()
            .set("display", "block")
            .set("margin-bottom", "15px")
            .set("border-radius", "8px");

        Image imageWithTitle = new Image(redSquare, "Image with title");
        imageWithTitle.setId("titled-image");
        imageWithTitle.setTitle("This is the title attribute");
        imageWithTitle.getStyle()
            .set("display", "block")
            .set("border-radius", "8px");

        Paragraph hint = new Paragraph("Hover over the second image to see its title.");
        hint.getStyle().set("font-size", "0.85em").set("color", "#666").set("font-style", "italic");

        demo.add(basicImage, imageWithTitle, hint);
        return demo;
    }

    private Div createSizingDemo() {
        Div demo = new Div();
        demo.setId("sizing-demo");
        demo.getStyle()
            .set("display", "flex")
            .set("gap", "20px")
            .set("flex-wrap", "wrap")
            .set("align-items", "flex-end");

        String[] sizes = {"50x50", "100x75", "150x100", "200x150"};
        String[] colors = {"%231976d2", "%234CAF50", "%23f57c00", "%23d32f2f"};
        String[] labels = {"Small", "Medium", "Large", "X-Large"};

        for (int i = 0; i < sizes.length; i++) {
            String[] dims = sizes[i].split("x");
            String svg = String.format(
                "data:image/svg+xml,%%3Csvg xmlns='http://www.w3.org/2000/svg' width='%s' height='%s'%%3E%%3Crect width='%s' height='%s' fill='%s'/%%3E%%3Ctext x='%d' y='%d' text-anchor='middle' fill='white' font-family='Arial' font-size='12'%%3E%sx%s%%3C/text%%3E%%3C/svg%%3E",
                dims[0], dims[1], dims[0], dims[1], colors[i],
                Integer.parseInt(dims[0]) / 2, Integer.parseInt(dims[1]) / 2 + 5,
                dims[0], dims[1]
            );

            Div container = new Div();
            container.getStyle().set("text-align", "center");

            Image img = new Image(svg, labels[i] + " image");
            img.setId("sized-image-" + (i + 1));
            img.setWidth(dims[0] + "px");
            img.setHeight(dims[1] + "px");
            img.getStyle().set("border-radius", "4px");

            Div label = new Div(labels[i]);
            label.getStyle()
                .set("margin-top", "5px")
                .set("font-size", "0.85em")
                .set("color", "#666");

            container.add(img, label);
            demo.add(container);
        }

        return demo;
    }

    private Div createPlaceholderDemo() {
        Div demo = new Div();
        demo.setId("placeholder-demo");
        demo.getStyle()
            .set("display", "flex")
            .set("gap", "20px")
            .set("flex-wrap", "wrap");

        // Create placeholder SVGs with different content
        String avatarSvg = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='100' height='100'%3E%3Ccircle cx='50' cy='50' r='50' fill='%239c27b0'/%3E%3Ccircle cx='50' cy='40' r='18' fill='white'/%3E%3Cellipse cx='50' cy='85' rx='30' ry='20' fill='white'/%3E%3C/svg%3E";

        String landscapeSvg = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='200' height='120'%3E%3Crect width='200' height='120' fill='%2387CEEB'/%3E%3Cpolygon points='0,120 60,60 120,120' fill='%23228B22'/%3E%3Cpolygon points='80,120 140,50 200,120' fill='%23006400'/%3E%3Ccircle cx='160' cy='30' r='20' fill='%23FFD700'/%3E%3C/svg%3E";

        String productSvg = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='150' height='150'%3E%3Crect width='150' height='150' fill='%23f5f5f5'/%3E%3Crect x='25' y='50' width='100' height='80' rx='5' fill='%23e0e0e0'/%3E%3Ctext x='75' y='100' text-anchor='middle' fill='%239e9e9e' font-family='Arial' font-size='12'%3EProduct%3C/text%3E%3C/svg%3E";

        Image avatar = new Image(avatarSvg, "User avatar");
        avatar.setId("avatar-placeholder");
        avatar.getStyle()
            .set("border-radius", "50%")
            .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");

        Image landscape = new Image(landscapeSvg, "Landscape placeholder");
        landscape.setId("landscape-placeholder");
        landscape.getStyle()
            .set("border-radius", "8px")
            .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");

        Image product = new Image(productSvg, "Product placeholder");
        product.setId("product-placeholder");
        product.getStyle()
            .set("border-radius", "8px")
            .set("border", "1px solid #e0e0e0");

        demo.add(avatar, landscape, product);
        return demo;
    }

    private Div createGalleryDemo() {
        Div demo = new Div();
        demo.setId("gallery-demo");
        demo.getStyle()
            .set("display", "grid")
            .set("grid-template-columns", "repeat(4, 1fr)")
            .set("gap", "10px");

        String[] colors = {
            "%23f44336", "%23e91e63", "%239c27b0", "%23673ab7",
            "%233f51b5", "%232196f3", "%2303a9f4", "%2300bcd4",
            "%23009688", "%234caf50", "%238bc34a", "%23cddc39"
        };

        for (int i = 0; i < 12; i++) {
            String svg = String.format(
                "data:image/svg+xml,%%3Csvg xmlns='http://www.w3.org/2000/svg' width='100' height='100'%%3E%%3Crect width='100' height='100' fill='%s'/%%3E%%3Ctext x='50' y='55' text-anchor='middle' fill='white' font-family='Arial' font-size='24'%%3E%d%%3C/text%%3E%%3C/svg%%3E",
                colors[i], i + 1
            );

            Image img = new Image(svg, "Gallery image " + (i + 1));
            img.setId("gallery-img-" + (i + 1));
            img.getStyle()
                .set("width", "100%")
                .set("aspect-ratio", "1")
                .set("object-fit", "cover")
                .set("border-radius", "8px")
                .set("cursor", "pointer")
                .set("transition", "transform 0.2s");

            demo.add(img);
        }

        return demo;
    }

    private Div createDynamicDemo() {
        Div demo = new Div();
        demo.setId("dynamic-demo");

        String[] imageSources = {
            "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='200' height='150'%3E%3Crect width='200' height='150' fill='%234CAF50'/%3E%3Ctext x='100' y='80' text-anchor='middle' fill='white' font-family='Arial' font-size='16'%3EImage 1%3C/text%3E%3C/svg%3E",
            "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='200' height='150'%3E%3Crect width='200' height='150' fill='%232196F3'/%3E%3Ctext x='100' y='80' text-anchor='middle' fill='white' font-family='Arial' font-size='16'%3EImage 2%3C/text%3E%3C/svg%3E",
            "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='200' height='150'%3E%3Crect width='200' height='150' fill='%23FF9800'/%3E%3Ctext x='100' y='80' text-anchor='middle' fill='white' font-family='Arial' font-size='16'%3EImage 3%3C/text%3E%3C/svg%3E"
        };

        Image dynamicImage = new Image(imageSources[0], "Dynamic image");
        dynamicImage.setId("dynamic-image");
        dynamicImage.getStyle()
            .set("display", "block")
            .set("border-radius", "8px")
            .set("margin-bottom", "15px");

        Div buttons = new Div();
        buttons.getStyle().set("display", "flex").set("gap", "10px");

        for (int i = 0; i < imageSources.length; i++) {
            final int index = i;
            NativeButton btn = new NativeButton("Show Image " + (i + 1), e -> {
                dynamicImage.setSrc(imageSources[index]);
                dynamicImage.setAlt("Dynamic image " + (index + 1));
            });
            btn.setId("show-image-" + (i + 1));
            buttons.add(btn);
        }

        demo.add(dynamicImage, buttons);
        return demo;
    }
}
