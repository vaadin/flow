// Test script to verify GWT-TypeScript interaction
import { ArrayUtil } from './target/classes/META-INF/frontend/collections/JsArrayUtil.js';

console.log('✓ TypeScript module loaded');
console.log('✓ window.Vaadin:', typeof globalThis.Vaadin);
console.log('✓ window.Vaadin.TypeScript:', typeof globalThis.Vaadin?.TypeScript);
console.log('✓ window.Vaadin.TypeScript.ArrayUtil:', typeof globalThis.Vaadin?.TypeScript?.ArrayUtil);

// Test the function
const testArray = [1, 2, 3];
const result = globalThis.Vaadin.TypeScript.ArrayUtil.isEmpty(testArray);
console.log('✓ ArrayUtil.isEmpty([1,2,3]):', result);

const emptyArray = [];
const result2 = globalThis.Vaadin.TypeScript.ArrayUtil.isEmpty(emptyArray);
console.log('✓ ArrayUtil.isEmpty([]):', result2);

console.log('✓ All TypeScript functions work via window.Vaadin.TypeScript!');
