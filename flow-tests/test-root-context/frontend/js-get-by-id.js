/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
window.jsApiConnector = {
        
  jsFunction: function( element, appId, nodeId ){
    element.operation = function(){
      var node = window.Vaadin.Flow.clients[appId].getByNodeId(nodeId);
      element.textContent = node.textContent;
    }
  }
}
