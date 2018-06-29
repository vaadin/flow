package com.vaadin.generator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Named;
import org.jboss.forge.roaster.model.Packaged;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaEnumSource;

import com.vaadin.flow.component.HasTheme;
import com.vaadin.generator.exception.ComponentGenerationException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The variants collector that stores all the theme variants data and generates
 * a set of enums basing on it.
 */
class ThemeVariantsCollector {
    private final File targetDir;
    private final String basePackage;
    private final Map<String, Map<String, List<String>>> variants = new HashMap<>();

    /**
     * Creates the collector instance.
     *
     * @param targetDir
     *            the directory to generate the files into
     * @param basePackage
     *            the package that all the generated files will have
     */
    ThemeVariantsCollector(File targetDir, String basePackage) {
        if (!targetDir.isDirectory() && !targetDir.mkdirs()) {
            throw new ComponentGenerationException(
                    "Could not create target directory \"" + targetDir + '"');
        }
        this.targetDir = targetDir;
        this.basePackage = basePackage;
    }

    /**
     * Collects the data on the variant.
     * 
     * @param componentName
     *            the component name that the variants are relevant to
     * @param themeToVariants
     *            theme name and a list of variants applicable to the component
     *            for the current theme
     */
    void collect(String componentName,
            Map<String, List<String>> themeToVariants) {
        for (Map.Entry<String, List<String>> themeAndVariants : themeToVariants
                .entrySet()) {
            variants.computeIfAbsent(themeAndVariants.getKey(),
                    key -> new HashMap<>())
                    .computeIfAbsent(componentName, key -> new ArrayList<>())
                    .addAll(themeAndVariants.getValue());
        }
    }

    /**
     * Generates the theme variants classes based on the data collected.
     */
    void generateThemeVariants() {
        for (Map.Entry<String, Map<String, List<String>>> themeToVariants : variants
                .entrySet()) {

            Class<HasTheme.ThemeVariant> themeVariantInterface = HasTheme.ThemeVariant.class;
            JavaClassSource themeClass = Roaster.create(JavaClassSource.class);
            String themeName = StringUtils.capitalize(themeToVariants.getKey());
            themeClass.setPublic().setPackage(basePackage)
                    .setName(themeName + "Variants")
                    .addImport(themeVariantInterface);
            themeClass.getJavaDoc().setText(String.format(
                    "Contains '%s' theme variants for components.", themeName));

            for (Map.Entry<String, List<String>> classNameToVariants : themeToVariants
                    .getValue().entrySet()) {
                String rawName = classNameToVariants.getKey();
                String componentName = rawName.startsWith("Vaadin")
                        ? rawName.substring("Vaadin".length())
                        : rawName;
                JavaEnumSource classEnum = Roaster.create(JavaEnumSource.class)
                        .setName(StringUtils.capitalize(componentName)
                                + "Variants")
                        .setPackage(basePackage);
                classEnum.getJavaDoc().setText(String.format(
                        "Set of variants applicable for '%s' component and '%s' theme.",
                        componentName, themeName));

                Method requiredMethod = getThemeVariantMethod(
                        themeVariantInterface);
                classEnum.addInterface(themeVariantInterface);

                FieldSource<JavaEnumSource> variantField = classEnum.addField()
                        .setPrivate().setType(requiredMethod.getReturnType())
                        .setName("variant").setFinal(true);

                classEnum.addMethod().setConstructor(true)
                        .setBody(String.format("this.%s = %s;",
                                variantField.getName(), variantField.getName()))
                        .addParameter(requiredMethod.getReturnType(),
                                variantField.getName());

                classEnum.addMethod().setPublic()
                        .setReturnType(requiredMethod.getReturnType())
                        .setName(requiredMethod.getName())
                        .setBody(String.format("return %s;",
                                variantField.getName()))
                        .addAnnotation(Override.class);

                for (String variant : classNameToVariants.getValue()) {
                    classEnum.addEnumConstant(createEnumFieldName(variant))
                            .setConstructorArguments(
                                    String.format("\"%s\"", variant));
                }
                themeClass.addNestedType(classEnum);
            }

            writeClass(themeClass);
        }
    }

    private Method getThemeVariantMethod(Class<?> themeVariantInterface) {
        if (!themeVariantInterface.isInterface()) {
            throw new IllegalStateException(String.format(
                    "Expected '%s' to be an interface, but it's not",
                    themeVariantInterface));
        }
        Method[] themeVariantMethods = themeVariantInterface.getMethods();
        if (themeVariantMethods.length != 1) {
            throw new IllegalStateException(String.format(
                    "Expected interface '%s' to have exactly one method, but got: '%s'",
                    themeVariantInterface,
                    Arrays.toString(themeVariantMethods)));
        }
        return themeVariantMethods[0];
    }

    private String createEnumFieldName(String variantName) {
        return StringUtils.upperCase(variantName, Locale.ENGLISH)
                .replaceAll("-", "_");
    }

    private <T extends Named & Packaged<?>> void writeClass(T themeClass) {
        String fileName = themeClass.getName() + ".java";
        try {
            Files.write(
                    new File(
                            ComponentGeneratorUtils.convertPackageToDirectory(
                                    targetDir, themeClass.getPackage(), true),
                            fileName).toPath(),
                    themeClass.toString().getBytes(UTF_8));
        } catch (IOException ex) {
            throw new ComponentGenerationException(
                    "Error writing the generated Java source file \"" + fileName
                            + "\" to \"" + targetDir + '"',
                    ex);
        }
    }
}
