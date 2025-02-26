package com.vaadin.rewrite;

import org.openrewrite.NlsRewrite.Description;
import org.openrewrite.NlsRewrite.DisplayName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.JavadocVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Annotation;
import org.openrewrite.java.tree.J.ClassDeclaration;
import org.openrewrite.java.tree.J.Identifier;
import org.openrewrite.java.tree.J.MethodDeclaration;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Javadoc;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class FixMissingJavadocsForNewMethods extends Recipe {

    private String version;
    private Map<String, List<String>> classToMethodNames = new HashMap<>();
    // pattern focuses on the two colons that seperate the class name from the
    // method name and signature
    // the preceding return type as well as the exact signature of the method is
    // going to be ignore
    // -> we are going to check explicitly if a method with a given name has the
    // respective annotation
    // in case it is overloaded
    private static final Pattern pattern = Pattern.compile("(?:.*?)([\\w\\.]+)::(.*?)\\((?:.*?)\\)");

    @JsonCreator
    public FixMissingJavadocsForNewMethods(@JsonProperty("pathToReportFile") String pathToReportFile,
            @JsonProperty("version") String version) throws IOException {
        this.version = version;
        if (pathToReportFile != null) {
            // TODO: find out why the recipe is called multiple times, including with null
            // do we need a preprocessor recipe?
            parseReportFile(pathToReportFile);
        }
    }

    private void parseReportFile(String pathToReportFile) throws IOException {
        for (String line : Files.readAllLines(Paths.get(pathToReportFile))) {

            // signatures can be quite complex, i.e., new: method java.lang.Class<? extends
            // com.vaadin.flow.router.RouterLayout>
            // com.vaadin.flow.server.RouteRegistry::getLayout(java.lang.String)
            // TODO 'manual' type erasure -> remove generics
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                System.err.println("Found: " + matcher.group(1) + ":" + matcher.group(2));
                String className = matcher.group(1);
                String methodName = matcher.group(2);
                classToMethodNames.computeIfAbsent(className, k -> new ArrayList<>()).add(methodName);
            }
        }
    }

    @Override
    public @DisplayName String getDisplayName() {
        return "Adds @since tag together with a version to the Javadoc of new methods";
    }

    @Override
    public @Description String getDescription() {
        return """
        After a Revapi report has been generated, it will be processed and the resulting file containing a 
        list of entries like 'new: method boolean com.vaadin.flow.plugin.base.PluginAdapterBuild::forceProductionBuild()'
        will be stored inside this instance. If any of the listed methods is encountered during processing,
        a fitting Javadoc @since tag will be added.""";
    }

    @Override
    public boolean causesAnotherCycle() {
        return false;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public MethodDeclaration visitMethodDeclaration(MethodDeclaration method, ExecutionContext p) {

                var classDeclaration = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (classDeclaration.getType() instanceof JavaType.FullyQualified) {
                    String fqn = ((JavaType.FullyQualified) classDeclaration.getType()).getFullyQualifiedName();
                    var methods = classToMethodNames.get(fqn);
                    if (!methods.isEmpty()) {
                        if (methods.contains(method.getSimpleName())) {
                            // add @since tag
                            // method.getComments().stream().anyMatch(Javadoc.DocComment.class::isInstance);
                            // List<Annotation> annotations = new ArrayList<>();
                            // // new Annotation(null, Space.EMPTY, Markers.EMPTY, J.Identifier.build("Y U NO WORK") , null)
                            // return method.withLeadingAnnotations( annotations );
                        }

                    }
                }
                
                return super.visitMethodDeclaration(method, p);
            }

        };
    }

}
