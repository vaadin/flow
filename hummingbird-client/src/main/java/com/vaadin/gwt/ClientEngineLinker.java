/*
 * Copyright 2008 Google Inc.
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
package com.vaadin.gwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.CompilationResult;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.linker.Shardable;
import com.google.gwt.core.ext.linker.Transferable;
import com.google.gwt.core.ext.linker.impl.SelectionScriptLinker;
import com.google.gwt.dev.About;
import com.google.gwt.dev.util.DefaultTextOutput;

/**
 * A Linker for producing a single, cacheable JavaScript file from a GWT module.
 *
 * Emits a compile.properties which contains the name of the javascript file
 * using a "jsFile" key
 *
 *
 *
 * The use of this Linker requires that the module has exactly one distinct
 * compilation result.
 */
@LinkerOrder(Order.PRIMARY)
@Shardable
public class ClientEngineLinker extends SelectionScriptLinker {
    @Override
    public String getDescription() {
        return "Single Script";
    }

    @Transferable
    private static class Script extends Artifact<Script> {
        private final String javaScript;
        private final String strongName;

        public Script(String strongName, String javaScript) {
            super(ClientEngineLinker.class);
            this.strongName = strongName;
            this.javaScript = javaScript;
        }

        @Override
        public int compareToComparableArtifact(Script that) {
            int res = strongName.compareTo(that.strongName);
            if (res == 0) {
                res = javaScript.compareTo(that.javaScript);
            }
            return res;
        }

        @Override
        public Class<Script> getComparableArtifactType() {
            return Script.class;
        }

        public String getJavaScript() {
            return javaScript;
        }

        public String getStrongName() {
            return strongName;
        }

        @Override
        public int hashCode() {
            return strongName.hashCode() ^ javaScript.hashCode();
        }

        @Override
        public String toString() {
            return "Script " + strongName;
        }
    }

    @Override
    protected Collection<Artifact<?>> doEmitCompilation(TreeLogger logger,
            LinkerContext context, CompilationResult result,
            ArtifactSet artifacts) throws UnableToCompleteException {

        String[] js = result.getJavaScript();
        if (js.length != 1) {
            logger.branch(TreeLogger.ERROR,
                    "The module must not have multiple fragments when using the "
                            + getDescription() + " Linker.",
                    null);
            throw new UnableToCompleteException();
        }

        Collection<Artifact<?>> toReturn = new ArrayList<Artifact<?>>();
        toReturn.add(new Script(result.getStrongName(), js[0]));
        toReturn.addAll(
                emitSelectionInformation(result.getStrongName(), result));
        return toReturn;
    }

    @Override
    protected EmittedArtifact emitSelectionScript(TreeLogger logger,
            LinkerContext context, ArtifactSet artifacts)
                    throws UnableToCompleteException {

        // Find the single Script result
        Script result = getScript(logger, artifacts);

        DefaultTextOutput out = new DefaultTextOutput(true);

        // Emit the selection script.
        String bootstrap = generateSelectionScript(logger, context, artifacts);
        bootstrap = context.optimizeJavaScript(logger, bootstrap);
        out.print(bootstrap);
        out.newlineOpt();

        // Emit the module's JS a closure.
        out.print("(function () {");
        out.newlineOpt();
        out.print("var $gwt_version = \"" + About.getGwtVersionNum() + "\";");
        out.newlineOpt();
        out.print("var $wnd = window;");
        out.newlineOpt();
        out.print("var $doc = $wnd.document;");
        out.newlineOpt();
        out.print("var $moduleName, $moduleBase;");
        out.newlineOpt();
        out.print(
                "var $stats = $wnd.__gwtStatsEvent ? function(a) {$wnd.__gwtStatsEvent(a)} : null;");
        out.newlineOpt();

        out.print("var $strongName = '" + result.getStrongName() + "';");
        out.newlineOpt();

        out.print(result.getJavaScript());

        // Generate the call to tell the bootstrap code that we're ready to go.
        out.newlineOpt();
        out.print("if (" + context.getModuleFunctionName() + ") "
                + context.getModuleFunctionName()
                + ".onScriptLoad(gwtOnLoad);");
        out.newlineOpt();
        out.print("})();");
        out.newlineOpt();

        return emitString(logger, out.toString(),
                getJsFilename(context, result));
    }

    private String getJsFilename(LinkerContext context, Script result) {
        return context.getModuleName() + "-" + result.getStrongName()
                + ".cache.js";
    }

    private Script getScript(TreeLogger logger, ArtifactSet artifacts)
            throws UnableToCompleteException {
        Set<Script> results = artifacts.find(Script.class);
        if (results.size() != 1) {
            logger.log(TreeLogger.ERROR,
                    "The module must have exactly one distinct"
                            + " permutation when using the " + getDescription()
                            + " Linker; found " + results.size(),
                    null);
            throw new UnableToCompleteException();
        }
        Script result = results.iterator().next();
        return result;
    }

    /**
     * Unimplemented. Normally required by
     * {@link #doEmitCompilation(TreeLogger, LinkerContext, CompilationResult, ArtifactSet)}
     * .
     */
    @Override
    protected String getCompilationExtension(TreeLogger logger,
            LinkerContext context) throws UnableToCompleteException {
        throw new UnableToCompleteException();
    }

    /**
     * Unimplemented. Normally required by
     * {@link #doEmitCompilation(TreeLogger, LinkerContext, CompilationResult, ArtifactSet)}
     * .
     */
    @Override
    protected String getModulePrefix(TreeLogger logger, LinkerContext context,
            String strongName) throws UnableToCompleteException {
        throw new UnableToCompleteException();
    }

    @Override
    protected String getSelectionScriptTemplate(TreeLogger logger,
            LinkerContext context) throws UnableToCompleteException {
        return "com/google/gwt/core/linker/SingleScriptTemplate.js";
    }

    @Override
    public ArtifactSet link(TreeLogger logger, LinkerContext context,
            ArtifactSet artifacts, boolean onePermutation)
                    throws UnableToCompleteException {
        ArtifactSet result = super.link(logger, context, artifacts,
                onePermutation);
        if (!onePermutation) {
            result.add(emitStrongNamePropertyFile(logger, context, artifacts));
        }
        return result;
    }

    public static class StrongNamePropertyFile
            extends Artifact<StrongNamePropertyFile> {

        private String strongName;

        public StrongNamePropertyFile(String strongName) {
            super(ClientEngineLinker.class);
            this.strongName = strongName;
        }

        @Override
        public int hashCode() {
            return strongName.hashCode();
        }

        @Override
        protected int compareToComparableArtifact(StrongNamePropertyFile o) {
            return strongName.compareTo(o.strongName);
        }

        @Override
        protected Class<StrongNamePropertyFile> getComparableArtifactType() {
            return StrongNamePropertyFile.class;
        }

    }

    private Artifact<?> emitStrongNamePropertyFile(TreeLogger logger,
            LinkerContext context, ArtifactSet artifacts)
                    throws UnableToCompleteException {
        Script result = getScript(logger, artifacts);

        String contents = "jsFile=" + getJsFilename(context, result);
        return emitString(logger, contents, "compile.properties");

        // return new StrongNamePropertyFile(result.getStrongName());
    }
}
