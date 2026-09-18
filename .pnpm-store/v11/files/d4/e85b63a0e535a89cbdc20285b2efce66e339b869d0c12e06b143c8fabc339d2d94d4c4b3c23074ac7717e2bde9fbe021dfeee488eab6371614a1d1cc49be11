/**
 * @license
 * Copyright (c) 2018 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at
 * http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at
 * http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at
 * http://polymer.github.io/PATENTS.txt
 */
import { Analyzer, Document, ResolvedUrl } from 'polymer-analyzer';
import { AssignedBundle, BundleManifest } from './bundle-manifest';
/**
 * Looks up and/or defines the unique name for an item exported with the given
 * name in a module within a bundle.
 */
export declare function getOrSetBundleModuleExportName(bundle: AssignedBundle, moduleUrl: ResolvedUrl, name: string): string;
/**
 * Returns a set of every name exported by a module.
 */
export declare function getModuleExportNames(document: Document): Set<string>;
/**
 * Ensures that exported names from modules which have the same URL as their
 * bundle will have precedence over other module exports, which will be
 * counter-suffixed in the event of name collisions.  This has no technical
 * benefit, but it results in module export naming choices that are easier
 * to reason about for developers and may aid in debugging.
 */
export declare function reserveBundleModuleExportNames(analyzer: Analyzer, manifest: BundleManifest): Promise<void>;
