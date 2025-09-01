/*
 * Copyright 2000-2025 Vaadin Ltd.
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

package com.vaadin.flow.server.startup;

import java.util.LinkedHashSet;
import java.util.Set;

import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.Pair;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.communication.IndexHtmlRequestListener;
import com.vaadin.flow.server.communication.IndexHtmlResponse;
import com.vaadin.pro.licensechecker.BuildType;
import com.vaadin.pro.licensechecker.Capabilities;
import com.vaadin.pro.licensechecker.Capability;
import com.vaadin.pro.licensechecker.LicenseChecker;
import com.vaadin.pro.licensechecker.MissingLicenseKeyException;

/**
 * Abstract base class implementing the {@link VaadinServiceInitListener} for
 * initializing a license checking mechanism during the service initialization.
 * <p>
 * This class handles the validation of the license for a specific product and
 * its version during the initialization of the Vaadin service. The license
 * checking mechanism is performed only in development mode and in different
 * modes depending on the availability of Vaadin dev tools.
 * <p>
 * With Vaadin dev tools enabled, if the license check fails because of missing
 * license keys, handling is delegated to the Vaadin Dev Server so it can, for
 * example, display the pre-trial splash screen. However, if dev tools are
 * disabled, the License Checker will open the vaadin.com "Validate license"
 * page in a browser window to let the user log in or register and then try to
 * download a valid license.
 * <p>
 * Subclasses are expected to provide the product name and version required for
 * the license validation by invoking the constructor of this class and to
 * properly register the implementation for runtime discovery.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @see VaadinServiceInitListener
 */
public abstract class BaseLicenseCheckerServiceInitListener
        implements VaadinServiceInitListener {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(BaseLicenseCheckerServiceInitListener.class);

    private final String productName;
    private final String productVersion;

    protected BaseLicenseCheckerServiceInitListener(String productName,
            String productVersion) {
        this.productName = productName;
        this.productVersion = productVersion;
    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        var service = event.getSource();
        var configuration = service.getDeploymentConfiguration();
        if (!configuration.isProductionMode()) {
            // Using a null BuildType to allow trial licensing builds,
            // The variable is defined to avoid method signature ambiguity
            BuildType buildType = null;

            if (configuration.isDevToolsEnabled()) {
                // If dev tools are enabled, do a check and then delegate
                // handling of a missing key to dev tools to show the splash
                // screen
                // Any other license error results in an immediate failure
                try {
                    LicenseChecker.checkLicense(productName, productVersion,
                            buildType, url -> {
                                // this callback is called only if no local
                                // license keys are detected
                                throw new MissingLicenseKeyException(
                                        "No license key present");
                            }, 0, Capabilities.of(Capability.PRE_TRIAL));
                } catch (MissingLicenseKeyException ex) {
                    LOGGER.debug(
                            "Missing license key for {} {} will be handled by Vaadin Dev Server",
                            productName, productVersion);
                    // instruct dev tools to check license at runtime
                    CheckProductLicense.register(event, productName,
                            productVersion);
                }
            } else {
                // Fallback to online validation waiting for license key
                // download
                LicenseChecker.checkLicense(productName, productVersion,
                        Capabilities.of(Capability.PRE_TRIAL), buildType);
            }
        }
    }

    /**
     * A {@link IndexHtmlRequestListener} implementation used to modify the
     * Vaadin runtime generated {@literal index.html} to inject additional
     * product-specific license information for Vaadin Dev Tools license
     * checking.
     */
    private static final class CheckProductLicense
            implements IndexHtmlRequestListener {

        private final Set<Pair<String, String>> products = new LinkedHashSet<>();

        private CheckProductLicense() {
        }

        static void register(ServiceInitEvent event, String productName,
                String productVersion) {
            event.getAddedIndexHtmlRequestListeners()
                    .filter(CheckProductLicense.class::isInstance)
                    .map(CheckProductLicense.class::cast).findFirst()
                    .orElseGet(() -> {
                        var listener = new CheckProductLicense();
                        event.addIndexHtmlRequestListener(listener);
                        return listener;
                    }).addProduct(productName, productVersion);
        }

        private void addProduct(String productName, String productVersion) {
            this.products.add(new Pair<>(productName, productVersion));
        }

        @Override
        public void modifyIndexHtmlResponse(
                IndexHtmlResponse indexHtmlResponse) {

            StringBuilder script = new StringBuilder(
                    """
                            window.Vaadin = window.Vaadin || {};
                            window.Vaadin.devTools = window.Vaadin.devTools || {};
                            window.Vaadin.devTools.createdCvdlElements =
                            window.Vaadin.devTools.createdCvdlElements || [];

                            const registerProduct = function(productName,productVersion) {
                                const product = {};
                                product.constructor['cvdlName'] = productName;
                                product.constructor['version'] = productVersion
                                product.tagName = `--${productName}`;
                                window.Vaadin.devTools.createdCvdlElements.push(product);
                            };
                            """);
            products.forEach(product -> script.append(
                    "registerProduct('%s','%s');".formatted(product.getFirst(),
                            product.getSecond())));
            Document document = indexHtmlResponse.getDocument();
            Element elm = new Element("script");
            elm.attr("initial", "");
            elm.appendChild(new DataNode(script.toString()));
            document.head().insertChildren(0, elm);
        }
    }

}
