#!/usr/bin/env node
import * as acorn from "acorn";
import { execSync } from "child_process";
import * as fs from "fs";
import * as glob from "glob";
import MagicString from "magic-string";
import { sep as pathSeparator } from "path";
import * as prettier from "prettier";
const htmlParse = require("a-node-html-parser").parse;

const assumeBooleanAttributes = ["hidden", "checked"];

// Comparison of Lit and Polymer in https://43081j.com/2018/08/future-of-polymer

const inputArg = process.argv[2];
if (!inputArg) {
  console.error(`Usage: ${process.argv[1]} <file or directory to convert>`);
  process.exit();
}

const stat = fs.lstatSync(inputArg);
let useLit1 = false;
if (process.argv.includes("-1")) {
  useLit1 = true;
}

let useOptionalChaining = false;
if (process.argv.includes("-chain")) {
  useOptionalChaining = true;
}
let outputSuffix = "";
if (process.argv.includes("-out")) {
  outputSuffix = ".out.js";
}
if (stat.isFile()) {
  convertFile(inputArg, useLit1, useOptionalChaining, outputSuffix);
} else if (stat.isDirectory()) {
  const vaadinVersion = readVaadinVersion(inputArg);
  if (vaadinVersion && vaadinVersion.startsWith("14.")) {
    useLit1 = true;
  }

  const jsFilePattern = inputArg + "/**/*.js";
  console.log("inputArg", inputArg);
  console.log("jsFiles", jsFilePattern);
  try {
    const jsFiles = glob.sync(jsFilePattern);

    jsFiles
      .filter((jsFile) => !jsFile.includes("node_modules"))
      .forEach((file) =>
        convertFile(file, useLit1, useOptionalChaining, outputSuffix)
      );
  } catch (e) {
    console.error("Error listing directory", e);
    process.exit();
  }

  // Also convert Java if the needed tools are installed
  try {
    run("mvn --version");
  } catch (e) {
    console.warn("");
    console.warn(
      "Maven (mvn) was not found. Skipping conversion of potential Java files."
    );
    console.warn("");
    process.exit();
  }

  try {
    run("java --version");
  } catch (e) {
    console.warn("");
    console.warn(
      "Java (java) was not found. Skipping conversion of potential Java files."
    );
    console.warn("");

    process.exit();
  }

  const jarVersion = "0.6.0.rc1";
  const groupId = "org.vaadin.artur";
  const artifactId = "polymer-to-lit";
  const repo =
    "-DremoteRepositories=prereleases::default::https://maven.vaadin.com/vaadin-prereleases/";
  console.log("Downloading Java dependencies if needed...");
  try {
    run(
      `mvn dependency:get -Dartifact=${groupId}:${artifactId}:${jarVersion} ${repo}`
    );
  } catch (e) {
    console.error(e);
    process.exit();
  }

  console.log("Running Java converter...");
  let jarPath = "";

  try {
    jarPath = run(
      `mvn help:evaluate -q -DforceStdout -Dexpression="settings.localRepository/${groupId.replace(
        /\./g,
        pathSeparator
      )}/${artifactId}/${jarVersion}/${artifactId}-${jarVersion}.jar"`
    );
  } catch (e) {
    console.error(e);
    process.exit();
  }

  try {
    const result = run(`java -jar "${jarPath}" "${inputArg}"`);

    console.log(result);
  } catch (e) {
    console.error(e);
  }
}

type BindingOrText = {
  type: "text" | "oneway" | "twoway";
  value: string;
};

function char(str: string, i: number): string | undefined {
  if (i >= str.length) {
    return undefined;
  }
  return str.charAt(i);
}
function splitExpression(expression: string): BindingOrText[] {
  let current: BindingOrText | undefined;
  const result: BindingOrText[] = [];

  for (var i = 0; i < expression.length; i++) {
    if (!current || current.type === "text") {
      if (char(expression, i) === "[" && char(expression, i + 1) === "[") {
        // Oneway start
        if (current) {
          result.push(current);
        }
        current = { type: "oneway", value: "" };
        i++;
      } else if (
        char(expression, i) === "{" &&
        char(expression, i + 1) === "{"
      ) {
        // Twoway start
        if (current) {
          result.push(current);
        }
        current = { type: "twoway", value: "" };
        i++;
      } else {
        if (!current) {
          current = { type: "text", value: "" };
        }
        current.value += char(expression, i);
      }
    } else {
      // Inside a binding
      if (
        current.type === "oneway" &&
        char(expression, i) === "]" &&
        char(expression, i + 1) === "]"
      ) {
        result.push(current);
        i++;
        current = undefined;
      } else if (
        current.type === "twoway" &&
        char(expression, i) === "}" &&
        char(expression, i + 1) === "}"
      ) {
        result.push(current);
        i++;
        current = undefined;
      } else {
        current.value += char(expression, i);
      }
    }
  }
  return result;
}

