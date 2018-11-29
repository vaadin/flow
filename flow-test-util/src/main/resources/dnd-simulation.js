function createEvent(typeOfEvent) {
    var event = document.createEvent('CustomEvent');
    event.initCustomEvent(typeOfEvent, true, true, null);
    event.dataTransfer = {
        data : {},
        setData : function(key, value) {
            this.data[key] = value;
        },
        getData : function(key) {
            return this.data[key];
        }
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

function simulateHTML5DragAndDrop(element, target) {
    var dragStartEvent = createEvent('dragstart');
    dispatchEvent(element, dragStartEvent);
    var dropEvent = createEvent('drop');
    dispatchEvent(target, dropEvent, dragStartEvent.dataTransfer);
    var dragEndEvent = createEvent('dragend');
    dispatchEvent(element, dragEndEvent, dropEvent.dataTransfer);
}

var source = arguments[0];
var target = arguments[1];
simulateHTML5DragAndDrop(source, target);