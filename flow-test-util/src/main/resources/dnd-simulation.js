function createEvent(typeOfEvent, effectAllowed, dropEffect) {
  var event = document.createEvent('CustomEvent');
  event.initCustomEvent(typeOfEvent, true, true, null);
  event.dataTransfer = {
    data: {},
    setData: function (key, value) {
      this.data[key] = value;
    },
    getData: function (key) {
      return this.data[key];
    },
    effectAllowed: effectAllowed,
    dropEffect: dropEffect
  };
  return event;
}

function dispatchEvent(element, event, transferData) {
  if (transferData !== undefined) {
    event.dataTransfer = transferData;
  }
  if (element.dispatchEvent) {
    element.dispatchEvent(event);
  } else if (element.fireEvent) {
    element.fireEvent('on' + event.type, event);
  }
}

function simulateHTML5DragAndDrop(source, target) {
  var effectAllowed = source['__effectAllowed'];
  var dropEffect = target['__dropEffect'];

  var dragStartEvent = createEvent('dragstart', effectAllowed);
  dispatchEvent(source, dragStartEvent);

  var dragEnterEvent = createEvent("dragenter", effectAllowed);
  dispatchEvent(target, dragEnterEvent);

  var dragOverEvent = createEvent("dragover", effectAllowed);
  dispatchEvent(target, dragOverEvent);

  if (dragEnterEvent.dataTransfer.dropEffect === dropEffect &&
      dragOverEvent.dataTransfer.dropEffect  === dropEffect &&
      dropEffect !== "NONE" &&
      effectAllowed !== "NONE" &&
      (effectAllowed === "ALL" ||
          effectAllowed === dropEffect ||
          effectAllowed.includes(dropEffect))) {
    var dropEvent = createEvent('drop', effectAllowed);
    dispatchEvent(target, dropEvent);
    var dragEndEvent = createEvent('dragend', effectAllowed, dropEffect);
    dispatchEvent(source, dragEndEvent);
  } else {
    var dragEndEvent = createEvent('dragend', effectAllowed, "NONE");
    dispatchEvent(source, dragEndEvent);
  }
}

var source = arguments[0];
var target = arguments[1];
simulateHTML5DragAndDrop(source, target);
