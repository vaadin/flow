window.jsApiConnector = {
        
  jsFunction: function( element, appId, nodeId ){
    element.operation = function(){
      var node = window.Vaadin.Flow.clients[appId].getByNodeId(nodeId);
      element.textContent = node.textContent;
    }
  }
}
