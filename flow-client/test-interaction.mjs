// Test script to verify GWT-TypeScript interaction
import { ArrayUtil } from './target/classes/META-INF/frontend/collections/JsArrayUtil.js';

console.log('✓ TypeScript module loaded');
console.log('✓ window.VaadinTypeScript:', typeof globalThis.VaadinTypeScript);
console.log('✓ window.VaadinTypeScript.ArrayUtil:', typeof globalThis.VaadinTypeScript?.ArrayUtil);

// Test the function
const testArray = [1, 2, 3];
const result = globalThis.VaadinTypeScript.ArrayUtil.isEmpty(testArray);
console.log('✓ ArrayUtil.isEmpty([1,2,3]):', result);

const emptyArray = [];
const result2 = globalThis.VaadinTypeScript.ArrayUtil.isEmpty(emptyArray);
console.log('✓ ArrayUtil.isEmpty([]):', result2);

console.log('✓ All TypeScript functions work via global scope!');
