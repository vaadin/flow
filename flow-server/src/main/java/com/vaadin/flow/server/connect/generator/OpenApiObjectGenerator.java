/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.server.connect.generator;

import javax.annotation.Nullable;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LiteralStringValueExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.SourceRoot;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.connect.Endpoint;
import com.vaadin.flow.server.connect.EndpointNameChecker;
import com.vaadin.flow.server.connect.auth.AnonymousAllowed;

/**
 * Java parser class which scans for all {@link Endpoint} classes and
 * produces OpenApi json.
 */
public class OpenApiObjectGenerator {
    public static final String EXTENSION_VAADIN_CONNECT_PARAMETERS_DESCRIPTION = "x-vaadin-parameters-description";
    public static final String EXTENSION_VAADIN_FILE_PATH = "x-vaadin-file-path";

    private static final String VAADIN_CONNECT_OAUTH2_SECURITY_SCHEME = "vaadin-connect-oauth2";
    private static final String VAADIN_CONNECT_OAUTH2_TOKEN_URL = "/oauth/token";

    private List<Path> javaSourcePaths = new ArrayList<>();
    private OpenApiConfiguration configuration;
    private Map<String, ResolvedReferenceType> usedTypes;
    private Map<ClassOrInterfaceDeclaration, String> endpointsJavadoc;
    private Map<String, ClassOrInterfaceDeclaration> nonEndpointMap;
    private Map<String, String> qualifiedNameToPath;
    private Map<String, PathItem> pathItems;
    private Set<String> generatedSchema;
    private OpenAPI openApiModel;
    private final EndpointNameChecker endpointNameChecker = new EndpointNameChecker();
    private ClassLoader typeResolverClassLoader;
    private SchemaResolver schemaResolver;

    /**
     * Adds the source path to the generator to process.
     *
     * @param sourcePath
     *            the source path to generate the medatata from
     */
    public void addSourcePath(Path sourcePath) {
        if (sourcePath == null) {
            throw new IllegalArgumentException(
                    "Java source path must be a valid directory");
        }
        if (!sourcePath.toFile().exists()) {
            throw new IllegalArgumentException(String
                    .format("Java source path '%s' doesn't exist", sourcePath));
        }
        this.javaSourcePaths.add(sourcePath);
    }

    /**
     * Set project's class loader which is used for resolving types from that
     * project.
     *
     * @param typeResolverClassLoader
     *            the project's class loader for type resolving
     */
    void setTypeResolverClassLoader(ClassLoader typeResolverClassLoader) {
        this.typeResolverClassLoader = typeResolverClassLoader;
    }

