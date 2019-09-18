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
package com.vaadin.flow.linker;

import java.io.IOException;
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
import com.google.gwt.core.ext.linker.impl.ResourceInjectionUtil;
import com.google.gwt.core.linker.CrossSiteIframeLinker;
import com.google.gwt.core.linker.SingleScriptLinker;
import com.google.gwt.dev.About;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.util.tools.Utility;

/**
 * Customized version of {@link SingleScriptLinker} which uses a modified
 * version of the single script template ({@value #SINGLE_SCRIPT_TEMPLATE_JS}).
 * This is because the template from {@link SingleScriptLinker} uses a
 * computeScriptBase function which does a <code>document.write</code> and thus
 * cannot be called deferredly.
 *
 * @see SingleScriptLinker
 * @since 1.0
 */
@LinkerOrder(Order.PRIMARY)
@Shardable
public class ClientEngineLinker extends SingleScriptLinker {

    /**
     * The customized version of the single script template.
     */
    private static final String SINGLE_SCRIPT_TEMPLATE_JS = "com/vaadin/flow/linker/ClientEngineSingleScriptTemplate.js";
    /**
     * The computeScriptBase implementation which doesn't use document.write. It
     * is taken from {@link CrossSiteIframeLinker}.
     */
    private static final String COMPUTE_SCRIPT_BASE_DOT_JS = "com/google/gwt/core/ext/linker/impl/computeScriptBase.js";

    @Override
    public String getDescription() {
        return "Flow Custom Single Script Linker";
    }

    /*
     * Overridden because parent implementation is private and we need it here.
     */
    @Transferable
    private static class Script extends Artifact<Script> {
        private final String javaScript;
        private final String strongName;

        protected Script(String strongName, String javaScript) {
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

    /*
     * (non-Javadoc)
     *
     * @see com.google.gwt.core.linker.SingleScriptLinker#doEmitCompilation(com.
     * google.gwt.core.ext.TreeLogger, com.google.gwt.core.ext.LinkerContext,
     * com.google.gwt.core.ext.linker.CompilationResult,
     * com.google.gwt.core.ext.linker.ArtifactSet)
     *
     * Overridden to avoid exception during compilation from SingleScriptLinker.
     */
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

        ArrayList<Artifact<?>> toReturn = new ArrayList<>();
        toReturn.add(new Script(result.getStrongName(), js[0]));
        toReturn.addAll(
                emitSelectionInformation(result.getStrongName(), result));
        return toReturn;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.google.gwt.core.linker.SingleScriptLinker#emitSelectionScript(com.
     * google.gwt.core.ext.TreeLogger, com.google.gwt.core.ext.LinkerContext,
     * com.google.gwt.core.ext.linker.ArtifactSet)
     *
     * Overridden because of customized client engine file name.
     */
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

    /*
     * (non-Javadoc)
     *
     * @see com.google.gwt.core.ext.linker.impl.SelectionScriptLinker#
     * fillSelectionScriptTemplate(java.lang.StringBuffer,
     * com.google.gwt.core.ext.TreeLogger,
     * com.google.gwt.core.ext.LinkerContext,
     * com.google.gwt.core.ext.linker.ArtifactSet,
     * com.google.gwt.core.ext.linker.CompilationResult)
     *
     * Overridden because need to use same compute base script as in
     * CrossSiteIframeLinker, ClientEngineLinker.COMPUTE_SCRIPT_BASE_DOT_JS. It
     * doesn't use document.write.
     */
    @Override
    protected String fillSelectionScriptTemplate(StringBuffer selectionScript,
            TreeLogger logger, LinkerContext context, ArtifactSet artifacts,
            CompilationResult result) throws UnableToCompleteException {
        String computeScriptBase;
        String processMetas;
        try {
            computeScriptBase = Utility
                    .getFileFromClassPath(COMPUTE_SCRIPT_BASE_DOT_JS);
            processMetas = Utility.getFileFromClassPath(PROCESS_METAS_JS);
        } catch (IOException e) {
            logger.log(TreeLogger.ERROR,
                    "Unable to read selection script template", e);
            throw new UnableToCompleteException();
        }
        replaceAll(selectionScript, "__COMPUTE_SCRIPT_BASE__",
                computeScriptBase);

        replaceAll(selectionScript, "__PROCESS_METAS__", processMetas);

        ResourceInjectionUtil.injectResources(selectionScript, artifacts);
        permutationsUtil.addPermutationsJs(selectionScript, logger, context);

        replaceAll(selectionScript, "__MODULE_FUNC__",
                context.getModuleFunctionName());
        replaceAll(selectionScript, "__MODULE_NAME__", context.getModuleName());
        replaceAll(selectionScript, "__HOSTED_FILENAME__", getHostedFilename());

        return selectionScript.toString();
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
        return results.iterator().next();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.google.gwt.core.linker.SingleScriptLinker#getSelectionScriptTemplate(
     * com.google.gwt.core.ext.TreeLogger,
     * com.google.gwt.core.ext.LinkerContext)
     *
     * Overridden because we use a customized single script template.
     */
    @Override
    protected String getSelectionScriptTemplate(TreeLogger logger,
            LinkerContext context) throws UnableToCompleteException {
        return SINGLE_SCRIPT_TEMPLATE_JS;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.google.gwt.core.ext.linker.impl.SelectionScriptLinker#link(com.google
     * .gwt.core.ext.TreeLogger, com.google.gwt.core.ext.LinkerContext,
     * com.google.gwt.core.ext.linker.ArtifactSet, boolean)
     *
     * Overridden so that we can provide compule.properties with a property
     * mapping to the client engine javascript file name.
     */
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

    /*
     * Provides the property file that maps the client engine javascript file.
     */
    private Artifact<?> emitStrongNamePropertyFile(TreeLogger logger,
            LinkerContext context, ArtifactSet artifacts)
                    throws UnableToCompleteException {
        Script result = getScript(logger, artifacts);

        String contents = "jsFile=" + getJsFilename(context, result);
        return emitString(logger, contents, "compile.properties");
    }
}
