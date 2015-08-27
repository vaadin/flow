package hummingbird.component;

import com.vaadin.server.ClassResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.tests.components.AbstractTestUIWithLog;
import com.vaadin.ui.Button;
import com.vaadin.ui.FileDownloader;

public class FileDownloaderUI extends AbstractTestUIWithLog {

    @Override
    protected void setup(VaadinRequest request) {
        Button button = new Button("Download excel file");
        button.addClickListener(e -> {
            log("Download clicked");
        });
        new FileDownloader(new ClassResource("excel.xlsx")).attach(button);
        add(button);
    }

}
