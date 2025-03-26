package com.vaadin.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;


public class FixMissingJavadocsForNewMethodsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        // spec.recipeFromResources("com.vaadin.rewrite.FixMissingJavadocsForNewMethods")
        //         .parser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true));
                try {
                    File file = Files.createTempFile("testdata", "dat").toFile();
                    file.deleteOnExit();
                    PrintWriter pw = new PrintWriter(file);
                    pw.println("new: method boolean foo.bar.A::testBar(java.lang.String)");
                    pw.close();
                    spec.recipe(new FixMissingJavadocsForNewMethods(file.getAbsolutePath(), "1.6"));
                    // spec.recipe(new FixMissingJavadocsForNewMethods("src\\test\\java\\com\\vaadin\\rewrite\\signaturesForAtSince.txt", "1.6"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

    @Test
    void testFixingNoComment(){

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
                 */
                boolean testBar(String s) {
                return true;
                }
              }
            """)

        );

    }

    @Test
    void testFixingJustComments(){

        rewriteRun(
            java(
            """
                package foo.bar;
                /*
                  Some random comment here*/
                class A {
                /* This method
                 * totally does stuff.
                 * /
                boolean testBar(String s) {
                return true;
                }
              }
            """,
            """
                package foo.bar;
                /*
                  Some random comment here*/
                class A {
                /* This method
                 * totally does stuff.
                 * /
                /**
                 * @since 1.6
                 */
                boolean testBar(String s) {
                return true;
                }
              }
            """)

        );

    }

    @Test
    void testFixingJavadocWithComment(){

        rewriteRun(
            java(
            """
                package foo.bar;
                class A {
                /* TODO: fix me
                */
                /**
                 * @author Jon Doe
                 * @param s the String
                 * /
                boolean testBar(String s) {
                return true;
                }
              }
            """,
            """
                package foo.bar;
                class A {
                /* TODO: fix me
                */
                /**
                 * @author Jon Doe
                 * @param s the String
                 * @since 1.6
                 * /
                boolean testBar(String s) {
                return true;
                }
              }
            """)

        );

    }

    @Test
    void testFixingJavadocWithCommentInReverseOrder(){

        rewriteRun(
            java(
            """
                package foo.bar;
                class A {
                /**
                 * @author Jon Doe
                 * @param s the String
                 * /
                /* TODO: fix me
                */
                boolean testBar(String s) {
                return true;
                }
              }
            """,
            """
                package foo.bar;
                class A {
                /**
                 * @author Jon Doe
                 * @param s the String
                 * @since 1.6
                 * /
                /* TODO: fix me
                */
                boolean testBar(String s) {
                return true;
                }
              }
            """)

        );

    }

}
