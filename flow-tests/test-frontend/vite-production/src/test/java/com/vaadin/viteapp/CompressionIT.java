package com.vaadin.viteapp;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.brotli.dec.BrotliInputStream;
import org.junit.Assert;
import org.junit.Test;

public class CompressionIT {

    private String getRootURL() {
        return "http://localhost:8888";
    }

    @Test
    public void resourcesAvailableAsUncompressed() throws Exception {
        String bundleName = getJsBundleName();

        String file = IOUtils.toString(new URL(getRootURL() + bundleName),
                StandardCharsets.UTF_8);
        Assert.assertTrue(file.contains("generated-flow-imports"));
    }

    @Test
    public void resourcesAvailableAsBrotli() throws Exception {
        String bundleName = getJsBundleName();

        URL compressedUrl = new URL(getRootURL() + bundleName + ".br");
        BrotliInputStream stream = new BrotliInputStream(
                compressedUrl.openStream());

        String file = IOUtils.toString(stream, StandardCharsets.UTF_8);
        Assert.assertTrue(file.contains("generated-flow-imports"));
    }

    private String getJsBundleName() throws Exception {
        String indexHtml = IOUtils.toString(
                new URL(getRootURL() + "/index.html"), StandardCharsets.UTF_8);
        Pattern p = Pattern.compile(".* src=\"./VAADIN/build/([^\"]*).*",
                Pattern.DOTALL);

        Matcher m = p.matcher(indexHtml);
        if (!m.matches()) {
            throw new IllegalStateException("No script found");
        }
        return "/VAADIN/build/" + m.group(1);
    }

}
