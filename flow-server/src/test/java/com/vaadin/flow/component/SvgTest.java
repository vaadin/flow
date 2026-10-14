/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

public class SvgTest {

    @Test
    public void attachedToElement() {
        // This will throw an assertion error if the element is not attached to
        // the component
        new Svg("<svg></svg>").getParent();
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullStream() {
        new Svg((InputStream) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void text() {
        new Svg("hello");
    }

    static String TRIVIAL_SVG = """
            <svg>
                <circle cx="50" cy="50" r="40" stroke="green" stroke-width="4" fill="yellow" />
            </svg>""";

    static String TRIVIAL_SVG2 = """
            <svg height="140" width="500">
              <ellipse cx="200" cy="80" rx="100" ry="50"
              style="fill:yellow;stroke:purple;stroke-width:2" />
            </svg>""";

    static String SVG_WITH_DOCTYPE_ET_AL = """
            <?xml version="1.0" encoding="UTF-8" standalone="no"?>
            <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
            <svg width="391" height="391" viewBox="-70.5 -70.5 391 391" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
            <rect fill="#fff" stroke="#000" x="-70" y="-70" width="390" height="390"/>
            <g opacity="0.8">
                 <rect x="25" y="25" width="200" height="200" fill="lime" stroke-width="4" stroke="pink" />
                 <circle cx="125" cy="125" r="75" fill="orange" />
                 <polyline points="50,150 50,200 200,200 200,100" stroke="red" stroke-width="4" fill="none" />
                 <line x1="50" y1="50" x2="200" y2="200" stroke="blue" stroke-width="4" />
            </g>
            </svg>""";

    @Test
    public void simpleSvg() {
        Svg svg = new Svg(TRIVIAL_SVG);
        Assert.assertEquals(TRIVIAL_SVG, getSvgDocumentBody(svg));
    }

    @Test
    public void withDocType() {
        Svg svg = new Svg(SVG_WITH_DOCTYPE_ET_AL);
        Assert.assertTrue(getSvgDocumentBody(svg).startsWith("<svg"));
    }

    @Test
    public void resetSvg() {
        Svg svg = new Svg(TRIVIAL_SVG);
        Assert.assertEquals(TRIVIAL_SVG, getSvgDocumentBody(svg));
        svg.setSvg(TRIVIAL_SVG2);
        Assert.assertEquals(TRIVIAL_SVG2, getSvgDocumentBody(svg));
    }

    @Test
    public void fromStream() {
        Svg svg = new Svg(new ByteArrayInputStream(TRIVIAL_SVG.getBytes()));
        Assert.assertEquals(TRIVIAL_SVG, getSvgDocumentBody(svg));
    }

    private static String getSvgDocumentBody(Svg svg) {
        return svg.getElement().getProperty("innerHTML");
    }

}
