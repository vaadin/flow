/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
window.inMemoryConnector = {
        
  init: function( component, inMemoryElement ){
    component.$dataContainer = inMemoryElement;

    component.useInMemoryElement = function(parent){
      var copy = component.$dataContainer.cloneNode(true);
      parent.appendChild(copy);
    }
  }
}
