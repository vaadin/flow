import type { Plugin, SourceMapInput } from 'vite';
import ts from 'typescript';
import * as path from 'path';
import MagicString from 'magic-string';

type SupportedBasicTypes = 'any' | 'bigInt' | 'boolean' | 'null' | 'number' | 'string' | 'undefined' | 'unknown';

type ComponentPropertyType =
    | 'BIG_DECIMAL'
    | 'BIG_INTEGER'
    | 'BOOLEAN'
    | 'BYTE'
    | 'CHARACTER'
    | 'DOUBLE'
    | 'ENUM'
    | 'FLOAT'
    | 'INTEGER'
    | 'LONG'
    | 'SHORT'
    | 'STRING';

type ItemOptions = Array<{ label: string; value: any }>;

interface GenericArrayItemDescriptor {
    tsTypeString?: string;
    copilotProperty?: ComponentPropertyType;
    items: ItemOptions;
}

interface UnionDescriptor {
    tsTypeString?: string;
    items?: ItemOptions;
    null: boolean;
    undefined: boolean;
}

interface PropertyDescriptor {
    // Name of the property
    name: string;
    // Label that is displayed in the UI as a more human-readable version of name
    label: string;
    // Can be declared as question mark or union with undefined
    optional?: boolean;
    // Flag that indicates the given property can be null or undefined
    nullOrUndefined?: boolean;
    // Whether it can be null
    null?: boolean;
    // Whether it can be undefined
    undefined?: boolean;
    // True if given type is one of SupportedBasicTypes.
    basic?: boolean;
    // Flag that indicates the property is an object type
    object?: boolean;
    // Flag that indicates the property is an array type. true for number[], Array<string>...
    array?: boolean;
    // True if property is marked with any type
    any?: boolean;
    // Flag that represents that property cannot be successfully extracted.
    error?: boolean;
    // Explanation of the error
    errorReason?: string;
    // Type string extracted using Typescript API
    tsTypeString?: string;
    // Property that is known by the Copilot.
    copilotProperty?: ComponentPropertyType;
    // Options that can property value get. List has items for unions, enums etc...
    options?: ItemOptions;
}

interface ResponseData {
    error: boolean;
    errorMessage?: string;
    properties?: PropertyDescriptor[];
}

type ObjectTypeResolver = (type: ts.Type) => PropertyDescriptor[] | undefined;

/**
 * ResolverException is a known exception that is thrown explicitly to tell user why the given type cannot be resolved.
 */
export class ResolverException extends Error {
    constructor(message: string) {
        super(message);
        this.name = 'ResolverException';

        // Necessary to fix the prototype chain in transpiled JS
        Object.setPrototypeOf(this, new.target.prototype);
    }
}

interface PluginOptions {
    // Enable/disable the plugin
    enabled?: boolean;
    // Paths to exclude from processing
    exclude?: string[];
    // Enable debug logging
    debug?: boolean;
    // Maximum properties to track in memory (to prevent memory leaks)
    maxPropertiesInMemory?: number;
    // Whether to generate source maps
    sourcemap?: boolean;
}

const DEFAULT_OPTIONS: PluginOptions = {
    enabled: true,
    exclude: ['/generated/', '/node_modules/'],
    debug: false,
    maxPropertiesInMemory: 1000,
    sourcemap: true
};

/**
 * Custom Language Service Host that tracks files incrementally
 */
class IncrementalLanguageServiceHost implements ts.LanguageServiceHost {
    private files: Map<string, { version: number; content: string }> = new Map();
    private projectVersion = 0;
    private parsedConfig: ts.ParsedCommandLine;
    
    constructor() {
        this.parsedConfig = this.loadTsConfig();
    }
    
    private loadTsConfig(): ts.ParsedCommandLine {
        const configPath = ts.findConfigFile('./', ts.sys.fileExists, 'tsconfig.json');
        if (!configPath) {
            throw new Error('tsconfig.json not found in project root');
        }
        const config = ts.readConfigFile(configPath, ts.sys.readFile);
        if (config.error) {
            throw new Error(`Failed to read tsconfig.json: ${config.error.messageText}`);
        }
        return ts.parseJsonConfigFileContent(config.config, ts.sys, path.dirname(configPath));
    }
    
    updateFile(fileName: string, content: string): void {
        const normalizedPath = path.resolve(fileName);
        const existing = this.files.get(normalizedPath);
        
        if (!existing || existing.content !== content) {
            this.files.set(normalizedPath, {
                version: existing ? existing.version + 1 : 0,
                content
            });
            this.projectVersion++;
        }
    }
    
