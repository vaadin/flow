// The export statement makes this file valid only when it is loaded as an ES
// module; a classic <script> would fail to parse it
export const marker = 'runtime-module.js';

const div = document.createElement('div');
div.id = 'eager-module-marker';
div.textContent = marker;
document.body.appendChild(div);
