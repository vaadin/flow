window.jsApiConnector = {
        
        jsFunction: function( element, appId, nodeId ){
            element.operation = function(){
                var node = window.vaadin.clients[appId].getByNodeId(nodeId);
                element.textContent = node.textContent;
            }
        }
}
