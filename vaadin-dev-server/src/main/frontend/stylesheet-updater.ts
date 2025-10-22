/*
 * Copyright 2000-2025 Vaadin Ltd.
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
 *
 */

/** StyleSheetLike allows us to also pass pseudo-stylesheets for nested at-rules */
type StyleSheetLike = CSSStyleSheet | { cssRules?: CSSRuleList; href?: string | null };

type AffectedStyleSheetRule = { parentSheet: CSSStyleSheet; rule: CSSImportRule; ruleIndex: number };

/**
 * Reloads all stylesheets elements that directly or transitively reference the given resource.
 * This includes CSS imports, font references, and image URLs.
 * @param resourcePath - The path to the resource (CSS file, image, font, etc.)
 * @returns A promise that resolves when all stylesheets have been updated
 */
export async function updateStylesheetsReferencingResource(resourcePath: string): Promise<void> {
  const normalizedResourcePath = normalizePath(resourcePath);
  const documentStyleSheets = collectDocumentStyleSheets();
  const visited = new Set<string>();
  const affectedImports: Array<AffectedStyleSheetRule> = [];
  const rootStyleSheets = new Set<CSSStyleSheet>();

  // Scan all document stylesheets
  for (const [href, styleSheet] of documentStyleSheets) {
    if (href && normalizePath(href) === normalizedResourcePath && styleSheet.ownerNode instanceof HTMLLinkElement) {
      // Resource references a linked stylesheet, update it immediately
      styleSheet.ownerNode.href = cacheKiller(href);
      rootStyleSheets.add(styleSheet);
    } else {
      await updateAndCollectAffectedImports(
        documentStyleSheets,
        styleSheet,
        normalizedResourcePath,
        visited,
        affectedImports
      );
    }
  }

  // Update all affected imports
  for (const { parentSheet, rule, ruleIndex } of affectedImports) {
    updatedImportedStyleSheet(parentSheet, rule, ruleIndex);
    rootStyleSheets.add(findRootStyleSheet(parentSheet));
  }

  if (rootStyleSheets.size == 0) {
    return;
  }

  console.log(`Reloading ${rootStyleSheets.size} stylesheet(s) referencing ${resourcePath}`);

  // Ensure all modified stylesheets are refreshed and applied by temporarily change the media type
  rootStyleSheets.forEach((rootStyleSheet) => {
    const originalMedia = rootStyleSheet.media.mediaText;
    rootStyleSheet.media.mediaText += ' vaadin';
    // The little delay makes sure that the browser has time to apply the updated styles
    setTimeout(() => {
      rootStyleSheet.media.mediaText = originalMedia;
    }, 10);
  });
}

function findRootStyleSheet(styleSheet: CSSStyleSheet): CSSStyleSheet {
  let rootStyleSheet = styleSheet;
  while (rootStyleSheet.parentStyleSheet) {
    rootStyleSheet = rootStyleSheet.parentStyleSheet;
  }
  return rootStyleSheet;
}

/**
 * Recursively scans a stylesheet to find all import rules that reference the target resource.
 * @param documentStyleSheets - Map of normalized hrefs to CSSStyleSheet objects
 * @param styleSheet - The stylesheet to scan
 * @param targetResource - The normalized path of the resource to find
 * @param visited - Set of already visited URLs to prevent circular imports
 * @param affectedImports - Array to collect affected import rules
 */
async function updateAndCollectAffectedImports(
  documentStyleSheets: Map<string, CSSStyleSheet>,
  styleSheet: StyleSheetLike,
  targetResource: string,
  visited: Set<string>,
  affectedImports: Array<AffectedStyleSheetRule>
): Promise<void> {
  if (!styleSheet) return;

  const stylesheetHref = (styleSheet as CSSStyleSheet).href;
  if (stylesheetHref) {
    const normalizedHref = normalizePath(stylesheetHref);
    if (visited.has(normalizedHref)) return;
    visited.add(normalizedHref);
  }

  try {
    const rules = (styleSheet as CSSStyleSheet).cssRules || (styleSheet as any).rules;
    if (!rules) return;

    for (let i = 0; i < rules.length; i++) {
      const rule = rules[i] as CSSRule & { style?: CSSStyleDeclaration; parentStyleSheet: CSSStyleSheet };

      // Handle @import rules
      if (typeof CSSImportRule !== 'undefined' && rule instanceof CSSImportRule) {
        const importHref = (rule as CSSImportRule).href;
        const resolvedHref = importHref
          ? resolveUrl(importHref, (styleSheet as CSSStyleSheet).href || window.location.href)
          : undefined;

        if (resolvedHref) {
          const normalizedImportHref = normalizePath(resolvedHref);

          // Check if the import itself matches the target
          if (normalizedImportHref === targetResource) {
            affectedImports.push({
              parentSheet: styleSheet as CSSStyleSheet,
              rule: rule as CSSImportRule,
              ruleIndex: i
            });
          }

          // Recursively check the imported stylesheet
          const importedSheet = (rule as CSSImportRule).styleSheet as CSSStyleSheet | null;
          if (importedSheet && !visited.has(normalizedImportHref)) {
            await updateAndCollectAffectedImports(
              documentStyleSheets,
              importedSheet,
              targetResource,
              visited,
              affectedImports
            );
          }
        }
      }

      // Handle style rules with url() references (fonts, images, etc.)
      if (
        (typeof CSSStyleRule !== 'undefined' && rule instanceof CSSStyleRule) ||
        (typeof CSSFontFaceRule !== 'undefined' && rule instanceof CSSFontFaceRule)
      ) {
        checkAndUpdateRuleForUrlReference(rule as CSSStyleRule | CSSFontFaceRule, targetResource);
      }

      // Handle nested rules (e.g., @media, @supports)
      const nestedRules = (rule as any).cssRules as CSSRuleList | undefined;
      if (nestedRules) {
        const nestedSheet: StyleSheetLike = {
          cssRules: nestedRules,
          href: (styleSheet as CSSStyleSheet).href
        };
        await updateAndCollectAffectedImports(
          documentStyleSheets,
          nestedSheet,
          targetResource,
          visited,
          affectedImports
        );
      }
    }
  } catch (error: any) {
    if (error && error.name === 'SecurityError') {
      console.warn(`Cannot access cssRules due to CORS policy for stylesheet: ${(styleSheet as CSSStyleSheet).href}`);
    } else {
      console.warn(`Error scanning stylesheet for resource:`, error);
    }
  }
}

