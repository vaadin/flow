/*
 *  ------------------------------------------------------------------------
 *                                 Copyright ©
 *
 *               DST Financial Services International Ltd 2019
 *                            All rights reserved.
 *
 *
 *  No part of this document may be reproduced, stored in a retrieval
 *  system, or transmitted, in any form or by any means, electronic,
 *  mechanical, photocopying, networking or otherwise, without the prior
 *  written permission of DST Financial Services International Ltd.
 *
 *  The computer system, procedures, databases, code & programs created &
 *  maintained by DST Financial Services International Ltd are proprietary
 *  in nature & as such are confidential. Any unauthorised use, misuse, or
 *  disclosure of such items or information may result in civil liabilities
 *  & may be subject to criminal penalties under the applicable laws.
 *  ------------------------------------------------------------------------
 */

package com.vaadin.flow.component.html.testbench;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

@Route("Image")
public class ImageView extends Div {

    public ImageView() {
        Div log = new Div();
        log.setId("log");

        Image image1 = new Image("frontend/images/0.png", "logo");
        image1.setId("image1");
        image1.addClickListener(clickEvent -> {
            log.setText("Clicked image1");
        });
        Image image2 = new Image("frontend/images/0.png", "logo", e -> {
            log.setText("Clicked image2");
        });
        image2.setId("image2");
        Image image3 = new Image(new StreamResource("image.svg", () -> getImageInputStream()), "logo", e -> {
            log.setText("Clicked image3");
        });
        image3.setId("image3");
        add(log, image1, image2, image3);
    }

    private InputStream getImageInputStream()
    {
        String svg = "<?xml version='1.0' encoding='UTF-8' standalone='no'?>"
                + "<svg  xmlns='http://www.w3.org/2000/svg' "
                + "xmlns:xlink='http://www.w3.org/1999/xlink'>"
                + "<rect x='10' y='10' height='100' width='100' "
                + "style=' fill: #90C3D4'/><text x='30' y='30' fill='red'>"
                + "Hello"
                + "</text>" + "</svg>";
        return new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8));

    }
}