function convertFile(
  filename: string,
  useLit1: boolean,
  useOptionalChaining: boolean,
  outputSuffix: string
) {
  const jsInputFile = filename;
  let jsOutputFile = jsInputFile + outputSuffix;
  const jsContents = fs.readFileSync(jsInputFile, { encoding: "UTF-8" });
  if (!jsContents.includes("PolymerElement")) {
    return;
  }
  console.log("Processing " + jsInputFile);

  const tsOutput: MagicString = new MagicString(jsContents);
  const polymerJs = acorn.parse(jsContents, { sourceType: "module" });
  const initValues: any[] = [];
  const computedProperties: any[] = [];
  const observedProperties: any[] = [];
  let valueInitPosition = -1;
  let newMethodInjectPosition = -1;
  let usesRepeat = false;
  let usesHeaderRenderer = false;
  let usesBodyRenderer = false;
  let usesFooterRenderer = false;
  let usesUnsafeCss = false;

  function error(message?: any, ...optionalParams: any[]) {
    console.error("ERROR", message, optionalParams);
  }
  function warn(message?: any, ...optionalParams: any[]) {
    console.warn("WARNING", message, optionalParams);
  }
  function debug(message?: any, ...optionalParams: any[]) {
    console.log(message, optionalParams);
  }
  const body = (polymerJs as any).body;
  const modifyClass = (node: any, resolve: Resolver) => {
    const className = node.id.name;
    const parentClass = node.superClass;
    let tag = "";
    let template = "";

    newMethodInjectPosition = node.body.end - 1;

    // extends PolymerElement -> extends LitElement
    const newSuper = getSuperClass(node.superClass);
    if (!newSuper || !newSuper?.includes("LitElement")) {
      return;
    }
    if (newSuper) {
      tsOutput.overwrite(node.superClass.start, node.superClass.end, newSuper);
    }
    for (const classContent of node.body.body) {
      if (
        classContent.type == "MethodDefinition" &&
        classContent.key.name == "template"
      ) {
        // Replace the whole template() method
        const taggedTemplate = classContent.value.body.body[0].argument;
        template = taggedTemplate.quasi.quasis[0].value.raw;
        const modifiedTemplate = modifyTemplate(template);

        const html = modifiedTemplate.htmls.join("\n");
        // const css = modifiedTemplate.styles.join("\n"); //TODO
        const hasStyleIncludes = modifiedTemplate.styleIncludes.length > 0;
        const hasStyles = modifiedTemplate.styles.length > 0;

        if (hasStyleIncludes) {
          usesUnsafeCss = true;
        }
        const stylesGetter = `  static get styles() {
        ${hasStyleIncludes ? `const includedStyles = {};` : ""}
        ${modifiedTemplate.styleIncludes
          .map(
            (include) =>
              `includedStyles["${include}"] = ${nullSafe(
                `document.querySelector(
            "dom-module[id='${include}']"
            ).firstElementChild.content.firstElementChild.innerText`,
                ["document"],
                '""'
              )};`
          )
          .join("\n")}
        return [
          ${modifiedTemplate.styleIncludes
            .map((include) => `unsafeCSS(includedStyles["${include}"])`)
            .join(", ")}${modifiedTemplate.styleIncludes.length > 0 ? "," : ""}
        ${modifiedTemplate.styles.map((css) => `css\`${css}\``).join(",")}
        ];
      }`;

        const renderMethod = `render() {
        return html\`${html}\`;
              }`;
        tsOutput.overwrite(
          classContent.start,
          classContent.end,
          (hasStyles ? stylesGetter : "") + renderMethod
        );
      } else if (
        classContent.type == "MethodDefinition" &&
        classContent.key.name == "is"
      ) {
        tag = classContent.value.body.body[0].argument.value;
        // debug(getSource(classContent));
      } else if (
        classContent.type == "MethodDefinition" &&
        classContent.key.name == "_attachDom"
      ) {
        // Assume this means it it using light dom
        tsOutput.overwrite(
          classContent.start,
          classContent.end,
          `createRenderRoot() {
          // Do not use a shadow root
          return this;
        }
        `
        );
      } else if (
        classContent.type == "MethodDefinition" &&
        classContent.key.name == "ready"
      ) {
        // There is no 'ready' callback but it is approximately run at the same time as 'firstUpdated'
        // tsOutput.overwrite(classContent.key.start, classContent.key.end, "firstUpdated");
        // debug(classContent);
        const src = getSource(classContent);
        tsOutput.overwrite(
          classContent.start,
          classContent.end,
          src
            .replace("ready()", "firstUpdated(_changedProperties)")
            .replace("super.ready()", "super.firstUpdated(_changedProperties)")
        );
      } else if (
        classContent.type === "MethodDefinition" &&
        classContent.kind === "get" &&
        classContent.key.name === "properties"
      ) {
        if (
          classContent.value.type === "FunctionExpression" &&
          classContent.value.body.type == "BlockStatement"
        ) {
          const returnStatment = classContent.value.body.body;

          returnStatment[0].argument.properties.forEach((prop) => {
            // debug(prop);
            const propName = prop.key.name;

            if (prop.value.type === "Identifier") {
              // first: String
              // type of property
            } else {
              prop.value.properties.forEach((typeValue) => {
                const keyNode = typeValue.key;
                const valueNode = typeValue.value;

                if (keyNode.type === "Identifier") {
                  if (keyNode.name === "value") {
                    // Value initialization that must go into the constructor
                    if (valueNode.type === "Literal") {
                      const initValue = valueNode.raw;
                      initValues.push({
                        name: propName,
                        value: initValue,
                      });
                    } else if (valueNode.type === "FunctionExpression") {
                      if (
                        valueNode?.body?.body[0]?.type === "ReturnStatement"
                      ) {
                        const arrayExpression = getSource(
                          valueNode.body.body[0].argument
                        );
                        initValues.push({
                          name: propName,
                          value: arrayExpression,
                        });
                      }
                    } else if (valueNode.type === "ArrayExpression") {
                      initValues.push({
                        name: propName,
                        value: getSource(valueNode),
                      });
                    } else {
                      warn(
                        "UNKNOWN type of 'value' for property",
                        propName,
                        ":",
                        valueNode.type
                      );
                    }
                    removeIncludingTrailingComma(typeValue);
                  } else if (keyNode.name === "computed") {
                    computedProperties.push({
                      name: propName,
                      value: resolveExpression(valueNode.value, true, resolve),
                    });
                    removeIncludingTrailingComma(typeValue);
                  } else if (keyNode.name === "observer") {
                    const observer = valueNode.value;
                    if (observer.includes("(")) {
                      // Complex observer
                      tsOutput.prependLeft(
                        valueNode.start,
                        "/* TODO: Convert this complex observer manually */\n"
                      );
                    } else {
                      observedProperties.push({
                        name: propName,
                        value: observer,
                      });
                      removeIncludingTrailingComma(typeValue);
                    }
                  } else if (keyNode.name === "reflectToAttribute") {
                    removeIncludingTrailingComma(typeValue);
                    if (
                      valueNode.type === "Literal" &&
                      valueNode.raw === "true"
                    ) {
                      tsOutput.prependLeft(typeValue.start, "reflect: true,");
                    }
                  }
                }
              });
            }
          });
        }
      } else if (
        classContent.type === "MethodDefinition" &&
        classContent.kind === "constructor"
      ) {
        const constructorNode = classContent;
        valueInitPosition = constructorNode.value.body.end - 1;
      } else {
        // warn("Unhandled class content", classContent);
      }
      // TODO handle properties
      // static get properties() {
      //   return {
      //     myProperty: Boolean,
      //     mySecondProperty: {
      //       type: String,
      //       reflectToAttribute: true
      //     }
      //   };
      // }
      // ->
      // static get properties() {
      //   return {
      //     myProperty: { type: Boolean },
      //     mySecondProperty: {
      //       type: String,
      //       reflect: true
      //     }
      //   };
      // }
      // or
      // @property({ type: Boolean })
      // myProperty = false;
      // debug(getSource(classContent));
    }
    return { className, parentClass, template, tag };
  };

  const removeIncludingTrailingComma = (node) => {
    if (jsContents.substring(node.end, node.end + 1) === ",") {
      tsOutput.remove(node.start, node.end + 1);
    } else {
      tsOutput.remove(node.start, node.end);
    }
    // debug(
    //   getSource(node),
    //   "and then",
    //   jsContents.substring(node.end, node.end + 1+5)
    // );
    // if (jsContents.charAt(node.end+1)===',') {
    //   tsOutput.remove(node.end+1,node.end+1);
    // }
    // Fixme trailing ,
  };
  const rewriteTextNode = (node, resolver: Resolver) => {
    const bindingRe = /\[\[(.+?)\]\]/g;
    const bindingRe2 = /\{\{(.+?)\}\}/g;

    var result = node.rawText;
    result = result.replace(bindingRe, (_fullMatch, variableName) => {
      const resolved = resolveExpression(variableName, true, resolver);
      return `\${${resolved}}`;
    });
    result = result.replace(bindingRe2, (_fullMatch, variableName) => {
      const resolved = resolveExpression(variableName, true, resolver);
      return `\${${resolved}}`;
    });
    node.rawText = result;
  };

  type Resolver = (
    originalExpression: string,
    node: any,
    makeNullSafe: boolean,
    resolver: Resolver,
    undefinedValue: string,
    qualifiedPrefixes: string[]
  ) => string;

  const rewriteElement = (element: any, resolver: Resolver) => {
    if (
      element.tagName === "TEMPLATE" &&
      element.getAttribute("is") === "dom-if"
    ) {
      const polymerExpression = element.getAttribute("if");
      replaceWithLitIf(element, polymerExpression, resolver, element);
      return;
    } else if (element.tagName === "DOM-IF") {
      const template = element.childNodes.filter(
        (el) => el.tagName === "TEMPLATE"
      )[0];
      const polymerExpression = element.getAttribute("if");
      replaceWithLitIf(element, polymerExpression, resolver, template);
      return;
    } else if (element.tagName === "DOM-REPEAT") {
      const template = element.childNodes.filter(
        (el) => el.tagName === "TEMPLATE"
      )[0];
      const polymerItemsExpression = element.getAttribute("items");
      replaceWithLitRepeat(element, polymerItemsExpression, resolver, template);
      return;
    } else if (
      element.tagName === "TEMPLATE" &&
      element.getAttribute("is") === "dom-repeat"
    ) {
      const polymerItemsExpression = element.getAttribute("items");
      replaceWithLitRepeat(element, polymerItemsExpression, resolver, element);
      return;
    } else if (element.tagName === "VAADIN-GRID-COLUMN") {
      const templates = element.childNodes.filter(
        (child) => child.tagName === "TEMPLATE"
      );
      const headerTemplate = templates.find(
        (template) => template.getAttribute("class") === "header"
      );
      const bodyTemplate = templates.find(
        (template) => !template.hasAttribute("class")
      );
      const footerTemplate = templates.find(
        (template) => template.getAttribute("class") === "footer"
      );

      if (headerTemplate) {
        usesHeaderRenderer = true;
        headerTemplate.childNodes.forEach((child) =>
          rewriteHtmlNode(child, resolver)
        );

        element.setAttribute(
          `\${columnHeaderRenderer(
        (column) =>
          html\`${headerTemplate.innerHTML}\`
      )}
      `,
          ""
        );
        headerTemplate.remove();
      }
      if (bodyTemplate) {
        usesBodyRenderer = true;
        const itemResolver: Resolver = (
          originalExpression,
          expr,
          makeNullSafe,
          resolver,
          undefinedValue
        ) => {
          if (originalExpression.startsWith("item.")) {
            return makeNullSafe
              ? nullSafe(originalExpression, ["item", "this"], undefinedValue)
              : originalExpression;
          }

          return resolver(
            originalExpression,
            expr,
            true,
            itemResolver,
            undefinedValue,
            ["this"]
          );
        };
        bodyTemplate.childNodes.forEach((child) =>
          rewriteHtmlNode(child, itemResolver)
        );

        element.setAttribute(
          `\${columnBodyRenderer(
        (item) =>
          html\`${bodyTemplate.innerHTML}\`
      )}
      `,
          ""
        );
        bodyTemplate.remove();
      }
      if (footerTemplate) {
        usesFooterRenderer = true;
        footerTemplate.childNodes.forEach((child) =>
          rewriteHtmlNode(child, resolver)
        );

        element.setAttribute(
          `\${columnFooterRenderer(
        (column) =>
          html\`${footerTemplate.innerHTML}\`
      )}
      `,
          ""
        );
        footerTemplate.remove();
      }
    } else if (element.attributes) {
      // TODO rewrite input checked="[[checked]]" => ?checked=${this.checked}
      for (const key of Object.keys(element.attributes)) {
        const value = element.attributes[key];

        if (key.startsWith("on-")) {
          const eventName = key.substring(3);
          const eventHandler = value;
          element.removeAttribute(key);
          element.setAttribute("@" + eventName, `\${this.${eventHandler}}`);
        } else if (
          (value.includes("[[") && value.includes("]]")) ||
          (value.includes("{{") && value.includes("}}"))
        ) {
          // Expression can be either "[[foo]]"" or a combined expression like "{{helloText}} [[worldText]]"

          const expressions = splitExpression(value);

          if (key.endsWith("$")) {
            // attribute binding prop$="[[foo]]" => prop=${this.foo}
            let attributeKey = key.substring(0, key.length - 1);
            if (attributeKey.endsWith("\\")) {
              attributeKey = attributeKey.substring(0, attributeKey.length - 1);
            }

            if (assumeBooleanAttributes.includes(attributeKey)) {
              attributeKey = "?" + attributeKey;
            }
            element.setAttribute(
              attributeKey,
              "${" + resolveExpressions(expressions, true, resolver) + "}"
            );
            element.removeAttribute(key);
          } else {
            // property binding prop="[[foo]]" => .prop=${this.foo}
            // prop="[[!and(property1, property2)]]" => .prop=${!this.and(this.property1, this.property2)}
            // debug("Rewrite prop: ", key, value);
            element.setAttribute(
              "." + key,
              "${" + resolveExpressions(expressions, true, resolver) + "}"
            );
            element.removeAttribute(key);
          }

          if (expressions.length === 1 && expressions[0].type === "twoway") {
            // @value-change=${(e) => (this.name = e.target.value)}
            const eventName = key + "-changed";
            const attributeKey = "@" + eventName;
            const attributeValue = `\${(e) => (${resolveExpressions(
              expressions,
              false,
              resolver
            )} = e.target.value)}`;
            element.setAttribute(attributeKey, attributeValue);
          }
        }
      }
    }

    //
    for (const child of element.childNodes) {
      rewriteHtmlNode(child, resolver);
    }
  };
  const modifyTemplate = (inputHtml) => {
    //  if (inputHtml.includes("}}")) {
    //   throw "Template contains two way bindings which are not supported";
    // }
    const root = htmlParse(inputHtml, {
      lowerCaseTagName: true,
      script: true,
      style: true,
      pre: true,
      comment: true,
    });
    // debug(root.innerHTML);
    const htmls: string[] = [];
    const styles: string[] = [];
    const styleIncludes: string[] = [];
    for (const child of root.childNodes) {
      // debug(child);
      if (child.tagName == "CUSTOM-STYLE" || child.tagName == "STYLE") {
        let style;
        if (child.tagName == "CUSTOM-STYLE") {
          style = child.childNodes[1];
        } else {
          style = child;
        }
        const includes: string = style.getAttribute("include");
        if (includes) {
          styleIncludes.push(...includes.split(" "));
        }
        const css = style.innerHTML;
        styles.push(css);
        child.remove();
      } else {
        rewriteHtmlNode(child, baseResolver);
        // if (child.nodeType === 1) {
        //   htmls.push(child.outerHTML);
        // } else {
        //   htmls.push(child.rawText);
        // }
        // TODO  href="mailto:[[item.email]]"
        // TODO <template> tags
      }
    }
    htmls.push(root.innerHTML);

    const ret = { htmls, styles, styleIncludes };
    //debug(ret);
    return ret;
  };

  const getSource = (node: any) => {
    return jsContents.substring(node.start, node.end);
  };
  const skipImports = [
    "@polymer/polymer/polymer-element.js",
    "@polymer/polymer/lib/utils/html-tag.js",
  ];
  for (const node of body) {
    if (node.type === "ClassDeclaration") {
      modifyClass(node, baseResolver);
    } else if (node.type === "ImportDeclaration") {
      removeImport(node, "html", "PolymerElement");
      removeImport(node, "@polymer/polymer/lib/elements/dom-if.js");
      removeImport(node, "@polymer/polymer/lib/elements/dom-repeat.js");
    } else if (node.type === "ExportNamedDeclaration") {
      // export class foobar
      if (node.declaration.type === "ClassDeclaration") {
        modifyClass(node.declaration, baseResolver);
      } else {
        warn("Unhandled root node", node.type, getSource(node));
      }
    } else if (
      !getSource(node).includes("customElements.define") &&
      !getSource(node).includes("window.Vaadin.")
    ) {
      warn("Unhandled root node", node.type, getSource(node));
    }
  }

  const litImport = useLit1 ? "lit-element" : "lit";
  tsOutput.prepend(`import { html, LitElement, css } from "${litImport}";\n`);
  if (usesRepeat) {
    tsOutput.prepend(`import { repeat } from "lit/directives/repeat.js";\n`);
  }
  if (usesUnsafeCss) {
    tsOutput.prepend(`import { unsafeCSS } from "${litImport}";\n`);
  }

  const usedRenderers: string[] = [];
  if (usesBodyRenderer) usedRenderers.push("columnBodyRenderer");
  if (usesFooterRenderer) usedRenderers.push("columnFooterRenderer");
  if (usesHeaderRenderer) usedRenderers.push("columnHeaderRenderer");
  if (usedRenderers.length !== 0) {
    tsOutput.prepend(
      `import { ${usedRenderers.join(", ")} } from "@vaadin/grid/lit.js";\n`
    );
  }

  const valueInitCode = initValues
    .map((initValue) => {
      return `this.${initValue.name} = ${initValue.value};`;
    })
    .join("\n");

  const computedPropertiesCode = computedProperties
    .map((computedProperty) => {
      return `get ${computedProperty.name}() {
      return ${computedProperty.value};
    }`;
    })
    .join("\n");

  const observedPropertiesCode = observedProperties
    .map((observedProperty) => {
      const variable = observedProperty.name;
      const observer = observedProperty.value;
      return `set ${variable}(newValue) {
      const oldValue = this.${variable};
      this._${variable} = newValue;
      if (oldValue !== newValue) {
        this.${observer}(newValue, oldValue);
        this.requestUpdateInternal("${variable}", oldValue, this.constructor.properties.${variable});
      }
    }
    get ${variable}() {
      return this._${variable};
    }
  `;
    })
    .join("\n");
  if (valueInitCode.length != 0) {
    if (valueInitPosition !== -1) {
      tsOutput.prependRight(valueInitPosition, valueInitCode);
    } else {
      // No constructor
      const constructorCode = `constructor() {
        super();
        ${valueInitCode}
      }`;
      tsOutput.prependRight(newMethodInjectPosition, constructorCode);
    }
  }

  if (observedPropertiesCode.length != 0) {
    tsOutput.prependRight(newMethodInjectPosition, observedPropertiesCode);
  }
  if (computedPropertiesCode.length != 0) {
    tsOutput.prependRight(newMethodInjectPosition, computedPropertiesCode);
  }

  function resolveExpressions(
    expressions: BindingOrText[],
    makeNullSafe: boolean,
    resolver: Resolver,
    undefinedValue: string = "undefined",
    qualifiedPrefixes: string[] = ["this"]
  ) {
    // result is used inside ${ }
    // abc -> "abc"
    // [[abc]] -> this.abc
    // abc[[abc]] -> "abc"+this.abc

    if (expressions.length === 1) {
      return resolveExpression(
        expressions[0].value,
        makeNullSafe,
        resolver,
        undefinedValue,
        qualifiedPrefixes
      );
    }

    const resolved = expressions.map((bindingOrText) => {
      if (bindingOrText.type === "text") {
        return `'${bindingOrText.value}'`;
      } else {
        return (
          "(" +
          resolveExpression(
            bindingOrText.value,
            makeNullSafe,
            resolver,
            "''", // Use "" so concatenation does not write 'undefined'. Maybe it should be used for other bindings also...
            qualifiedPrefixes
          ) +
          ")"
        );
      }
    });

    const result = resolved.join("+");
    return result;
  }
  function resolveExpression(
    expression: string,
    makeNullSafe: boolean,
    resolver: Resolver,
    undefinedValue: string = "undefined",
    qualifiedPrefixes: string[] = ["this"]
  ) {
    try {
      const expr: any = (acorn.parse(expression) as any).body[0];
      // debug("resolveExpression", expression); //, inspect(expr, { depth: null }));

      return resolver(
        expression,
        expr,
        makeNullSafe,
        resolver,
        undefinedValue,
        qualifiedPrefixes
      );
    } catch (e) {
      throw new Error("Unable to parse expression: " + expression);
    }
  }
  function baseResolver(
    originalExpression: string,
    expr: any,
    makeNullSafe: boolean,
    resolve: Resolver,
    undefinedValue: string,
    qualifiedPrefixes: string[]
  ) {
    // debug("thisResolver", expr.type);
    if (expr.type === "ExpressionStatement") {
      return resolve(
        originalExpression,
        expr.expression,
        makeNullSafe,
        resolve,
        undefinedValue,
        qualifiedPrefixes
      );
    } else if (expr.type === "MemberExpression") {
      const result = prependThisIfNeeded(
        qualifiedPrefixes,
        originalExpression.substring(expr.start, expr.end)
      );

      return makeNullSafe
        ? nullSafe(result, qualifiedPrefixes, undefinedValue)
        : result;
    } else if (expr.type === "Literal") {
      return expr.raw;
    } else if (expr.type === "Identifier") {
      const result = prependThisIfNeeded(qualifiedPrefixes, expr.name);
      return makeNullSafe
        ? nullSafe(result, qualifiedPrefixes, undefinedValue)
        : result;
    } else if (expr.type === "UnaryExpression") {
      return (
        expr.operator +
        "(" +
        resolve(
          originalExpression,
          expr.argument,
          makeNullSafe,
          resolve,
          undefinedValue,
          qualifiedPrefixes
        ) +
        ")"
      );
    } else if (expr.type === "CallExpression") {
      const args = expr.arguments
        .map((argument) =>
          resolve(
            originalExpression,
            argument,
            makeNullSafe,
            resolve,
            undefinedValue,
            qualifiedPrefixes
          )
        )
        .join(", ");
      const caller = resolve(
        originalExpression,
        expr.callee,
        makeNullSafe,
        resolve,
        undefinedValue,
        qualifiedPrefixes
      );
      const retval = "(" + caller + ")(" + args + ")";
      // debug("Caller", caller);
      // debug("Call ret", retval);
      return retval;
    }
    // debug(expr);
    const retval = originalExpression.substring(expr.start, expr.end);
    warn("Unresolved expression", expr, "returning", retval);
    return retval;
  }

  let output = tsOutput.toString();
  // this.$['ordersGrid'] -> this.renderRoot.querySelector("#ordersGrid")
  output = output.replace(
    /this.\$\[['"]([^;., ()]*['"]\])/g,
    `this.renderRoot.querySelector("#$1")`
  );
  output = output.replace(
    /this\.\$\.([^;., ()]*)/g,
    `this.renderRoot.querySelector("#$1")`
  );
  const prettified = prettier.format(output, {
    parser: "typescript",
  });
  fs.writeFileSync(jsOutputFile, prettified);
  function removeImport(node: any, ...identifiersOrFrom: string[]) {
    const remove: any[] = [];
    if (identifiersOrFrom.includes(node.source.value)) {
      tsOutput.remove(node.start, node.end);
      return;
    }
    node.specifiers.forEach((specifier) => {
      if (identifiersOrFrom.includes(specifier?.imported?.name)) {
        remove.push(specifier);
      }
    });
    if (remove.length === 0) {
      return;
    }
    if (remove.length === node.specifiers.length) {
      // Remove all
      tsOutput.remove(node.start, node.end);
    } else {
      error("Unable to remove only part of an import");
      //FIXME Broken
      remove.forEach((specifier) => removeIncludingTrailingComma(specifier));
    }
  }
  function rewriteHtmlNode(child: any, resolver: Resolver) {
    if (child.nodeType === 1) {
      rewriteElement(child, resolver);
    } else if (child.nodeType === 3) {
      rewriteTextNode(child, resolver);
    } else {
      warn("unhandled child", child);
    }
  }
  function replaceWithLitIf(
    element: any,
    polymerExpression: string,
    resolver: Resolver,
    template: any
  ) {
    const expression = polymerExpression.substring(
      2,
      polymerExpression.length - 2
    );

    const litExpression = resolveExpression(expression, true, resolver);
    template.childNodes.forEach((child) => rewriteElement(child, resolver));
    const litIf = `\${${litExpression} ? html\`${template.innerHTML}\` : html\`\`}`;
    element.replaceWith(litIf);
  }

  function replaceWithLitRepeat(
    element: any,
    polymerItemsExpression: string,
    outerResolver: Resolver,
    template: any
  ) {
    const expression = polymerItemsExpression.substring(
      2,
      polymerItemsExpression.length - 2
    );

    // debug("expression", expression);
    const litExpression = resolveExpression(
      expression,
      true,
      outerResolver,
      "[]"
    );
    // debug("litExpression", litExpression);

    const itemResolver = (
      originalExpression: string,
      node: any,
      makeNullSafe: boolean,
      resolver: Resolver,
      undefinedValue: string,
      qualifiedPrefixes
    ) => {
      return resolver(
        originalExpression,
        node,
        makeNullSafe,
        outerResolver,
        undefinedValue,
        [...qualifiedPrefixes, "item", "index"]
      );
    };

    template.childNodes.forEach((child) => rewriteElement(child, itemResolver));

    const litRepeat = `\${(${litExpression}).map((item, index) => html\`${template.innerHTML}\`)}`;
    element.replaceWith(litRepeat);
  }
  function nullSafe(
    name: any,
    assumedNonNull: string[],
    undefinedValue: string
  ) {
    // debug("nullSafe", name);
    // Polymer allows using "a.b.c" when "a" or "b" is undefined
    // webpack 4 does not support ?. so to be compati
    if (useOptionalChaining) {
      const first = name.split(".")[0];
      if (assumedNonNull.includes(first)) {
        return (
          first + "." + name.substring(first.length + 1).replace(/\./g, "?.")
        );
      }
      return name.replace(/\./g, "?.");
    } else {
      // this.a -> this.a
      // this.a.b -> (this.a) ? this.a.b : undefined
      // this.a.b.c -> (this.a && this.a.b) ? this.a.b.c : undefined
      // item.foo -> (item) ? item.foo
      // item.foo.bar -> (item && item.foo) ? item.foo.bar : undefined

      // debug("nullSafe", name, 'using undef',undefinedValue);

      const parts = name.split(".");

      let condition = "";
      let lastPart = parts.length - 1;
      if (undefinedValue !== "undefined") {
        // If using something else than undefined, we should check also the last part of the value and fall back if that is undefined
        // e.g. item.list -> fallback if `list` is undefined
        lastPart++;
      }

      for (var i = 1; i <= lastPart; i++) {
        let accessor = parts[0];

        for (var j = 1; j < i; j++) {
          accessor += "." + parts[j];
        }

        if (condition !== "") {
          condition += " && ";
        }

        if (!assumedNonNull.includes(accessor)) {
          condition += accessor;
        }
      }
      if (condition) {
        const ret = `(${condition}) ? ${name} : ${undefinedValue}`;
        // debug(ret);
        return ret;
      } else {
        return name;
      }
    }
  }
  function prependThisIfNeeded(qualifiedPrefixes: string[], variable: string) {
    const parts = variable.split(/\./);
    if (qualifiedPrefixes.includes(parts[0])) {
      return variable;
    }
    return "this." + variable;
  }
  function getSuperClass(superClass: any): string | undefined {
    if (superClass.type === "CallExpression") {
      // commonly used for mixins
      return getSource(superClass).replace("PolymerElement", "LitElement");
    } else if (superClass.type === "Identifier") {
      return "LitElement";
    } else {
      warn("Unknown super class type", superClass);
    }
    return undefined;
  }
}
function run(cmd: string) {
  //  console.log("Running", cmd);
  return execSync(cmd, { encoding: "utf-8" });
}
function readVaadinVersion(projectFolder: string): string | undefined {
  const pomFile = projectFolder + pathSeparator + "pom.xml";
  if (!fs.existsSync(pomFile)) {
    return undefined;
  }

  const pomXml = fs.readFileSync(pomFile, { encoding: "utf-8" });
  const match = pomXml.match(/<vaadin.version>(.*?)<\/vaadin.version>/);
  if (match) {
    return match[1];
  }
  return undefined;
}