/**
 * Collects all CSSStyleSheet objects from document.styleSheets corresponding to a linked stylesheet (no inline styles).
 * @returns a map of normalized hrefs to CSSStyleSheet objects
 */
function collectDocumentStyleSheets(): Map<string, CSSStyleSheet> {
  const styleSheets = new Map<string, CSSStyleSheet>();
  for (let i = 0; i < document.styleSheets.length; i++) {
    const sheet = document.styleSheets[i] as CSSStyleSheet;
    const normalizedHref = sheet.href ? normalizePath(sheet.href || '') : undefined;
    if (normalizedHref) {
      styleSheets.set(normalizedHref, sheet);
    }
  }
  return styleSheets;
}

function updatedImportedStyleSheet(styleSheet: StyleSheetLike, rule: CSSImportRule, ruleIndex: number) {
  if (styleSheet instanceof CSSStyleSheet && rule.href) {
    const newHref = cacheKiller(rule.href);
    styleSheet.deleteRule(ruleIndex);
    styleSheet.insertRule(`@import url('${newHref}');`, ruleIndex);
  }
}

/**
 * Checks if a CSS rule contains a url() reference matching the target resource.
 * @param rule - The CSS rule
 * @param targetResource - The normalized path of the resource to find
 * @returns True if the target resource is referenced in this rule
 */
function checkAndUpdateRuleForUrlReference(rule: CSSStyleRule | CSSFontFaceRule, targetResource: string) {
  const styleDecl = (rule as any).style as CSSStyleDeclaration | undefined;
  if (!styleDecl) return false;

  // Iterate through all style properties
  for (let j = 0; j < styleDecl.length; j++) {
    const property = styleDecl[j] as string;
    const value = styleDecl.getPropertyValue(property);

    // Extract url() references from the property value
    const urlRegex = /(url\(['"]?)([^'")]+)(['"]?\))/gi;
    let match: RegExpExecArray | null;

    while ((match = urlRegex.exec(value)) !== null) {
      const urlPath = match[2].trim();
      // Skip data URLs
      if (!urlPath.startsWith('data:')) {
        // Resolve relative URLs against the stylesheet's base URL
        const baseHref = (rule as any).parentStyleSheet?.href || window.location.href;
        const resolvedUrl = resolveUrl(urlPath, baseHref);
        if (normalizePath(resolvedUrl) === targetResource) {
          styleDecl.setProperty(property, value.replaceAll(urlRegex, `$1${cacheKiller(urlPath)}$3`));
          return true;
        }
      }
    }
  }

  return false;
}

/**
 * Resolves a relative URL against a base URL.
 * @param relativePath - The relative path
 * @param baseUrl - The base URL
 * @returns The resolved absolute URL
 */
function resolveUrl(relativePath: string, baseUrl: string): string {
  try {
    return new URL(relativePath, baseUrl).href;
  } catch (_error) {
    // If URL construction fails, return the relative path as-is
    return relativePath;
  }
}

/**
 * Normalizes a path for comparison by removing query strings, fragments, and normalizing slashes.
 * @param path - The path to normalize
 * @returns The normalized path
 */
function normalizePath(path: string): string {
  try {
    const url = new URL(path, window.location.href);
    // Remove query string and fragment
    return url.origin + url.pathname;
  } catch (_error) {
    // If not a valid URL, just clean up the path
    return path.split('?')[0]!.split('#')[0]!;
  }
}

function cacheKiller(originalHref: string): string {
  // Preserve existing query parameters and hash, and add/update a dedicated cache-busting parameter
  const [hrefWithoutHash, hash = ''] = originalHref.split('#');
  const [base, query = ''] = hrefWithoutHash.split('?');
  const params = new URLSearchParams(query);
  const cacheParam = 'v-hotreload';
  params.set(cacheParam, String(new Date().getTime()));
  const newQuery = params.toString();
  return `${base}?${newQuery}${hash ? `#${hash}` : ''}`;
}
