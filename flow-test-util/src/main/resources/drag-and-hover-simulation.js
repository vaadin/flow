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

function simulateHTML5DragAndHover(source, target) {
  var effectAllowed = source['__effectAllowed'];
  var dropEffect = target['__dropEffect'];

  var dragStartEvent = createEvent('dragstart', effectAllowed);
  dispatchEvent(source, dragStartEvent);

  var dragEnterEvent = createEvent("dragenter", effectAllowed);
  dispatchEvent(target, dragEnterEvent);
}

var source = arguments[0];
var target = arguments[1];
simulateHTML5DragAndHover(source, target);