    getCompilationSettings(): ts.CompilerOptions {
        return this.parsedConfig.options;
    }
    
    getScriptFileNames(): string[] {
        return [...this.files.keys()];
    }
    
    getScriptVersion(fileName: string): string {
        const normalizedPath = path.resolve(fileName);
        const file = this.files.get(normalizedPath);
        return file ? file.version.toString() : '0';
    }
    
    getScriptSnapshot(fileName: string): ts.IScriptSnapshot | undefined {
        const normalizedPath = path.resolve(fileName);
        const file = this.files.get(normalizedPath);
        
        if (file) {
            return ts.ScriptSnapshot.fromString(file.content);
        }
        
        // Try to read from file system
        if (ts.sys.fileExists(fileName)) {
            const content = ts.sys.readFile(fileName);
            if (content) {
                this.updateFile(fileName, content);
                return ts.ScriptSnapshot.fromString(content);
            }
        }
        
        return undefined;
    }
    
    getCurrentDirectory(): string {
        return process.cwd();
    }
    
    getDefaultLibFileName(options: ts.CompilerOptions): string {
        return ts.getDefaultLibFilePath(options);
    }
    
    fileExists(fileName: string): boolean {
        const normalizedPath = path.resolve(fileName);
        return this.files.has(normalizedPath) || ts.sys.fileExists(fileName);
    }
    
    readFile(fileName: string): string | undefined {
        const normalizedPath = path.resolve(fileName);
        const file = this.files.get(normalizedPath);
        return file ? file.content : ts.sys.readFile(fileName);
    }
    
    getProjectVersion(): string {
        return this.projectVersion.toString();
    }
}

/**
 * A Vite plugin that statically analyzes TSX files to discover JSX elements and
 * their `props` types, then injects runtime code to expose those properties for
 * debugging / tooling.
 *
 * How it works (build-time):
 * 1) For each `.tsx` module (excluding configurable paths), the plugin:
 *    - Uses a shared TypeScript Language Service for efficient parsing
 *    - Parses the source file and collects distinct JSX opening elements
 *    - For each element, resolves the props type via TypeScript's type checker
 *    - Serializes the resolved properties into a `ResponseData` payload
 *
 * How it works (runtime, via injected code):
 * 2) Adds a global registry under `window.Vaadin.copilot.ReactProperties`:
 *    - `properties: Record<string, ResponseData>` – in-memory map of tagName → ResponseData
 *    - `registerer(tagName: string, value: ResponseData)` – helper to populate `properties`
 *    - Implements LRU-like cleanup to prevent memory leaks
 *
 * 3) All components (both intrinsic and custom) are registered via the global
 *    registry to avoid runtime variable existence issues.
 *
 * @param userOptions - Configuration options for the plugin
 * @returns Vite plugin instance
 */
