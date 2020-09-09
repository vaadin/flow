package com.vaadin.flow.uitest.ui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;

import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.frontend.FrontendUtils;

public class FrontendTestUtil {

    public static void writeToFrontendFile(VaadinService vaadinService,
            String text) {
        final String projectFrontendDir = FrontendUtils.getProjectFrontendDir(
                vaadinService.getDeploymentConfiguration());
        File styleFile = new File(projectFrontendDir, "styles.css");
        try {
            BufferedWriter out = null;
            try {
                FileWriter fstream = new FileWriter(styleFile);
                out = new BufferedWriter(fstream);
                out.write(text);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
