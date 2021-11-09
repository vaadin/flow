/* innerHtml = ``; */
import { PolymerElement } from '@polymer/polymer/polymer-element.js';
const $_documentContainer = document.createElement('template');
$_documentContainer.innerHTML =
  `<dom-module id='no-html-template'> 
    <template><div>No Template</div></template> 
  </dom-module>`;