export default function reactComponentPropertiesPlugin(userOptions: PluginOptions = {}): Plugin {
    const options = { ...DEFAULT_OPTIONS, ...userOptions };
    
    // Create Language Service once and reuse
    let serviceHost: IncrementalLanguageServiceHost | undefined;
    let languageService: ts.LanguageService | undefined;
    
    const getOrCreateLanguageService = (): { service: ts.LanguageService; host: IncrementalLanguageServiceHost } => {
        if (!languageService || !serviceHost) {
            serviceHost = new IncrementalLanguageServiceHost();
            languageService = ts.createLanguageService(serviceHost, ts.createDocumentRegistry());
            
            if (options.debug) {
                console.log('[ReactProperties] Created TypeScript Language Service');
            }
        }
        return { service: languageService, host: serviceHost };
    };
    
    return {
        name: 'vaadin-react-component-properties-plugin',
        enforce: 'pre',
        
        buildStart() {
            // Reset Language Service at the start of each build
            if (languageService) {
                languageService.dispose();
            }
            languageService = undefined;
            serviceHost = undefined;
            
            if (options.debug) {
                console.log('[ReactProperties] Plugin started with options:', options);
            }
        },
        
        buildEnd() {
            // Clean up Language Service
            if (languageService) {
                languageService.dispose();
                languageService = undefined;
                serviceHost = undefined;
                
                if (options.debug) {
                    console.log('[ReactProperties] Language Service disposed');
                }
            }
        },
        
        transform(code, id) {
            if (!options.enabled) {
                return;
            }
            
            const [bareId] = id.split('?');
            if (!bareId.endsWith('.tsx')) {
                return;
            }
            
            // Check if file should be excluded
            const shouldExclude = options.exclude?.some(pattern => id.includes(pattern));
            if (shouldExclude) {
                return;
            }
            
            try {
                const { service, host } = getOrCreateLanguageService();
                
                // Update the file in the Language Service
                host.updateFile(bareId, code);
                
                // Get the source file from Language Service
                const program = service.getProgram();
                if (!program) {
                    if (options.debug) {
                        console.warn('[ReactProperties] Could not get program from Language Service');
                    }
                    return;
                }
                
                const sourceFile = program.getSourceFile(bareId);
                if (!sourceFile) {
                    if (options.debug) {
                        console.warn('[ReactProperties] Could not get source file:', bareId);
                    }
                    return;
                }
                
                const typeChecker = program.getTypeChecker();
                const distinctJsxOpeningLikeElements = getDistinctJsxOpeningLikeElements(sourceFile);
                
                // If no JSX elements found, skip transformation
                if (distinctJsxOpeningLikeElements.length === 0) {
                    return;
                }
                
                // Use MagicString for better source map support
                const s = new MagicString(code);
                
                // Process JSX elements
                const componentPropsMap = new Map<string, string>();
                
                distinctJsxOpeningLikeElements.forEach((node) => {
                    const nodeName = node.tagName.getText();
                    try {
                        const resolver = new Resolver(typeChecker);
                        resolver.findPropsParamType(node, sourceFile);
                        const propertyDescriptors = resolver.resolveProps();
                        const responseDataStr = JSON.stringify({
                            error: false,
                            properties: propertyDescriptors
                        } as ResponseData);
                        
                        // Store for later injection, avoiding duplicates
                        if (!componentPropsMap.has(nodeName)) {
                            componentPropsMap.set(nodeName, responseDataStr);
                        }
                    } catch (e: unknown) {
                        const errorMessage = e instanceof ResolverException 
                            ? e.message 
                            : (e instanceof Error ? e.message : 'Unknown error occurred');
                        
                        const responseDataStr = JSON.stringify({
                            error: true,
                            errorMessage
                        } as ResponseData);
                        
                        if (!componentPropsMap.has(nodeName)) {
                            componentPropsMap.set(nodeName, responseDataStr);
                        }
                        
                        if (options.debug) {
                            console.warn(`[ReactProperties] Failed to resolve props for ${nodeName}:`, errorMessage);
                        }
                    }
                });
                
                // Generate injection code
                const injectCode = generateInjectionCode(componentPropsMap, options);
                
                // Append the injection code
                s.append(injectCode);
                
                return {
                    code: s.toString(),
                    map: options.sourcemap ? s.generateMap({ hires: true }) as SourceMapInput : null,
                };
                
            } catch (error) {
                if (options.debug) {
                    console.error('[ReactProperties] Transform error:', error);
                }
                return;
            }
        },
    };
}

/**
 * Generate the runtime injection code
 */
