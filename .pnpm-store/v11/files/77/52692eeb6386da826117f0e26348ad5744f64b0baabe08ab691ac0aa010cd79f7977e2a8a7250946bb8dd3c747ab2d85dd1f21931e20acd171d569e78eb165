/**
 * @license
 * Copyright (c) 2015 The Polymer Project Authors. All rights reserved.
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
import * as jsonschema from 'jsonschema';
import { Function as ResolvedFunction } from '../javascript/function';
import { Analysis as AnalysisResult, Element as ResolvedElement, ElementMixin as ResolvedMixin, Feature } from '../model/model';
import { UrlResolver } from '../url-loader/url-resolver';
import { Analysis } from './analysis-format';
export declare type ElementOrMixin = ResolvedElement | ResolvedMixin;
export declare type Filter = (feature: Feature | ResolvedFunction) => boolean;
export declare function generateAnalysis(input: AnalysisResult, urlResolver: UrlResolver, filter?: Filter): Analysis;
export declare class ValidationError extends Error {
    errors: jsonschema.ValidationError[];
    constructor(result: jsonschema.ValidatorResult);
}
/**
 * Throws if the given object isn't a valid AnalyzedPackage according to
 * the JSON schema.
 */
export declare function validateAnalysis(analyzedPackage: Analysis | null | undefined): void;
