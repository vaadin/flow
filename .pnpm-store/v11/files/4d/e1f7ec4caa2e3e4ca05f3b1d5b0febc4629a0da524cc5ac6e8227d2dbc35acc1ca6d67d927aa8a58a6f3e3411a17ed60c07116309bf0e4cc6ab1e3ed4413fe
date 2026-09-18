/**
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
import * as parse5 from 'parse5';
import { ParsedHtmlDocument } from '../html/html-document';
import { SourcePosition } from '../model/model';
export declare type LocationResult = AttributesSection | AttributeValue | TagName | EndTag | TextNode | ScriptContents | StyleContents | Comment;
/** In the tagname of a start tag. */
export interface TagName {
    kind: 'tagName';
    element: parse5.ASTNode;
}
/** In an end tag. */
export interface EndTag {
    kind: 'endTag';
    element: parse5.ASTNode;
}
/** In the attributes section of a start tag. Maybe in an attribute name. */
export interface AttributesSection {
    kind: 'attribute';
    /** The attribute name that we're hovering over, if any. */
    attribute: string | null;
    /** The element whose start tag we're in. */
    element: parse5.ASTNode;
}
/** In the value of an attribute of a start tag. */
export interface AttributeValue {
    kind: 'attributeValue';
    attribute: string;
    element: parse5.ASTNode;
}
/** In a text node. */
export interface TextNode {
    kind: 'text';
    textNode?: parse5.ASTNode;
}
/** In the text of a <script> */
export interface ScriptContents {
    kind: 'scriptTagContents';
    textNode?: parse5.ASTNode;
}
/** In the text of a <style> */
export interface StyleContents {
    kind: 'styleTagContents';
    textNode?: parse5.ASTNode;
}
/** In a <!-- comment --> */
export interface Comment {
    kind: 'comment';
    commentNode: parse5.ASTNode;
}
/**
 * Given a position and an HTML document, try to describe what new text typed
 * at the given position would be.
 *
 * Where possible we try to return the ASTNode describing that position, but
 * sometimes there does not actually exist one. (for a simple case, the empty
 * string should be interpreted as a text node, but there is no text node in
 * an empty document, but there would be after the first character was typed).
 */
export declare function getLocationInfoForPosition(document: ParsedHtmlDocument, position: SourcePosition): LocationResult;