function generateInjectionCode(
    componentPropsMap: Map<string, string>, 
    options: PluginOptions
): string {
    if (componentPropsMap.size === 0) {
        return '';
    }
    
    // Helper function to escape strings for safe JavaScript injection
    const escapeForJS = (str: string): string => {
        return str
            .replace(/\\/g, '\\\\')
            .replace(/'/g, "\\'")
            .replace(/"/g, '\\"')
            .replace(/\n/g, '\\n')
            .replace(/\r/g, '\\r')
            .replace(/\t/g, '\\t');
    };
    
    let code = `
;(function() {
  'use strict';
  if (typeof window === 'undefined') return;
  
  window.Vaadin = window.Vaadin || {};
  window.Vaadin.copilot = window.Vaadin.copilot || {};
  window.Vaadin.copilot.ReactProperties = window.Vaadin.copilot.ReactProperties || {};
  window.Vaadin.copilot.ReactProperties.properties = window.Vaadin.copilot.ReactProperties.properties || {};
  
  if (!window.Vaadin.copilot.ReactProperties.registerer) {
    const maxProperties = ${options.maxPropertiesInMemory};
    const propertyKeys = [];
    
    window.Vaadin.copilot.ReactProperties.registerer = function(tagName, value) {
      const properties = window.Vaadin.copilot.ReactProperties.properties;
      
      // Implement LRU-like cleanup to prevent memory leaks
      if (!properties[tagName]) {
        propertyKeys.push(tagName);
        if (propertyKeys.length > maxProperties) {
          const keyToRemove = propertyKeys.shift();
          delete properties[keyToRemove];
        }
      }
      
      properties[tagName] = value;
    };
  }
  
`;
    
    // Register each component
    componentPropsMap.forEach((propsData, nodeName) => {
        const escapedName = escapeForJS(nodeName);
        code += `  window.Vaadin.copilot.ReactProperties.registerer("${escapedName}", ${propsData});\n`;
    });
    
    code += `})();\n`;
    
    return code;
}

/**
 * Traverse an AST and return all JsxOpeningLikeElement that are unique by tag name
 * (JsxOpeningElement | JsxSelfClosingElement).
 */
function getDistinctJsxOpeningLikeElements(root: ts.Node): ts.JsxOpeningLikeElement[] {
    const out: ts.JsxOpeningLikeElement[] = [];
    const foundTags = new Set<string>();
    const visit = (node: ts.Node) => {
        // There isn't a built-in ts.isJsxOpeningLikeElement type guard,
        // so check the two concrete cases:
        if (ts.isJsxOpeningElement(node) || ts.isJsxSelfClosingElement(node)) {
            // Both are assignable to JsxOpeningLikeElement
            const jsxOpeningLikeElement = node as ts.JsxOpeningLikeElement;
            const tagStr = jsxOpeningLikeElement.tagName.getText();
            if (!foundTags.has(tagStr)) {
                foundTags.add(tagStr);
                out.push(node as ts.JsxOpeningLikeElement);
            }
        }
        ts.forEachChild(node, visit);
    };

    visit(root);
    return out;
}

/**
 * Finds the JSX namespace or throws exception
 * @param checker TS checker to get Symbols
 * @param sourceFile
 */
function findJsxNamespaceSymbolOrThrow(checker: ts.TypeChecker, sourceFile: ts.SourceFile) {
    let jsxNamespaceSymbol = checker
        .getSymbolsInScope(sourceFile, ts.SymbolFlags.Namespace)
        .find((sym) => sym.name === 'JSX');
    if (jsxNamespaceSymbol) {
        return jsxNamespaceSymbol;
    }
    jsxNamespaceSymbol = checker
        .getAmbientModules()
        .flatMap((mod) => Array.from(mod.exports?.values() ?? []))
        .find((sym) => sym.name === 'IntrinsicElements');
    if (jsxNamespaceSymbol) {
        return jsxNamespaceSymbol;
    }
    jsxNamespaceSymbol = checker.resolveName('JSX', undefined, ts.SymbolFlags.Namespace, false);
    if (jsxNamespaceSymbol) {
        return jsxNamespaceSymbol;
    }

    throw new Error('JSX namespace not found (make sure DOM/React types are loaded)');
}

/*
 * Resolves the property of a component using Typescript Compiler API. The flow as follows:
 *  - Finds the props object
 *  - Parses the props object to obtain property descriptors
 *  - Resolves each property based on its type.
 *  If props object cannot be resolved or defined as `Any`, an exception is thrown.
 *  If a property cannot be resolved, property is marked as `error=true`.
 */
class Resolver {
    private readonly checker: ts.TypeChecker;
    private readonly objectTypeResolvers: ObjectTypeResolver[] = [];
    private propsType?: ts.Type;
    private _intrinsicElement = false;

    constructor(checker: ts.TypeChecker) {
        this.checker = checker;
        this.objectTypeResolvers.push(this.handleClassOrInterface);
        this.objectTypeResolvers.push(this.handleAnonymousOrMappedObject);
        this.objectTypeResolvers.push(this.handlePropsIntersection);

        this.objectTypeResolvers = this.objectTypeResolvers.map((typeResolver) => (typeResolver = typeResolver.bind(this)));
    }

    /**
     * Finds the props parameter of the given node in the SourceFile
     *
     * @param node JSX opening element... example <MyButton>, <div>, <AnyComponent>
     * @param sourceFile physical, parsed file by Typescript Compiler API
     */
    findPropsParamType(node: ts.JsxOpeningLikeElement, sourceFile: ts.SourceFile) {
        const checker = this.checker;
        const symbol = checker.getSymbolAtLocation(node.tagName);
        if (!symbol) {
            throw new ResolverException('Could not resolve symbol');
        }
        const nodeName = node.tagName.getText();
        const componentType = checker.getTypeOfSymbolAtLocation(symbol, node);
        const callSignatures = componentType.getCallSignatures();

        let propsParamType: ts.Type;
        if (callSignatures.length === 0) {
            const jsxNameSpace = findJsxNamespaceSymbolOrThrow(checker, sourceFile);
            const jsxExports = checker.getExportsOfModule(jsxNameSpace);
            const intrinsicElements = jsxExports.find((e) => e.name === 'IntrinsicElements');
            if (!intrinsicElements) {
                throw new ResolverException('Could not find IntrinsicElements in JSX namespace');
            }
            const members = intrinsicElements.members;
            if (!members || !members.has(nodeName as ts.__String)) {
                throw new ResolverException('Unable to find node in JSX namespace');
            }
            const symbolInJsxMemberTable = members.get(nodeName as ts.__String)!;
            propsParamType = checker.getTypeOfSymbolAtLocation(
                symbolInJsxMemberTable,
                symbolInJsxMemberTable.valueDeclaration!,
            );
            this._intrinsicElement = true;
        } else {
            const propsType = callSignatures[0].getParameters()[0];
            if (!propsType) throw new ResolverException('Props parameter not found');
            propsParamType = checker.getTypeOfSymbolAtLocation(propsType, node);
        }
        this.propsType = propsParamType;
    }

    resolveProps(): PropertyDescriptor[] {
        if (!this.propsType) {
            throw new ResolverException('Props type is not initialized');
        }
        return this.resolvePropsObject(this.propsType);
    }

    get intrinsicElement() {
        return this._intrinsicElement;
    }

    private resolvePropsObject(type: ts.Type): PropertyDescriptor[] {
        if (!this.isObject(type) && !type.isUnionOrIntersection()) {
            throw new ResolverException(`Props should be an object-typed. Found type =${describeType(type)}`);
        }
        for (let i = 0; i < this.objectTypeResolvers.length; i++) {
            const resolver = this.objectTypeResolvers[i];
            const result = resolver(type);
            if (result) {
                return result;
            }
        }
        throw new ResolverException('Unable to resolve properties');
    }

    private handlePropsIntersection(type: ts.Type): PropertyDescriptor[] | undefined {
        if (!type.isIntersection()) {
            return undefined;
        }
        return this.resolveProperties(type.getApparentProperties());
    }

    private handleClassOrInterface(type: ts.Type): PropertyDescriptor[] | undefined {
        if (!type.isClassOrInterface()) {
            return undefined;
        }
        const classOrInterface = type as ts.InterfaceType;
        const properties = classOrInterface.getApparentProperties();
        return this.resolveProperties(properties);
    }

    private handleAnonymousOrMappedObject(type: ts.Type): PropertyDescriptor[] | undefined {
        if (!(type.flags & ts.TypeFlags.Object)) {
            return undefined;
        }
        const typeAsObject = type as ts.ObjectType;
        if (!(typeAsObject.objectFlags & ts.ObjectFlags.Anonymous || typeAsObject.objectFlags & ts.ObjectFlags.Mapped)) {
            return undefined;
        }
        const properties = typeAsObject.getApparentProperties();
        return this.resolveProperties(properties);
    }

    /**
     * Resolves the given properties and returns the transformed/parsed PropertyDescriptors.
     * @param properties properties of an object
     */
    private resolveProperties(properties: ts.Symbol[]): PropertyDescriptor[] {
        return properties
            .filter(property => {
                // Filter out private/internal properties
                const name = property.getName();
                return !name.startsWith('_') && !name.startsWith('$$');
            })
            .map((property) => {
                const propertyDescriptor: PropertyDescriptor = {
                    name: property.getName(),
                    label: camelCaseToHumanReadable(property.getName()) ?? property.getName(),
                };
                const resolvedProperty = this.resolveSymbol(property);
                if (!resolvedProperty) {
                    propertyDescriptor.error = true;
                    propertyDescriptor.errorReason = 'Could not resolve property type';
                    return propertyDescriptor;
                }
                const questionToken = this.getQuestionTokenFromSymbol(property);
                if (questionToken) {
                    propertyDescriptor.optional = true;
                }
                try {
                    this.resolvePropertyType(resolvedProperty, propertyDescriptor);
                } catch (error: unknown) {
                    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
                    propertyDescriptor.errorReason = errorMessage;
                    propertyDescriptor.error = true;
                }
                return propertyDescriptor;
            });
    }

    private resolvePropertyType(resolvedProperty: ts.Type, propertyDescriptor: PropertyDescriptor) {
        const basicType = this.getBasicType(resolvedProperty);
        if (basicType === 'any') {
            propertyDescriptor.any = true;
            propertyDescriptor.basic = true;
            propertyDescriptor.tsTypeString = 'any';
        } else if (basicType === 'boolean') {
            propertyDescriptor.basic = true;
            propertyDescriptor.copilotProperty = 'BOOLEAN';
        } else if (basicType === 'number') {
            propertyDescriptor.basic = true;
            propertyDescriptor.copilotProperty = 'FLOAT';
        } else if (basicType === 'null') {
            propertyDescriptor.basic = true;
            propertyDescriptor.null = true;
            propertyDescriptor.nullOrUndefined = true;
        } else if (basicType === 'undefined') {
            propertyDescriptor.basic = true;
            propertyDescriptor.undefined = true;
            propertyDescriptor.nullOrUndefined = true;
        } else if (basicType === 'string') {
            propertyDescriptor.basic = true;
            propertyDescriptor.copilotProperty = 'STRING';
        } else if (this.isArray(resolvedProperty)) {
            propertyDescriptor.tsTypeString = 'Array';
            propertyDescriptor.array = true;
            const arrayTypes = this.resolveArrayTypeItems(resolvedProperty);
            if (arrayTypes.tsTypeString) {
                propertyDescriptor.tsTypeString = arrayTypes.tsTypeString;
            }
            propertyDescriptor.options = arrayTypes.items;
        } else if (resolvedProperty.isUnionOrIntersection()) {
            propertyDescriptor.tsTypeString = 'unionOrIntersection';
            const unionOrIntersection = resolvedProperty as ts.UnionOrIntersectionType;
            const resolved = this.resolveUnionOrIntersection(unionOrIntersection);
            if (resolved.tsTypeString) {
                propertyDescriptor.tsTypeString = resolved.tsTypeString;
            }
            propertyDescriptor.optional = propertyDescriptor.optional || resolved.null || resolved.undefined;
            propertyDescriptor.options = resolved.items;
        } else {
            propertyDescriptor.error = true;
            propertyDescriptor.errorReason = `Unable to resolve ${this.checker.typeToString(resolvedProperty)}`;
        }
        if (basicType && !propertyDescriptor.tsTypeString) {
            propertyDescriptor.tsTypeString = basicType;
        }
        if (!propertyDescriptor.copilotProperty) {
            propertyDescriptor.copilotProperty = findCopilotTypeFromBasicType(propertyDescriptor.tsTypeString);
        }
    }

    /**
     * This method resolves union typed properties.
     * There are some examples:
     *  thisUnion: "Foo" | "My" | "Bar" <-- supported
     *  thatUnion: string | undefined <-- supported
     *  unionWithNull: string | undefined | null <-- supported with the same as the one before.
     *  anotherUnion: "Foo" | string | number | undefined <-- this one should throw an exception
     * @param unionOrIntersection Any union
     * @returns
     */
    private resolveUnionOrIntersection(unionOrIntersection: ts.UnionOrIntersectionType): UnionDescriptor {
        const types = unionOrIntersection.types;
        let combinedTypes: ts.Type[] = [];
        types.forEach((k) => {
            combinedTypes.push(k);
        });
        const undefinedPresent = !!combinedTypes.find((type) => this.isUndefined(type));
        combinedTypes = combinedTypes.filter((f) => !this.isUndefined(f));
        const nullPresent = !!combinedTypes.find((type) => this.isNull(type));
        combinedTypes = combinedTypes.filter((f) => !this.isNull(f));

        // Here we removed null and undefined from the union.
        const typesDescription = combinedTypes.map((type) => this.getBasicType(type));
        this.throwIfManyBasicTypesPresent(typesDescription);
        const values = combinedTypes
            .map((type) => {
                const value = this.getAnyType(type).value;
                // We should skip boolean literals e.g. true, false from the value selection. Otherwise, it would become an item with options rather than plain boolean.
                if (type.flags & ts.TypeFlags.BooleanLiteral) {
                    return undefined;
                }
                return value;
            })
            .filter((valueProp) => !!valueProp);
        const descriptor: UnionDescriptor = {
            null: nullPresent,
            undefined: undefinedPresent,
            tsTypeString: typesDescription[0],
        };
        if (values.length === 0) {
            // Meaning that basic type is used in the union e.g. string | undefined, so we can assume this is a single value field.
            return descriptor;
        } else if (values.length > 0) {
            if (values.length !== combinedTypes.length) {
                // This is kind of combination of basic type and a value. e.g. string | "Foo"
                throw new ResolverException('Unable to recognize the union values');
            }
            descriptor.items = this.collectItems(combinedTypes);
        }
        return descriptor;
    }

    private collectItems(types: ts.Type[]): ItemOptions {
        return types
            .map((type) => {
                const anyType = this.getAnyType(type);
                if (anyType.value !== undefined && anyType.value !== null) {
                    const value = String(anyType.value);
                    return { label: value, value };
                }
                return undefined;
            })
            .filter((item): item is { label: string; value: any } => item !== undefined);
    }

    /**
     *
     * Handler for generic array object Array<'Foo' |'Test'| 'Foo2'> or Array<string>
     * @param arrayProperty
     * @returns
     */
    private resolveArrayTypeItems(arrayProperty: ts.Type): GenericArrayItemDescriptor {
        const resolvedTypes = this.getResolvedArgsForArrayType(arrayProperty);
        if (!resolvedTypes) {
            throw new ResolverException('Unable to resolve args');
        }
        let combinedTypes: ts.Type[] = [];
        resolvedTypes.forEach((resolvedType) => {
            const anyResolvedType = this.getAnyType(resolvedType);
            const types = anyResolvedType.types as ts.Type[] | undefined;
            if (types) {
                combinedTypes = [...combinedTypes, ...types];
            } else if (anyResolvedType.intrinsicName) {
                // String[] does not have types, rather they are exposed with an internal intrinsicName property.
                combinedTypes = [...combinedTypes, resolvedType];
            }
        });
        const typesDescription = combinedTypes.map((type) => this.getBasicType(type));
        const anyUndefined = typesDescription.find((l) => l === undefined);
        if (anyUndefined) {
            throw new ResolverException('Array contains one or more unrecognizable types');
        }
        this.throwIfManyBasicTypesPresent(typesDescription);
        const items = combinedTypes
            .map((type) => {
                const anyType = this.getAnyType(type);
                if (anyType.value) {
                    return { label: anyType.value, value: anyType.value };
                }
                return undefined;
            })
            .filter((item) => !!item);

        return {
            tsTypeString: typesDescription[0],
            items,
        };
    }

    private getAnyType(type: ts.Type): {
        value?: string;
        types?: ts.Type[];
        intrinsicName?: string;
        [key: string]: unknown;
    } {
        // Using a more specific return type while still accessing internal properties
        return type as {
            value?: string;
            types?: ts.Type[];
            intrinsicName?: string;
            [key: string]: unknown;
        };
    }

    /**
     * Gets the question mark of a node if exists to mark property whether it is optional.
     * @param symbol parsed symbol
     */
    private getQuestionTokenFromSymbol(symbol: ts.Symbol): ts.QuestionToken | undefined {
        const decl = symbol.valueDeclaration ?? symbol.declarations?.[0];
        if (!decl) return undefined;

        if (ts.isParameter(decl) || ts.isPropertySignature(decl) || ts.isPropertyDeclaration(decl)) {
            return decl.questionToken;
        }

        return undefined;
    }

    private isString(type: ts.Type) {
        if (type.flags & ts.TypeFlags.String) {
            return true;
        }
        if (type.flags & ts.TypeFlags.StringLiteral) {
            return true;
        }
        return false;
    }

    private isAny(type: ts.Type) {
        if (type.flags & ts.TypeFlags.Any) {
            return true;
        }
        return false;
    }

    private isUndefined(type: ts.Type) {
        if (type.flags & ts.TypeFlags.Undefined) {
            return true;
        }
        return false;
    }

    private isNumber(type: ts.Type) {
        if (type.flags & ts.TypeFlags.Number) {
            return true;
        }
        if (type.flags & ts.TypeFlags.NumberLiteral) {
            return true;
        }
        return false;
    }

    private isNull(type: ts.Type) {
        if (type.flags & ts.TypeFlags.Null) {
            return true;
        }
        return false;
    }

    private isBoolean(type: ts.Type) {
        if (type.flags & ts.TypeFlags.Boolean) {
            return true;
        }
        if (type.flags & ts.TypeFlags.BooleanLiteral) {
            return true;
        }
        // Maybe also check types contain 2 types "false" and "true" if this generates any false positives
        return false;
    }

    private isArray(type: ts.Type) {
        return this.getResolvedArgsForArrayType(type) !== undefined;
    }

    private isObject(type: ts.Type) {
        if (type.flags & ts.TypeFlags.Object) {
            return true;
        }
        return false;
    }

    private getResolvedArgsForArrayType(type: ts.Type): ts.Type[] | undefined {
        const typeWithArgs = type as { resolvedTypeArguments?: ts.Type[] };
        return typeWithArgs.resolvedTypeArguments;
    }

    private resolveSymbol(symbol: ts.Symbol): ts.Type | undefined {
        const symbolType = symbol.valueDeclaration
            ? this.checker.getTypeOfSymbolAtLocation(symbol, symbol.valueDeclaration)
            : this.checker.getTypeOfSymbol(symbol);
        if (!symbolType) {
            return undefined;
        }
        return symbolType;
    }

    private getBasicType(type: ts.Type): SupportedBasicTypes | undefined {
        if (this.isString(type)) {
            return 'string';
        } else if (this.isNumber(type)) {
            return 'number';
        } else if (this.isUndefined(type)) {
            return 'undefined';
        } else if (this.isAny(type)) {
            return 'any';
        } else if (this.isBoolean(type)) {
            return 'boolean';
        } else if (this.isNull(type)) {
            return 'null';
        }
        return undefined;
    }

    private throwIfManyBasicTypesPresent(typesDescription: Array<SupportedBasicTypes | undefined>) {
        const uniqueDescribedTypes = new Set<SupportedBasicTypes>();
        typesDescription.forEach((type) => {
            if (type) {
                uniqueDescribedTypes.add(type);
            }
        });
        if (uniqueDescribedTypes.size > 1) {
            throw new ResolverException('More than one type has been found.');
        }
    }
}

/**
 * Method that checks the given type string and returns represented ComponentPropertyType.
 * @param type that obtained from typescript compiler api.
 */
function findCopilotTypeFromBasicType(type?: string): ComponentPropertyType | undefined {
    if (!type) {
        return undefined;
    }
    if (type === 'string') {
        return 'STRING';
    } else if (type === 'number') {
        return 'FLOAT';
    } else if (type === 'boolean') {
        return 'BOOLEAN';
    }
    return undefined;
}

function camelCaseToHumanReadable(input?: string): string | undefined {
    if (!input || input.length === 0) return undefined;

    // Add space before capital letters, except at the start
    // capitalize first letter
    return input
        .replace(/([a-z\d])([A-Z])/g, '$1 $2') // E.g. isVisible → is Visible
        .replace(/([A-Z]+)([A-Z][a-z\d]+)/g, '$1 $2') // E.g. HTMLParser → HTML Parser
        .replace(/^./, (str) => str.toUpperCase())
        .trim();
}

/**
 * Utility method that helps the user to know the type of object while developing.
 * It does byte-wise operations for the given type and returns a string
 * @param type
 */
function describeType(type: ts.Type): string {
    const flags = ts.TypeFlags;
    const objFlags = ts.ObjectFlags;

    if (type.flags & flags.Any) return 'Any';
    if (type.flags & flags.Unknown) return 'Unknown';
    if (type.flags & flags.String) return 'String';
    if (type.flags & flags.Number) return 'Number';
    if (type.flags & flags.Boolean) return 'Boolean';
    if (type.flags & flags.BigInt) return 'BigInt';
    if (type.flags & flags.StringLiteral) return 'StringLiteral';
    if (type.flags & flags.NumberLiteral) return 'NumberLiteral';
    if (type.flags & flags.BooleanLiteral) return 'BooleanLiteral';
    if (type.flags & flags.BigIntLiteral) return 'BigIntLiteral';
    if (type.flags & flags.Enum) return 'Enum';
    if (type.flags & flags.EnumLiteral) return 'EnumLiteral';
    if (type.flags & flags.ESSymbol) return 'Symbol';
    if (type.flags & flags.UniqueESSymbol) return 'UniqueSymbol';
    if (type.flags & flags.Void) return 'Void';
    if (type.flags & flags.Undefined) return 'Undefined';
    if (type.flags & flags.Null) return 'Null';
    if (type.flags & flags.Never) return 'Never';
    if (type.flags & flags.NonPrimitive) return 'NonPrimitive';
    if (type.flags & flags.Object) {
        const objType = type as ts.ObjectType;

        if (objType.objectFlags & objFlags.Class) return 'Class';
        if (objType.objectFlags & objFlags.Interface) return 'Interface';
        if (objType.objectFlags & objFlags.Anonymous) return 'Anonymous Object';
        if (objType.objectFlags & objFlags.Reference) return 'Generic Instance';
        if (objType.objectFlags & objFlags.Tuple) return 'Tuple';
        if (objType.objectFlags & objFlags.Mapped) return 'Mapped Type';
        if (objType.objectFlags & objFlags.Instantiated) return 'Instantiated Type';
        if (objType.objectFlags & objFlags.ReverseMapped) return 'ReverseMapped Type';
        return 'Object';
    }
    if (type.flags & flags.Union) return 'Union';
    if (type.flags & flags.Intersection) return 'Intersection';
    if (type.flags & flags.Index) return 'Index Type';
    if (type.flags & flags.IndexedAccess) return 'IndexedAccess Type';
    if (type.flags & flags.TypeParameter) return 'Type Parameter';
    if (type.flags & flags.TypeVariable) return 'Type Variable';

    return 'Other';
}