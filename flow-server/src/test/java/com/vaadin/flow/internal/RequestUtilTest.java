package com.vaadin.flow.internal;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class RequestUtilTest {

    @Test
    public void httpRequestAcceptsBrotli_acceptsBrotliResourceReturnsTrue() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("Accept-Encoding")).thenReturn("deflate, br");

        Assert.assertTrue("Brotli resource request should be accepted.",
                RequestUtil.acceptsBrotliResource(request));
    }
    @Test
    public void httpRequestDoesntAcceptBrotli_acceptsBrotliResourceReturnsFalse() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("Accept-Encoding")).thenReturn("deflate, gzip");

        Assert.assertFalse("Brotli resource request should not be accepted.",
                RequestUtil.acceptsBrotliResource(request));
    }

    @Test
    public void httpRequestAcceptsGzip_acceptsGzippedResourceReturnsTrue() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("Accept-Encoding")).thenReturn("deflate, gzip");

        Assert.assertTrue("Gzipped resource request should be accepted.",
                RequestUtil.acceptsGzippedResource(request));
    }

    @Test
    public void httpRequestDoesntAcceptGzip_acceptsGzippedResourceReturnsFalse() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("Accept-Encoding")).thenReturn("deflate, br");

        Assert.assertFalse("Gzipped resource request should not be accepted.",
                RequestUtil.acceptsGzippedResource(request));
    }

    @Test
    public void acceptsEncoding_acceptsWeightedEncoding() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("Accept-Encoding")).thenReturn("deflate, gzip;q=1.0, *;q=0.5");

        Assert.assertTrue("Gzipped resource request should be accepted.",
                RequestUtil.acceptsGzippedResource(request));
        Assert.assertTrue("Brotli resource request should be accepted.",
                RequestUtil.acceptsBrotliResource(request));
        Assert.assertTrue(RequestUtil.acceptsEncoding(request, "deflate"));
    }

    @Test
    public void acceptsEncoding_doesntAcceptEncodingWithZeroWeight() {

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader("Accept-Encoding")).thenReturn("deflate, gzip;q=0, *;q=0.5");

        Assert.assertFalse("Gzipped resource quality value 0 should not be accepted.",
                RequestUtil.acceptsGzippedResource(request));
        Assert.assertTrue("Brotli resource request should be accepted due to '*;q=0.5'",
                RequestUtil.acceptsBrotliResource(request));
        Assert.assertTrue(RequestUtil.acceptsEncoding(request, "deflate"));

        Mockito.when(request.getHeader("Accept-Encoding")).thenReturn("deflate, gzip;q=0");
        Assert.assertFalse("Gzipped resource quality value 0 should not be accepted.",
                RequestUtil.acceptsGzippedResource(request));

        Mockito.when(request.getHeader("Accept-Encoding")).thenReturn("deflate, gzip;q=0.0");
        Assert.assertFalse("Gzipped resource quality value 0.0 should not be accepted.",
                RequestUtil.acceptsGzippedResource(request));

        Mockito.when(request.getHeader("Accept-Encoding")).thenReturn("deflate, gzip;q=0.00");
        Assert.assertFalse("Gzipped resource quality value 0.00 should not be accepted.",
                RequestUtil.acceptsGzippedResource(request));

        Mockito.when(request.getHeader("Accept-Encoding")).thenReturn("deflate, gzip;q=0.000");
        Assert.assertFalse("Gzipped resource quality value 0.000 should not be accepted.",
                RequestUtil.acceptsGzippedResource(request));

        Mockito.when(request.getHeader("Accept-Encoding")).thenReturn("deflate, gzip;q=0.0001");
        Assert.assertTrue("Gzipped resource quality value 0.0001 request should be accepted.",
                RequestUtil.acceptsGzippedResource(request));
    }

}