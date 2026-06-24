window.inMemoryConnector = {
        
  init: function( component, inMemoryElement ){
    component.$dataContainer = inMemoryElement;

    component.useInMemoryElement = function(parent){
      var copy = component.$dataContainer.cloneNode(true);
      parent.appendChild(copy);
    }
  }
}
