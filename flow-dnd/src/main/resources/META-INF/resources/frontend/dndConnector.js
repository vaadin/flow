window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.dndConnector = {

  __ondragenterListener: function (event) {
    // TODO filter by data type
    // TODO prevent dropping on itself (by default)
    const effect = event.currentTarget['__dropEffect'];
    if (effect) {
      event.dataTransfer.dropEffect = effect;
    }
    if (effect && effect !== 'none') {
      event.currentTarget.classList.add("v-drag-over-target");
      // enables browser specific pseudo classes (at least FF)
      event.preventDefault();
      event.stopPropagation(); // don't let parents know
    }
  },

  __ondragoverListener: function (event) {
    // TODO filter by data type
    // TODO filter by effectAllowed != dropEffect due to Safari & IE11 ?
    const effect = event.currentTarget['__dropEffect'];
    if (effect) {
      event.dataTransfer.dropEffect = effect;
    }
    // allows the drop && don't let parents know
    event.preventDefault();
    event.stopPropagation();
  },

  __ondragleaveListener: function (event) {
    event.currentTarget.classList.remove("v-drag-over-target");
  },

  __ondropListener: function (event) {
    const effect = event.currentTarget['__dropEffect'];
    if (effect) {
      event.dataTransfer.dropEffect = effect;
    }
    event.currentTarget.classList.remove("v-drag-over-target");
    // prevent browser handling && don't let parents know
    event.preventDefault();
    event.stopPropagation();
  },

  updateDropTarget : function(element) {
    if (element['__active']) {
      element.addEventListener('dragenter', this.__ondragenterListener, false);
      element.addEventListener('dragover', this.__ondragoverListener, false);
      element.addEventListener('dragleave', this.__ondragleaveListener, false);
      element.addEventListener('drop', this.__ondropListener, false);
    } else {
      element.removeEventListener('dragenter', this.__ondragenterListener, false);
      element.removeEventListener('dragover', this.__ondragoverListener, false);
      element.removeEventListener('dragleave', this.__ondragleaveListener, false);
      element.removeEventListener('drop', this.__ondropListener, false);
      element.classList.remove("v-drag-over-target");
    }
  },

  /** DRAG SOURCE METHODS: */

  __dragstartListener: function (event) {
    event.stopPropagation();
    event.dataTransfer.setData("text/plain", "");
    if (event.currentTarget['__effectAllowed']) {
      event.dataTransfer.effectAllowed = event.currentTarget['__effectAllowed'];
    }
    event.currentTarget.classList.add('v-dragged');
  },

  __dragendListener: function (event) {
    event.currentTarget.classList.remove('v-dragged');
  },

  updateDragSource: function (element) {
    if (element['draggable']) {
      element.addEventListener('dragstart', this.__dragstartListener, false);
      element.addEventListener('dragend', this.__dragendListener, false);
    } else {
      element.removeEventListener('dragstart', this.__dragstartListener, false);
      element.removeEventListener('dragend', this.__dragendListener, false);
    }
  }
};
