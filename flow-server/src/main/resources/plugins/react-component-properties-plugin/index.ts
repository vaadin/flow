import type { Plugin } from 'vite';
import type { Program } from 'typescript';
import ts from 'typescript';
import * as path from 'path';
import * as fs from "node:fs";

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
/**
* A Vite plugin that statically analyzes TSX files to discover JSX elements and
* their `props` types, then injects runtime code to expose those properties for
    * debugging / tooling.
*
* How it works (build-time):
* 1) For each `.tsx` module (excluding anything under `/generated/`), the plugin:
*    - Loads the TypeScript Program using the project's tsconfig.
*    - Parses the source file and collects distinct JSX opening elements.
*    - For each element, resolves the props type via a TypeScript TypeChecker (Resolver).
*    - Serializes the resolved properties into a `ResponseData` payload.
*
* How it works (runtime, via injected code):
* 2) Adds a global registry under `window.Vaadin.copilot.ReactProperties`:
*    - `properties: Record<string, ResponseData>` – in-memory map of tagName → ResponseData.
*    - `registerer(tagName: string, value: ResponseData)` – helper to populate `properties`.
* 3) For non-intrinsic elements (custom React components), the plugin tries to attach
*    the payload directly to the component function/object on `__debugProperties`
*    (mimicking a "Fiber-adjacent" location) for quick introspection:
    *
*       MyComponent.__debugProperties = { error: false, properties: [...] }
    *
    *    If that attachment isn’t possible (e.g., `MyComponent` isn't resolvable as a value),
*    it falls back to `registerer("MyComponent", payload)`.
*
* 4) For intrinsic elements (e.g., 'div', 'button'), the plugin always registers to the
*    global registry via `registerer("div", payload)`.
*
* Returned Data Shape:
    * --------------------
* interface ResponseData {
*   error: boolean;
*   errorMessage?: string;
*   properties?: PropertyDescriptor[];
* }
*
* `PropertyDescriptor` is the shape returned by your Resolver. Typically, this includes
* the prop name, type information, optional/required flags, default value info, etc.
*
* Runtime Globals (created if absent):
* ------------------------------------
* window.Vaadin.copilot.ReactProperties = {
    *   properties: Record<string, ResponseData>,
    *   registerer(tagName: string, value: ResponseData): void
    * }
 */
export default function reactComponentPropertiesPlugin(): Plugin {
    return {
        name: 'vaadin-react-component-properties-plugin',
        enforce: 'pre',
        transform(code, id) {
            const [bareId] = id.split('?');
            if (!bareId.endsWith('.tsx')) {
                return;
            }
            if (id.indexOf('/generated/') !== -1) {
                return;
            }
            let program: Program | undefined = undefined;
            try {
                const {parsed, host, rootNames, reactTypeMode} = resolveTsConfigAndGetParsed();
                program = ts.createProgram({
                    rootNames,
                    options: parsed.options,
                    host
                });
                console.error("React type mode:", reactTypeMode, "jsx:", parsed.options.jsx, "lib:", parsed.options.lib);
            } catch (e) {
                console.error('Failed to parse program file:', e);
                return;
            }
            if (!program) {
                return;
            }
            const sourceFile = program.getSourceFile(id);
            if (!sourceFile) {
                return;
            }
            const typeChecker = program.getTypeChecker();
            const distinctJsxOpeningLikeElements = getDistinctJsxOpeningLikeElements(sourceFile);

            let injectCode = `
        window.Vaadin = window.Vaadin || {};
        window.Vaadin.copilot = window.Vaadin.copilot || {};
        window.Vaadin.copilot.ReactProperties = window.Vaadin.copilot.ReactProperties || {};
        window.Vaadin.copilot.ReactProperties.properties = window.Vaadin.copilot.ReactProperties.properties || {};
        if(!window.Vaadin.copilot.ReactProperties.registerer){
          window.Vaadin.copilot.ReactProperties.registerer = function (tagName, value) {
            const properties = window.Vaadin.copilot.ReactProperties.properties;
            properties[tagName] = value;
          }
        }
        `;
            const responseDataStringBuilder = (
                error: boolean,
                errorMessage?: string,
                properties?: PropertyDescriptor[],
            ): string => {
                const responseData: ResponseData = { error, errorMessage, properties };
                return JSON.stringify(responseData);
            };
            const registererStringBuilder = (nodeName: string, value: string) => {
                return `window.Vaadin.copilot.ReactProperties.registerer("${nodeName}", ${value});\n`;
            };
            const fiberInjectionStringBuilder = (nodeName: string, responseDataStr: string) => {
                return `
            var ${nodeName}Props = ${responseDataStr};\n
            if(typeof ${nodeName} === 'object' || typeof ${nodeName} === 'function') {
              ${nodeName}.__debugProperties=${nodeName}Props;\n
            }else{
              ${registererStringBuilder(nodeName, `${nodeName}Props`)}
            } \n `;
            };
            distinctJsxOpeningLikeElements.forEach((node) => {
                const nodeName = node.tagName.getText();
                try {
                    const resolver = new Resolver(typeChecker, program);
                    resolver.findPropsParamType(node, sourceFile);
                    const propertyDescriptors = resolver.resolveProps();
                    const responseDataStr = responseDataStringBuilder(false, undefined, propertyDescriptors);
                    if (!resolver.intrinsicElement) {
                        injectCode += fiberInjectionStringBuilder(nodeName, responseDataStr);
                    } else {
                        injectCode += registererStringBuilder(nodeName, responseDataStr);
                    }
                } catch (e: any) {
                    const responseDataStr = responseDataStringBuilder(true, e.message);
                    injectCode += registererStringBuilder(nodeName, responseDataStr);
                    console.debug(e);
                }
            });

            return {
                code: code + injectCode,
                map: null,
            };
        },
    };
}



