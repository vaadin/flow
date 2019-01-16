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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ImageElementIT extends ChromeBrowserTest {

    private ImageElement image1;
    private ImageElement image2;
    private ImageElement image3;
    private DivElement log;

    @Before
    public void open() {
        getDriver().get("http://localhost:8888/Image");
        image1 = $(ImageElement.class).id("image1");
        image2 = $(ImageElement.class).id("image2");
        image3 = $(ImageElement.class).id("image3");
        log = $(DivElement.class).id("log");
    }

    @Test
    public void clickImage1() {
        image1.click();
        Assert.assertEquals("Clicked image1", log.getText());
    }

    @Test
    public void clickImage2() {
        image2.click();
        Assert.assertEquals("Clicked image2", log.getText());
    }

    @Test
    public void clickImage3() {
        image3.click();
        Assert.assertEquals("Clicked image3", log.getText());
    }
}
