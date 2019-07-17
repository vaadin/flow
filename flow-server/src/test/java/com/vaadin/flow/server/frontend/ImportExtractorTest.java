/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.server.frontend;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ImportExtractorTest {

    @Test
    public void removeComments_blockCommentsAreRemoved() {
        ImportExtractor extractor = new ImportExtractor(
                "/* comment \n sdf \n \n */import 'foo.js';");

        Assert.assertEquals("import 'foo.js';", extractor.removeComments());
    }

    @Test
    public void removeComments_lineCommentsAreRemoved() {
        ImportExtractor extractor = new ImportExtractor(
                "// sdfdsf \nimport from 'foo.js';\n //xxxxx \nimport {A}  from bar.js;");
        Assert.assertEquals(
                "\nimport from 'foo.js';\n" + " \n"
                        + "import {A}  from bar.js;",
                extractor.removeComments());
    }

    @Test
    public void removeComments_blockCommentInsideImport() {
        ImportExtractor extractor = new ImportExtractor(
                "import from /*fdg \n */'foo.js';");
        Assert.assertEquals("import from 'foo.js';",
                extractor.removeComments());
    }

    @Test
    public void removeComments_lineCommentInsideImport() {
        ImportExtractor extractor = new ImportExtractor(
                "import from // xcvxcvcx\n//vcbcvbcv\n 'foo.js';");
        Assert.assertEquals("import from \n\n 'foo.js';",
                extractor.removeComments());
    }

    @Test
    public void getImportsWithBlockComment() {
        ImportExtractor extractor = new ImportExtractor(
                "/* comment \n sdf \n \n */ import /* ddddddd*/'foo.js';");
        List<String> importedPaths = extractor.getImportedPaths();
        Assert.assertEquals(1, importedPaths.size());
        Assert.assertEquals("foo.js", importedPaths.get(0));
    }

    @Test
    public void getImportsWithLineComments() {
        ImportExtractor extractor = new ImportExtractor(
                "// sdfdsf \n  import from 'foo.js';\n //xxxxx \n import {A} // sdfsf\n from bar.js;");
        List<String> importedPaths = extractor.getImportedPaths();
        Assert.assertEquals(2, importedPaths.size());
        Assert.assertEquals("foo.js", importedPaths.get(0));
        Assert.assertEquals("bar.js", importedPaths.get(1));
    }

    @Test
    public void getImport_lineHasNoSemicolon() {
        ImportExtractor extractor = new ImportExtractor(
                "import {A} from 'foo.js'; import from 'bar.js'\n import from 'baz.js'");
        List<String> importedPaths = extractor.getImportedPaths();
        Assert.assertEquals(3, importedPaths.size());
        Assert.assertEquals("foo.js", importedPaths.get(0));
        Assert.assertEquals("bar.js", importedPaths.get(1));
        Assert.assertEquals("baz.js", importedPaths.get(2));
    }

    @Test
    public void getImports_thereAreNoImportsAtAll_noImportsFound() {
        ImportExtractor extractor = new ImportExtractor(
                "const container = document.createElement('template');\n" + "\n"
                        + "        container.innerHTML = `\n"
                        + "            <dom-module id=\"gui-styles\">\n"
                        + "                <template>\n"
                        + "                    <style>\n"
                        + "                        @import url('https://fonts.googleapis.com/css?family=Montserrat:700');\n"
                        + "                    </style>\n"
                        + "                </template>\n"
                        + "            </dom-module>`;\n"
                        + "        document.head.appendChild(container.content);");

        Assert.assertEquals(0, extractor.getImportedPaths().size());
    }

    @Test
    public void getImports_onlyImportAsFirstStatementsAreCounted() {
        ImportExtractor extractor = new ImportExtractor(
                "import {A} from 'foo.js'; \n some text  \n  import from 'ignored.js';");

        List<String> importedPaths = extractor.getImportedPaths();
        Assert.assertEquals(1, importedPaths.size());
        Assert.assertEquals("foo.js", importedPaths.get(0));
    }
}
