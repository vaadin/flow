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
package com.vaadin.flow.server;

import org.junit.Assert;
import org.junit.Test;

public class FrontendDependencyUrlResolverTest {

    @Test
    public void resolveToContextRoot_nullOrBlank_returnsNull() {
        Assert.assertNull(
                FrontendDependencyUrlResolver.resolveToContextRoot(null));
        Assert.assertNull(
                FrontendDependencyUrlResolver.resolveToContextRoot(""));
        Assert.assertNull(
                FrontendDependencyUrlResolver.resolveToContextRoot("   "));
    }

    @Test
    public void resolveToContextRoot_pathTraversal_returnsNull() {
        Assert.assertNull(FrontendDependencyUrlResolver
                .resolveToContextRoot("../foo.css"));
        Assert.assertNull(FrontendDependencyUrlResolver
                .resolveToContextRoot("foo/../bar.css"));
    }

    @Test
    public void resolveToContextRoot_externalUrls_unchanged() {
        Assert.assertEquals("http://cdn/x.css", FrontendDependencyUrlResolver
                .resolveToContextRoot("http://cdn/x.css"));
        Assert.assertEquals("https://cdn/x.css", FrontendDependencyUrlResolver
                .resolveToContextRoot("https://cdn/x.css"));
        Assert.assertEquals("//cdn/x.css", FrontendDependencyUrlResolver
                .resolveToContextRoot("//cdn/x.css"));
    }

    @Test
    public void resolveToContextRoot_explicitProtocols_unchanged() {
        Assert.assertEquals("context://foo.css", FrontendDependencyUrlResolver
                .resolveToContextRoot("context://foo.css"));
        Assert.assertEquals("base://foo.css", FrontendDependencyUrlResolver
                .resolveToContextRoot("base://foo.css"));
    }

    @Test
    public void resolveToContextRoot_absoluteServerPath_unchanged() {
        Assert.assertEquals("/assets/foo.css", FrontendDependencyUrlResolver
                .resolveToContextRoot("/assets/foo.css"));
    }

    @Test
    public void resolveToContextRoot_dotSlashRelative_strippedAndPrefixed() {
        Assert.assertEquals("context://foo.css", FrontendDependencyUrlResolver
                .resolveToContextRoot("./foo.css"));
    }

    @Test
    public void resolveToContextRoot_bareRelative_prefixedWithContext() {
        Assert.assertEquals("context://foo.css",
                FrontendDependencyUrlResolver.resolveToContextRoot("foo.css"));
        Assert.assertEquals("context://styles/foo.css",
                FrontendDependencyUrlResolver
                        .resolveToContextRoot("styles/foo.css"));
    }

    @Test
    public void resolveToContextRoot_trimsWhitespace() {
        Assert.assertEquals("context://foo.css", FrontendDependencyUrlResolver
                .resolveToContextRoot("  foo.css  "));
    }

    @Test
    public void isRuntimeDependencyUrl_runtimePrefixes_true() {
        Assert.assertTrue(FrontendDependencyUrlResolver
                .isRuntimeDependencyUrl("context://foo.js"));
        Assert.assertTrue(FrontendDependencyUrlResolver
                .isRuntimeDependencyUrl("base://foo.js"));
        Assert.assertTrue(FrontendDependencyUrlResolver
                .isRuntimeDependencyUrl("http://cdn/foo.js"));
        Assert.assertTrue(FrontendDependencyUrlResolver
                .isRuntimeDependencyUrl("https://cdn/foo.js"));
        Assert.assertTrue(FrontendDependencyUrlResolver
                .isRuntimeDependencyUrl("//cdn/foo.js"));
        Assert.assertTrue(FrontendDependencyUrlResolver
                .isRuntimeDependencyUrl("/assets/foo.js"));
    }

    @Test
    public void isRuntimeDependencyUrl_bareRelativeOrBundlerSpecifier_false() {
        Assert.assertFalse(
                FrontendDependencyUrlResolver.isRuntimeDependencyUrl("foo.js"));
        Assert.assertFalse(FrontendDependencyUrlResolver
                .isRuntimeDependencyUrl("./foo.js"));
        Assert.assertFalse(FrontendDependencyUrlResolver
                .isRuntimeDependencyUrl("../foo.js"));
        Assert.assertFalse(FrontendDependencyUrlResolver
                .isRuntimeDependencyUrl("@scope/pkg/foo.js"));
    }

    @Test
    public void isRuntimeDependencyUrl_nullOrBlank_false() {
        Assert.assertFalse(
                FrontendDependencyUrlResolver.isRuntimeDependencyUrl(null));
        Assert.assertFalse(
                FrontendDependencyUrlResolver.isRuntimeDependencyUrl(""));
        Assert.assertFalse(
                FrontendDependencyUrlResolver.isRuntimeDependencyUrl("   "));
    }
}
