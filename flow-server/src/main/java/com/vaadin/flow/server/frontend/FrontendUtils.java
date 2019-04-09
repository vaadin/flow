package com.vaadin.flow.server.frontend;

import java.io.File;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;

public abstract class FrontendUtils {
    
    public static final String WEBAPP_FOLDER = "./";
    public static final String WEBPACK_CONFIG ="webpack.config.js";
    
    public static String getBaseDir() {
        return System.getProperty("project.basedir", System.getProperty("user.dir", "."));
    }
    
    public static boolean isBowerLegacyMode() {
        boolean hasNpmFrontend = new File(getBaseDir(), "frontend").isDirectory();
        boolean hasBowerFrontend = new File(getBaseDir(), "src/main/webapp/frontend").isDirectory();
        boolean hasPackage = new File(getBaseDir(), PACKAGE_JSON).exists();
        boolean hasWebpackConfig = new File(FrontendUtils.WEBPACK_CONFIG).exists();

        return (!hasBowerFrontend || hasNpmFrontend || hasPackage && hasWebpackConfig) ? false :true;
    }

}