function resolveReactTypeMode(projectRoot: string): "DT" | "BUNDLED" | "MISSING" {
    try {
        path.require.resolve("@types/react/package.json", { paths: [projectRoot] });
        return "DT"; // DefinitelyTyped: React 18 style
    } catch {}
    try {
        const reactPkgPath = path.require.resolve("react/package.json", { paths: [projectRoot] });
        const pkgJson = JSON.parse(fs.readFileSync(reactPkgPath, "utf8"));
        if (pkgJson.types || pkgJson.typings) return "BUNDLED"; // React 19 style
    } catch {}
    return "MISSING";
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

type ParsedWithHost = {
    parsed: ts.ParsedCommandLine;
    host: ts.CompilerHost;
    rootNames: string[];
    shimPath: string;
    reactTypeMode: "DT" | "BUNDLED" | "MISSING";
};

/**
 * Parses tsconfig.json and returns a ParsedCommandLine with React/DOM typings ensured.
 * - Injects a virtual "__virtual__/react-types.d.ts" that references React types + DOM lib.
 * - Forces compilerOptions.types to include "react" and "react-dom" (works for React 18 & 19).
 * - Appends the virtual shim to `fileNames` so the Program always loads JSX namespace.
 */
export function resolveTsConfigAndGetParsed(): ParsedWithHost {
    const cwd = ts.sys.getCurrentDirectory();
    const configPath = ts.findConfigFile(cwd, ts.sys.fileExists, "tsconfig.json");
    if (!configPath) throw new Error("tsconfig.json not found.");

    const { config, error } = ts.readConfigFile(configPath, ts.sys.readFile);
    if (error) {
        throw new Error(ts.flattenDiagnosticMessageText(error.messageText, "\n"));
    }
    const projectRoot = path.dirname(configPath);
    const parsed = ts.parseJsonConfigFileContent(config, ts.sys, projectRoot, {}, configPath);
    parsed.options.jsx ??= ts.JsxEmit.ReactJSX;
    if (!parsed.options.lib?.some(l => String(l).toUpperCase().includes("DOM"))) {
        parsed.options.lib = [...(parsed.options.lib ?? []), "DOM" as any];
    }
    if (Array.isArray(parsed.options.types)) {
        const set = new Set(parsed.options.types);
        set.add("react");
        parsed.options.types = Array.from(set);
    }

    const reactTypeMode = resolveReactTypeMode(projectRoot);

    const shimPath = path.resolve(projectRoot, "__virtual__/react-types.d.ts");
    const shimContent = (() => {
        switch (reactTypeMode) {
            case "DT":
                // React 18: pulls @types/react (includes global JSX)
                return `/// <reference types="react" />\n/// <reference lib="dom" />\nexport {};\n`;
            case "BUNDLED":
                // React 19: import the package types so global JSX is applied
                return `/// <reference lib="dom" />\nimport type "react";\nexport {};\n`;
            default:
                // fallback to empty string
                console.error('React type mode is not DT or BUNDLED fallback to nothing');
                return '';
        }
    })();

    const rootNames = parsed.fileNames.includes(shimPath)
        ? parsed.fileNames
        : [...parsed.fileNames, shimPath];

    // Host that serves the virtual shim
    const host = ts.createCompilerHost(parsed.options);
    const norm = (p: string) =>
        (ts.sys.useCaseSensitiveFileNames ? path.resolve(p) : path.resolve(p).toLowerCase());
    const SHIM = norm(shimPath);

    const origReadFile = host.readFile?.bind(host);
    host.readFile = (fileName: string) => {
        if (norm(fileName) === SHIM) return shimContent;
        return origReadFile ? origReadFile(fileName) : ts.sys.readFile(fileName);
    };

    const origFileExists = host.fileExists?.bind(host);
    host.fileExists = (fileName: string) => {
        if (norm(fileName) === SHIM) return true;
        return origFileExists ? origFileExists(fileName) : ts.sys.fileExists(fileName);
    };

    const origGetSourceFile = host.getSourceFile?.bind(host);
    host.getSourceFile = (fileName, languageVersion, onError, shouldCreateNewSourceFile) => {
        if (norm(fileName) === SHIM) {
            return ts.createSourceFile(fileName, shimContent, languageVersion, /*setParent*/ true);
        }
        return origGetSourceFile
            ? origGetSourceFile(fileName, languageVersion, onError, shouldCreateNewSourceFile)
            : undefined;
    };

    return { parsed, host, rootNames, shimPath, reactTypeMode };
}
/**
 * Finds the JSX namespace or throws exception
 * @param checker TS checker to get Symbols
 * @param sourceFile
 */
export function findJsxNamespaceSymbolOrThrow(
    checker: ts.TypeChecker,
    program: ts.Program,
    sourceFile?: ts.SourceFile
): ts.Symbol {
    // 1) Prefer to resolve without a "location" to avoid scope restrictions
    const wideFlags =
        ts.SymbolFlags.All | // cast wide; covers namespace/module/interface/type/etc.
        ts.SymbolFlags.Namespace |
        ts.SymbolFlags.Module |
        ts.SymbolFlags.Interface |
        ts.SymbolFlags.Type;

    let jsxSym =
        checker.resolveName("JSX", /*location*/ undefined, wideFlags, /*excludeGlobals*/ false) ||
        (sourceFile
            ? checker.resolveName("JSX", sourceFile, wideFlags, /*excludeGlobals*/ false)
            : undefined);

    // 2) Fallback: scan in-scope symbols (helps some host setups)
    if (!jsxSym && sourceFile) {
        const inScope = checker.getSymbolsInScope(
            sourceFile,
            ts.SymbolFlags.Namespace | ts.SymbolFlags.Module
        );
        jsxSym = inScope.find((s) => s.escapedName === "JSX" as ts.__String);
    }

    if (!jsxSym) {
        // Give an actionable error—most failures are due to missing DOM/React types.
        const opts = program.getCompilerOptions();
        const msg =
            `JSX namespace not found.\n` +
            `Ensure your Program loads React + DOM types. Quick checks:\n` +
            `  - compilerOptions.jsx: ${opts.jsx ?? "undefined"} (should be ReactJSX/ReactJSXDev)\n` +
            `  - compilerOptions.lib: ${JSON.stringify(opts.lib ?? [])} (should include "DOM")\n` +
            `  - compilerOptions.types: ${JSON.stringify(opts.types ?? [])} (should include "react" on React 18; remove @types/react on React 19)`;
        throw new Error(msg);
    }
    return jsxSym;
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
    private readonly program: ts.Program;
    private readonly objectTypeResolvers: ObjectTypeResolver[] = [];
    private propsType?: ts.Type;
    private _intrinsicElement = false;

    constructor(checker: ts.TypeChecker, program: ts.Program) {
        this.checker = checker;
        this.program = program;
        this.objectTypeResolvers.push(this.handleClassOrInterface);
        this.objectTypeResolvers.push(this.handleAnonymousOrMappedObject);
        this.objectTypeResolvers.push(this.handlePropsIntersection);

        this.objectTypeResolvers = this.objectTypeResolvers.map((typeResolver) => (typeResolver = typeResolver.bind(this)));
    }

    /**
     * Finds the props parameter of the given node in the SourceFile
     *
     * @param node JSX opening element... example <MyButton>, <div>, <AnyComponent>
     * @param source that describes where node is present in the SourceFile
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
            const jsxNameSpace = findJsxNamespaceSymbolOrThrow(checker, this.program, sourceFile);
            console.log(jsxNameSpace);
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
    private resolveProperties(properties: ts.Symbol[]) {
        return properties.map((property) => {
            const propertyDescriptor: PropertyDescriptor = {
                name: property.getName(),
                label: camelCaseToHumanReadable(property.getName()) ?? property.getName(),
            };
            const resolvedProperty = this.resolveSymbol(property);
            if (!resolvedProperty) {
                propertyDescriptor.error = true;
                return propertyDescriptor;
            }
            const questionToken = this.getQuestionTokenFromSymbol(property);
            if (questionToken) {
                propertyDescriptor.optional = true;
            }
            try {
                this.resolvePropertyType(resolvedProperty, propertyDescriptor);
            } catch (error: any) {
                propertyDescriptor.errorReason = error.message;
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
                if (anyType.value) {
                    return { label: anyType.value, value: anyType.value };
                }
                return undefined;
            })
            .filter((item) => !!item);
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

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    private getAnyType(type: ts.Type): any {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        return type as any;
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

    private getResolvedArgsForArrayType(type: ts.Type) {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        return (type as any).resolvedTypeArguments as ts.Type[] | undefined;
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
    if (!input) return undefined;

    // Add space before capital letters, except at the start
    // capitalize first letter
    return input
        .replace(/([a-z\d])([A-Z])/g, '$1 $2') // E.g. isVisible → is Visible
        .replace(/([A-Z]+)([A-Z][a-z\d]+)/g, '$1 $2') // E.g. HTMLParser → HTML Parser
        .replace(/^./, (str) => str.toUpperCase());
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
