/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
function doNotifyJsExecution(){
  var lbl = document.createElement("label");
  lbl.setAttribute("id", "js");
  lbl.innerHTML='Inlined JS';
  document.body.appendChild(lbl);
}

function notifyJsExecution(){
  if ( document.body) {
    doNotifyJsExecution()
  } else {
    setTimeout(notifyJsExecution, 50);
  }
}

notifyJsExecution();