    /**
     * Sets the configuration to be used when generating an Open API spec.
     *
     * @param configuration
     *            the generator configuration
     */
    public void setOpenApiConfiguration(OpenApiConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Gets the Open API, generates it if necessary.
     *
     * @return the Open API data
     */
    public OpenAPI getOpenApi() {
        if (openApiModel == null) {
            init();
        }
        return openApiModel;
    }

    OpenAPI generateOpenApi() {
        init();
        return openApiModel;
    }

    private void init() {
        if (javaSourcePaths == null || configuration == null) {
            throw new IllegalStateException(
                    "Java source path and configuration should not be null");
        }
        openApiModel = createBasicModel();
        nonEndpointMap = new HashMap<>();
        qualifiedNameToPath = new HashMap<>();
        pathItems = new TreeMap<>();
        usedTypes = new HashMap<>();
        generatedSchema = new HashSet<>();
        endpointsJavadoc = new HashMap<>();
        schemaResolver = new SchemaResolver();
        ParserConfiguration parserConfiguration = createParserConfiguration();

        javaSourcePaths.stream()
                .map(path -> new SourceRoot(path, parserConfiguration))
                .forEach(this::parseSourceRoot);

        for (Map.Entry<String, ResolvedReferenceType> entry : usedTypes
                .entrySet()) {
            List<Schema> schemas = createSchemasFromQualifiedNameAndType(
                    entry.getKey(), entry.getValue());
            schemas.forEach(schema -> {
                if (qualifiedNameToPath.get(schema.getName()) != null) {
                    schema.addExtension(EXTENSION_VAADIN_FILE_PATH,
                            qualifiedNameToPath.get(schema.getName()));
                }
                openApiModel.getComponents().addSchemas(schema.getName(),
                        schema);
            });
        }
        addTagsInformation();
    }

    private ParserConfiguration createParserConfiguration() {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver(
                new ReflectionTypeSolver(false));
        if (typeResolverClassLoader != null) {
            combinedTypeSolver
                    .add(new ClassLoaderTypeSolver(typeResolverClassLoader));
        }
        return new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));
    }

    private void parseSourceRoot(SourceRoot sourceRoot) {
        try {
            sourceRoot.parse("", this::process);
        } catch (Exception e) {
            throw new IllegalStateException(String.format(
                    "Can't parse the java files in the source root '%s'",
                    sourceRoot), e);
        }
    }

    private void addTagsInformation() {
        for (Map.Entry<ClassOrInterfaceDeclaration, String> endpointJavadoc : endpointsJavadoc
                .entrySet()) {
            Tag tag = new Tag();
            ClassOrInterfaceDeclaration endpointDeclaration = endpointJavadoc
                    .getKey();
            String simpleClassName = endpointDeclaration.getNameAsString();
            tag.name(simpleClassName);
            tag.description(endpointJavadoc.getValue());
            tag.addExtension(EXTENSION_VAADIN_FILE_PATH,
                    qualifiedNameToPath.get(endpointDeclaration
                            .getFullyQualifiedName().orElse(simpleClassName)));
            openApiModel.addTagsItem(tag);
        }
    }

    private OpenAPI createBasicModel() {
        OpenAPI openAPI = new OpenAPI();

        Info info = new Info();
        info.setTitle(configuration.getApplicationTitle());
        info.setVersion(configuration.getApplicationApiVersion());
        openAPI.setInfo(info);

        Paths paths = new Paths();
        openAPI.setPaths(paths);

        Server server = new Server();
        server.setUrl(configuration.getServerUrl());
        server.setDescription(configuration.getServerDescription());
        openAPI.setServers(Collections.singletonList(server));
        Components components = new Components();
        SecurityScheme vaadinConnectOAuth2Scheme = new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .flows(new OAuthFlows().password(new OAuthFlow()
                        .tokenUrl(VAADIN_CONNECT_OAUTH2_TOKEN_URL)
                        .scopes(new Scopes())));
        components.addSecuritySchemes(VAADIN_CONNECT_OAUTH2_SECURITY_SCHEME,
                vaadinConnectOAuth2Scheme);
        openAPI.components(components);
        return openAPI;
    }

    @SuppressWarnings("squid:S1172")
    private SourceRoot.Callback.Result process(Path localPath,
            Path absolutePath, ParseResult<CompilationUnit> result) {
        result.ifSuccessful(compilationUnit -> compilationUnit.getPrimaryType()
                .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                .filter(classOrInterfaceDeclaration -> !classOrInterfaceDeclaration
                        .isInterface())
                .map(this::appendNestedClasses).orElse(Collections.emptyList())
                .forEach(classOrInterfaceDeclaration -> this.parseClass(
                        classOrInterfaceDeclaration, compilationUnit)));
        pathItems.forEach((pathName, pathItem) -> openApiModel.getPaths()
                .addPathItem(pathName, pathItem));
        return SourceRoot.Callback.Result.DONT_SAVE;
    }

    private Collection<ClassOrInterfaceDeclaration> appendNestedClasses(
            ClassOrInterfaceDeclaration topLevelClass) {
        Set<ClassOrInterfaceDeclaration> nestedClasses = topLevelClass
                .getMembers().stream()
                .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator
                        .comparing(NodeWithSimpleName::getNameAsString))));
        nestedClasses.add(topLevelClass);
        return nestedClasses;
    }

    private void parseClass(ClassOrInterfaceDeclaration classDeclaration,
            CompilationUnit compilationUnit) {
        Optional<AnnotationExpr> endpointAnnotation = classDeclaration
                .getAnnotationByClass(Endpoint.class);
        compilationUnit.getStorage().ifPresent(storage -> {
            String className = classDeclaration.getFullyQualifiedName()
                    .orElse(classDeclaration.getNameAsString());
            qualifiedNameToPath.put(className, storage.getPath().toString());
        });
        if (!GeneratorUtils.hasAnnotation(classDeclaration, compilationUnit, Endpoint.class)) {
            nonEndpointMap.put(classDeclaration.resolve().getQualifiedName(),
                    classDeclaration);
        } else {
            Optional<Javadoc> javadoc = classDeclaration.getJavadoc();
            if (javadoc.isPresent()) {
                endpointsJavadoc.put(classDeclaration,
                        javadoc.get().getDescription().toText());
            } else {
                endpointsJavadoc.put(classDeclaration, "");
            }
            pathItems.putAll(createPathItems(
                    getEndpointName(classDeclaration, endpointAnnotation.orElse(null)),
                    classDeclaration));
        }
    }

    private String getEndpointName(ClassOrInterfaceDeclaration classDeclaration,
            AnnotationExpr endpointAnnotation) {
        String endpointName = Optional.ofNullable(endpointAnnotation)
                .filter(Expression::isSingleMemberAnnotationExpr)
                .map(Expression::asSingleMemberAnnotationExpr)
                .map(SingleMemberAnnotationExpr::getMemberValue)
                .map(Expression::asStringLiteralExpr)
                .map(LiteralStringValueExpr::getValue)
                .filter(GeneratorUtils::isNotBlank)
                .orElse(classDeclaration.getNameAsString());

        // detect the endpoint value name
        if (endpointName.equals(classDeclaration.getNameAsString()) && endpointAnnotation != null) {
            String endpointValueName = getEndpointValueName(endpointAnnotation);
            if (endpointValueName != null) {
                endpointName = endpointValueName.substring(1, endpointValueName.length() - 1);
            }
        }

        String validationError = endpointNameChecker.check(endpointName);
        if (validationError != null) {
            throw new IllegalStateException(
                    String.format("Endpoint name '%s' is invalid, reason: '%s'",
                            endpointName, validationError));
        }
        return endpointName;
    }

    private String getEndpointValueName(AnnotationExpr endpointAnnotation) {
        return endpointAnnotation.getChildNodes().stream().filter(node ->
                node.getTokenRange().isPresent() &&
                        "value".equals(node.getTokenRange().get().getBegin().getText()))
                .map(node -> node.getTokenRange().get().getEnd().getText()).findFirst().orElse(null);
    }

    private List<Schema> parseNonEndpointClassAsSchema(
            String fullQualifiedName) {
        ClassOrInterfaceDeclaration typeDeclaration = nonEndpointMap
                .get(fullQualifiedName);
        if (typeDeclaration == null) {
            return Collections.emptyList();
        }
        List<Schema> result = new ArrayList<>();

        Schema schema = createSingleSchema(fullQualifiedName, typeDeclaration);
        generatedSchema.add(fullQualifiedName);

        NodeList<ClassOrInterfaceType> extendedTypes = typeDeclaration
                .getExtendedTypes();
        if (extendedTypes.isEmpty()) {
            result.add(schema);
            result.addAll(generatedRelatedSchemas(schema));
        } else {
            ComposedSchema parentSchema = new ComposedSchema();
            parentSchema.setName(fullQualifiedName);
            result.add(parentSchema);
            extendedTypes.forEach(parentType -> {
                ResolvedReferenceType resolvedParentType = parentType.resolve();
                String parentQualifiedName = resolvedParentType
                        .getQualifiedName();
                String parentRef = schemaResolver
                        .getFullQualifiedNameRef(parentQualifiedName);
                parentSchema.addAllOfItem(new ObjectSchema().$ref(parentRef));
                schemaResolver.addFoundTypes(parentQualifiedName,
                        resolvedParentType);
            });
            // The inserting order matters for `allof` property.
            parentSchema.addAllOfItem(schema);
            result.addAll(generatedRelatedSchemas(parentSchema));
        }
        return result;
    }

    private Schema createSingleSchema(String fullQualifiedName,
            ClassOrInterfaceDeclaration typeDeclaration) {
        Optional<String> description = typeDeclaration.getJavadoc()
                .map(javadoc -> javadoc.getDescription().toText());
        Schema schema = new ObjectSchema();
        schema.setName(fullQualifiedName);
        description.ifPresent(schema::setDescription);
        Map<String, Schema> properties = getPropertiesFromClassDeclaration(
                typeDeclaration);
        schema.properties(properties);
        List<String> requiredList = properties.entrySet().stream()
                .filter(stringSchemaEntry -> GeneratorUtils
                        .isNotTrue(stringSchemaEntry.getValue().getNullable()))
                .map(Map.Entry::getKey).collect(Collectors.toList());
        // Nullable is represented in requiredList instead.
        properties.values()
                .forEach(propertySchema -> propertySchema.nullable(null));
        schema.setRequired(requiredList);
        return schema;
    }

    private List<Schema> createSchemasFromQualifiedNameAndType(
            String qualifiedName, ResolvedReferenceType resolvedReferenceType) {
        List<Schema> list = parseNonEndpointClassAsSchema(qualifiedName);
        if (list.isEmpty()) {
            return parseReferencedTypeAsSchema(resolvedReferenceType);
        } else {
            return list;
        }
    }

    private Map<String, Schema> getPropertiesFromClassDeclaration(
            TypeDeclaration<ClassOrInterfaceDeclaration> typeDeclaration) {
        Map<String, Schema> properties = new TreeMap<>();
        for (FieldDeclaration field : typeDeclaration.getFields()) {
            if (field.isTransient() || field.isStatic()
                    || field.isAnnotationPresent(JsonIgnore.class)) {
                continue;
            }
            Optional<String> fieldDescription = field.getJavadoc()
                    .map(javadoc -> javadoc.getDescription().toText());
            field.getVariables().forEach(variableDeclarator -> {
                Schema propertySchema = parseTypeToSchema(
                        variableDeclarator.getType(),
                        fieldDescription.orElse(""));
                if (field.isAnnotationPresent(Nullable.class)
                        || GeneratorUtils.isTrue(propertySchema.getNullable())) {
                    // Temporarily set nullable to indicate this property is
                    // not required
                    propertySchema.setNullable(true);
                }
                properties.put(variableDeclarator.getNameAsString(),
                        propertySchema);
            });
        }
        return properties;
    }

    private Map<String, ResolvedReferenceType> collectUsedTypesFromSchema(
            Schema schema) {
        Map<String, ResolvedReferenceType> map = new HashMap<>();
        if (GeneratorUtils.isNotBlank(schema.getName())
                || GeneratorUtils.isNotBlank(schema.get$ref())) {
            String name = GeneratorUtils.firstNonBlank(schema.getName(),
                    schemaResolver.getSimpleRef(schema.get$ref()));
            ResolvedReferenceType resolvedReferenceType = schemaResolver
                    .getFoundTypeByQualifiedName(name);
            if (resolvedReferenceType != null) {
                map.put(name, resolvedReferenceType);
            } else {
                getLogger().info(
                        "Can't find the type information of class '{}'. "
                                + "This might result in a missing schema in the generated OpenAPI spec.",
                        name);
            }
        }
        if (schema instanceof ArraySchema) {
            map.putAll(collectUsedTypesFromSchema(
                    ((ArraySchema) schema).getItems()));
        } else if (schema instanceof MapSchema
                && schema.getAdditionalProperties() != null) {
            map.putAll(collectUsedTypesFromSchema(
                    (Schema) schema.getAdditionalProperties()));
        } else if (schema instanceof ComposedSchema
                && ((ComposedSchema) schema).getAllOf() != null) {
                for (Schema child : ((ComposedSchema) schema).getAllOf()) {
                    map.putAll(collectUsedTypesFromSchema(child));
                }
        }
        if (schema.getProperties() != null) {
            schema.getProperties().values().forEach(
                    o -> map.putAll(collectUsedTypesFromSchema((Schema) o)));
        }
        return map;
    }

    private boolean isReservedWord(String word) {
        return word != null
                && EndpointNameChecker.ECMA_SCRIPT_RESERVED_WORDS
                        .contains(word.toLowerCase());
    }

    private Map<String, PathItem> createPathItems(String endpointName,
            ClassOrInterfaceDeclaration typeDeclaration) {
        Map<String, PathItem> newPathItems = new HashMap<>();
        for (MethodDeclaration methodDeclaration : typeDeclaration
                .getMethods()) {
            if (isAccessForbidden(typeDeclaration, methodDeclaration)) {
                continue;
            }
            String methodName = methodDeclaration.getNameAsString();

            Operation post = createPostOperation(methodDeclaration);
            if (methodDeclaration.getParameters().isNonEmpty()) {
                post.setRequestBody(createRequestBody(methodDeclaration));
            }

            ApiResponses responses = createApiResponses(methodDeclaration);
            post.setResponses(responses);
            post.tags(Collections
                    .singletonList(typeDeclaration.getNameAsString()));
            PathItem pathItem = new PathItem().post(post);

            String pathName = "/" + endpointName + "/" + methodName;
            pathItem.readOperationsMap()
                    .forEach((httpMethod, operation) -> operation
                            .setOperationId(String.join("_", endpointName,
                                    methodName, httpMethod.name())));
            newPathItems.put(pathName, pathItem);
        }
        return newPathItems;
    }

    private boolean isAccessForbidden(
            ClassOrInterfaceDeclaration typeDeclaration,
            MethodDeclaration methodDeclaration) {
        return !methodDeclaration.isPublic()
                || (hasSecurityAnnotation(methodDeclaration)
                        ? methodDeclaration.isAnnotationPresent(DenyAll.class)
                        : typeDeclaration.isAnnotationPresent(DenyAll.class));
    }

    private boolean hasSecurityAnnotation(MethodDeclaration method) {
        return method.isAnnotationPresent(AnonymousAllowed.class)
                || method.isAnnotationPresent(PermitAll.class)
                || method.isAnnotationPresent(DenyAll.class)
                || method.isAnnotationPresent(RolesAllowed.class);
    }

    private Operation createPostOperation(MethodDeclaration methodDeclaration) {
        Operation post = new Operation();
        SecurityRequirement securityItem = new SecurityRequirement();
        securityItem.addList(VAADIN_CONNECT_OAUTH2_SECURITY_SCHEME);
        post.addSecurityItem(securityItem);

        methodDeclaration.getJavadoc().ifPresent(javadoc -> post
                .setDescription(javadoc.getDescription().toText()));
        return post;
    }

    private ApiResponses createApiResponses(
            MethodDeclaration methodDeclaration) {
        ApiResponse successfulResponse = createApiSuccessfulResponse(
                methodDeclaration);
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse("200", successfulResponse);
        return responses;
    }

    private ApiResponse createApiSuccessfulResponse(
            MethodDeclaration methodDeclaration) {
        Content successfulContent = new Content();
        // "description" is a REQUIRED property of Response
        ApiResponse successfulResponse = new ApiResponse().description("");
        methodDeclaration.getJavadoc().ifPresent(javadoc -> {
            for (JavadocBlockTag blockTag : javadoc.getBlockTags()) {
                if (blockTag.getType() == JavadocBlockTag.Type.RETURN) {
                    successfulResponse.setDescription(
                            "Return " + blockTag.getContent().toText());
                }
            }
        });
        if (!methodDeclaration.getType().isVoidType()) {
            MediaType mediaItem = createReturnMediaType(methodDeclaration);
            successfulContent.addMediaType("application/json", mediaItem);
            successfulResponse.content(successfulContent);
        }
        return successfulResponse;
    }

    private MediaType createReturnMediaType(
            MethodDeclaration methodDeclaration) {
        MediaType mediaItem = new MediaType();
        Type methodReturnType = methodDeclaration.getType();
        Schema schema = parseTypeToSchema(methodReturnType, "");
        if (methodDeclaration.isAnnotationPresent(Nullable.class)) {
            schema = schemaResolver.createNullableWrapper(schema);
        }
        usedTypes.putAll(collectUsedTypesFromSchema(schema));
        mediaItem.schema(schema);
        return mediaItem;
    }

    private RequestBody createRequestBody(MethodDeclaration methodDeclaration) {
        Map<String, String> paramsDescription = new HashMap<>();
        methodDeclaration.getJavadoc().ifPresent(javadoc -> {
            for (JavadocBlockTag blockTag : javadoc.getBlockTags()) {
                if (blockTag.getType() == JavadocBlockTag.Type.PARAM) {
                    paramsDescription.put(blockTag.getName().orElse(""),
                            blockTag.getContent().toText());
                }
            }
        });

        RequestBody requestBody = new RequestBody();
        Content requestBodyContent = new Content();
        requestBody.content(requestBodyContent);
        MediaType requestBodyObject = new MediaType();
        requestBodyContent.addMediaType("application/json", requestBodyObject);
        Schema requestSchema = new ObjectSchema();
        requestSchema.setRequired(new ArrayList<>());
        requestBodyObject.schema(requestSchema);
        methodDeclaration.getParameters().forEach(parameter -> {
            Schema paramSchema = parseTypeToSchema(parameter.getType(), "");
            usedTypes.putAll(collectUsedTypesFromSchema(paramSchema));
            String name = (isReservedWord(parameter.getNameAsString()) ? "_"
                    : "").concat(parameter.getNameAsString());
            if (GeneratorUtils.isBlank(paramSchema.get$ref())) {
                paramSchema.description(
                        paramsDescription.remove(parameter.getNameAsString()));
            }
            requestSchema.addProperties(name, paramSchema);
            if (GeneratorUtils.isNotTrue(paramSchema.getNullable())
                    && !parameter.isAnnotationPresent(Nullable.class)) {
                requestSchema.addRequiredItem(name);
            }
            paramSchema.setNullable(null);
        });
        if (!paramsDescription.isEmpty()) {
            requestSchema.addExtension(
                    EXTENSION_VAADIN_CONNECT_PARAMETERS_DESCRIPTION,
                    new LinkedHashMap<>(paramsDescription));
        }
        return requestBody;
    }

    private Schema parseTypeToSchema(Type javaType, String description) {
        try {
            Schema schema = parseResolvedTypeToSchema(javaType.resolve());
            if (GeneratorUtils.isNotBlank(description)) {
                schema.setDescription(description);
            }
            return schema;
        } catch (Exception e) {
            getLogger().info(String.format(
                    "Can't resolve type '%s' for creating custom OpenAPI Schema. Using the default ObjectSchema instead.",
                    javaType.asString()), e);
        }
        return new ObjectSchema();
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(OpenApiObjectGenerator.class);
    }

    private Schema parseResolvedTypeToSchema(ResolvedType resolvedType) {
        return schemaResolver.parseResolvedTypeToSchema(resolvedType);
    }

    @SuppressWarnings("squid:S1872")
    private List<Schema> parseReferencedTypeAsSchema(
            ResolvedReferenceType resolvedType) {
        List<Schema> results = new ArrayList<>();

        Schema schema = createSingleSchemaFromResolvedType(resolvedType);
        String qualifiedName = resolvedType.getQualifiedName();
        generatedSchema.add(qualifiedName);

        List<ResolvedReferenceType> directAncestors = resolvedType
                .getDirectAncestors().stream()
                .filter(parent -> parent.getTypeDeclaration().isClass()
                        && !Object.class.getName()
                                .equals(parent.getQualifiedName()))
                .collect(Collectors.toList());

        if (directAncestors.isEmpty()) {
            results.add(schema);
            results.addAll(generatedRelatedSchemas(schema));
        } else {
            ComposedSchema parentSchema = new ComposedSchema();
            parentSchema.name(qualifiedName);
            results.add(parentSchema);
            for (ResolvedReferenceType directAncestor : directAncestors) {
                String ancestorQualifiedName = directAncestor
                        .getQualifiedName();
                String parentRef = schemaResolver
                        .getFullQualifiedNameRef(ancestorQualifiedName);
                parentSchema.addAllOfItem(new ObjectSchema().$ref(parentRef));
                schemaResolver.addFoundTypes(ancestorQualifiedName,
                        directAncestor);
            }
            parentSchema.addAllOfItem(schema);
            results.addAll(generatedRelatedSchemas(parentSchema));
        }
        return results;
    }

    private List<Schema> generatedRelatedSchemas(Schema schema) {
        List<Schema> result = new ArrayList<>();
        collectUsedTypesFromSchema(schema).entrySet().stream()
                .filter(s -> !generatedSchema.contains(s.getKey()))
                .forEach(s -> result.addAll(
                        createSchemasFromQualifiedNameAndType(s.getKey(),
                                s.getValue())));
        return result;
    }

    private Schema createSingleSchemaFromResolvedType(
            ResolvedReferenceType resolvedType) {
        Schema schema = new ObjectSchema()
                .name(resolvedType.getQualifiedName());
        Map<String, Boolean> fieldsOptionalMap = getFieldsAndOptionalMap(
                resolvedType);
        Set<ResolvedFieldDeclaration> serializableFields = resolvedType
                .getDeclaredFields().stream()
                .filter(resolvedFieldDeclaration -> fieldsOptionalMap
                        .containsKey(resolvedFieldDeclaration.getName()))
                .collect(Collectors.toSet());
        // Make sure the order is consistent in properties map
        schema.setProperties(new TreeMap<>());
        for (ResolvedFieldDeclaration resolvedFieldDeclaration : serializableFields) {
            String name = resolvedFieldDeclaration.getName();
            Schema type = parseResolvedTypeToSchema(
                    resolvedFieldDeclaration.getType());
            if (!fieldsOptionalMap.get(name)) {
                schema.addRequiredItem(name);
            }
            schema.addProperties(name, type);
        }
        return schema;
    }

    /**
     * Because it's not possible to check the `transient` modifier and
     * annotation of a field using JavaParser API. We need this method to
     * reflect the type and get those information from the reflected object.
     *
     * @param resolvedType
     *            type of the class to get fields information
     * @return set of fields' name that we should generate.
     */
    private Map<String, Boolean> getFieldsAndOptionalMap(
            ResolvedReferenceType resolvedType) {
        if (!resolvedType.getTypeDeclaration().isClass()
                || resolvedType.getTypeDeclaration().isAnonymousClass()) {
            return Collections.emptyMap();
        }
        HashMap<String, Boolean> validFields = new HashMap<>();
        try {
            Class<?> aClass = getClassFromReflection(resolvedType);
            Arrays.stream(aClass.getDeclaredFields()).filter(field -> {

                int modifiers = field.getModifiers();
                return !Modifier.isStatic(modifiers)
                        && !Modifier.isTransient(modifiers)
                        && !field.isAnnotationPresent(JsonIgnore.class);
            }).forEach(field -> validFields.put(field.getName(),
                    field.isAnnotationPresent(Nullable.class)
                            || field.getType().equals(Optional.class)));
        } catch (ClassNotFoundException e) {

            String message = String.format(
                    "Can't get list of fields from class '%s'."
                            + "Please make sure that class '%s' is in your project's compile classpath. "
                            + "As the result, the generated TypeScript file will be empty.",
                    resolvedType.getQualifiedName(),
                    resolvedType.getQualifiedName());
            getLogger().info(message);
            getLogger().debug(message, e);
        }
        return validFields;
    }

    private Class<?> getClassFromReflection(ResolvedReferenceType resolvedType)
            throws ClassNotFoundException {
        String fullyQualifiedName = getFullyQualifiedName(resolvedType);
        if (typeResolverClassLoader != null) {
            return Class.forName(fullyQualifiedName, true,
                    typeResolverClassLoader);
        } else {
            return Class.forName(fullyQualifiedName);
        }
    }

    /**
     * This method return a fully qualified name from a resolved reference type
     * which is correct for nested declaration as well. The
     * {@link ResolvedReferenceType#getQualifiedName()} returns a canonical name
     * instead of a fully qualified name, which is not correct for nested
     * classes to be used in reflection. That's why this method is implemented.
     *
     * {@see Related discussion about FullyQualifiedName and CanonicalName:
     * https://github.com/javaparser/javaparser/issues/1480}
     *
     * @param resolvedReferenceType
     *            the type to get fully qualified name
     * @return fully qualified name
     */
    private String getFullyQualifiedName(
            ResolvedReferenceType resolvedReferenceType) {
        ResolvedReferenceTypeDeclaration typeDeclaration = resolvedReferenceType
                .getTypeDeclaration();
        String packageName = typeDeclaration.getPackageName();
        String canonicalName = typeDeclaration.getQualifiedName();
        if (GeneratorUtils.isBlank(packageName)) {
            return GeneratorUtils.replaceChars(canonicalName, '.', '$');
        } else {
            String name = GeneratorUtils.substringAfterLast(canonicalName,
                    packageName + ".");
            return String.format("%s.%s", packageName,
                    GeneratorUtils.replaceChars(name, '.', '$'));
        }
    }
}
