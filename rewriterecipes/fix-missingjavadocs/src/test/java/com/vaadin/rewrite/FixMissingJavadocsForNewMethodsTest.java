package com.vaadin.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

import java.io.IOException;


public class FixMissingJavadocsForNewMethodsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        // spec.recipeFromResources("com.vaadin.rewrite.FixMissingJavadocsForNewMethods")
        //         .parser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true));
                try {
                    spec.recipe(new FixMissingJavadocsForNewMethods("src\\test\\java\\com\\vaadin\\rewrite\\signaturesForAtSince.txt", "1.6"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

    @Test
    void testFix(){

        rewriteRun(
            java(
            """
                package foo.bar;
                class A {
                boolean testBar(String s) {
                return true;
                }
              }
            """,
            """
                package foo.bar;
                class A {
                /**
                 * @since 1.6
                 * /
                boolean testBar(String s) {
                return true;
                }
              }
            """)

        );

    }

}
