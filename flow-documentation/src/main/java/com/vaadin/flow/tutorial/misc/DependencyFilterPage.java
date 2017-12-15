package com.vaadin.flow.tutorial.misc;

import java.util.List;

import com.vaadin.flow.server.DependencyFilter;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.shared.ui.Dependency;
import com.vaadin.shared.ui.LoadMode;

@CodeFor("miscellaneous/tutorial-dependency-filter.asciidoc")
public class DependencyFilterPage {

    public class BundleFilter implements DependencyFilter {
        @Override
        public List<Dependency> filter(List<Dependency> dependencies,
                FilterContext filterContext) {

            if (filterContext.getService().getDeploymentConfiguration()
                    .isProductionMode()) {
                dependencies.clear();
                dependencies.add(new Dependency(Dependency.Type.HTML_IMPORT,
                        "my-bundle.html", LoadMode.EAGER));
            }

            return dependencies;
        }
    }

}
