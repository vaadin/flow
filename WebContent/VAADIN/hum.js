	var nodes = new Map();
	var ids = new Map();
	var nodeListeners = new Map();
	var templateMap = new Map();
	var nodeOverrides = new Map();

	var getNode = function(id) {
		var node = nodes.get(id);
		if (node == null) {
			node = {};
			ids.set(node, id);
			nodes.set(id, node);
		}
		return node;
	}

	var removeNode = function(node) {
		var id = ids.get(node);
		if (id !== undefined) {
			nodeListeners["delete"](node);
			nodes["delete"](id);
			ids["delete"](node);
		}
	}

	var handlers = {
		putNode : function(node, change) {
			var child = getNode(change.value);
			node[change.key + ""] = child;
		},
		put : function(node, change) {
			node[change.key + ""] = change.value;
		},
		putOverride: function(node, change) {
			var child = getNode(change.value);
			
			var overrides = nodeOverrides.get(node);
			if (overrides == null) {
				overrides = new Map();
				nodeOverrides.set(node, overrides);
			}
			overrides.set(change.key, node);
		},
		remove : function(node, change) {
			removeNode(node[change.key + ""]);
			delete node[change.key + ""];
		},
		listInsertNode : function(node, change) {
			var child = getNode(change.value);
			if (!(node[change.key] instanceof Array)) {
				node[change.key] = [];
			}
			node[change.key][change.index] = child;
		},
		listRemove : function(node, change) {
			removeNode(node[change.key][change.index]);
			node[change.key].splice(change.index, 1);
		},
		listInsert : function(node, change) {
			var value = change.value;
			if (!(node[change.key] instanceof Array)) {
				node[change.key] = [];
			}
			node[change.key][change.index] = value;
		}
	}

	var createTextElementListener = function(node, element) {
		return function(change) {
			if (change.key === "content") {
				if (change.type === "put") {
					element.textContent = change.value;
				} else if (change.type === "remove") {
					element.textContent = "";
				}
			}
		}
	}

	var addNodeListener = function(node, listener) {
		var listeners = nodeListeners.get(node);
		if (!listeners) {
			listeners = [];
			nodeListeners.set(node, listeners);
		}
		listeners.push(listener);
	}

	var createChildByTemplate = function(node, templateId) {
		var template = templateMap.get(templateId);

		element = document.createElement(template.tag);
		Object.getOwnPropertyNames(template.defaultAttributes).forEach(
				function(name) {
					element
							.setAttribute(name,
									template.defaultAttributes[name]);
				});

		switch (template.type) {
		case "ForElementTemplate":
		case "BoundElementTemplate":
			// Nothing more to do
			break;
		case "StaticChildrenElementTemplate":
			template.children.forEach(function(childTemplateId) {
				element
						.appendChild(createChildByTemplate(node,
								childTemplateId));
			});
			// Nothing more to do
			break;
		default:
			throw "Unsupported template type: " + template.type;
		}

		addNodeListener(node, createDynamicElementListener(node, element,
				template));

		return element;
	}

	var createChild = function(node) {
		var element;
		var templateId = node.TEMPLATE;
		var tag = node.TAG;
		if (templateId) {
			element = createChildByTemplate(node, templateId);
		} else if (tag === "#text") {
			element = document.createTextNode("");
			addNodeListener(node, createTextElementListener(node, element));
		} else {
			element = document.createElement(node.TAG);
			addNodeListener(node, createBasicElementListener(node, element));
		}

		return element;
	}

	var insertElementAtIndex = function(parent, child, index) {
		if (parent.childNodes.length === index) {
			parent.appendChild(child);
		} else {
			var referenceNode = parent.childNodes[index];
			parent.insertBefore(child, referenceNode);
		}
	}

	var createDynamicElementListener = function(node, element, template) {
		return function(change) {
			switch (change.type) {
			case "put":
				if (template.attributeBindings.hasOwnProperty(change.key)) {
					var attributeName = template.attributeBindings[change.key];
					element.setAttribute(attributeName, change.value);
				} else if (change.key !== "TEMPLATE") {
					throw "put not supported for " + change.key;
				}
				break;
			case "remove":
				if (template.attributeBindings.hasOwnProperty(change.key)) {
					var attributeName = template.attributeBindings[change.key];
					element.removeAttribute(attributeName);
				} else {
					throw "Unsupported remove";
				}
				break;
			case "putOverride":
				if (template.id !== change.key) {
					console.log(template.id, " ignores override for ", change.key);
					break;
				}
				var overrideNode = getNode(change.value);
				
				var listener;
				if (element.nodeType === 1) {
					listener = createBasicElementListener(overrideNode, element);
				} else if (element.nodeType === 3) {
					listener = createTextElementListener(overrideNode, element);
				} else {
					throw "Unsupported node type " + document.nodeType;
				}
				addNodeListener(overrideNode, listener);
				break;
			case "listInsertNode":
				if (template.type === "ForElementTemplate"
						&& change.key === template.modelKey) {
					var childNode = nodes.get(change.value);
					var childElement = createChildByTemplate(childNode,
							template.childTemplate);
					insertElementAtIndex(element, childElement, change.index);
				} else {
					throw change.type
							+ " only supported for ForElementTemplate modelKey";
				}
				break;
			case "listRemove":
				if (template.type === "ForElementTemplate"
						&& change.key === template.modelKey) {
					element.childNodes[change.index].remove();
				} else {
					throw "Unsupported listRemove";
				}
				break;
			default:
				throw "Unsupported change type: " + change.type;
			}
		}
	}

	var createBasicElementListener = function(node, element) {
		var handleAttribute = function(key, value) {
			if (key === "TAG") {
				// TODO Verify with element
			} else {
				element.setAttribute(key, value);
			}
		}

		var handleInsertNode = function(change) {
			if (change.key !== 'CHILDREN') {
				return;
			}
			var childNode = nodes.get(change.value);
			var childElement = createChild(childNode);

			insertElementAtIndex(element, childElement, change.index)
		}
		
		var addListener = function(type) {
			var listener = function() {
				console.log("Got event " + type + " from ", element);
				window.vEvent(ids.get(node), type)
			}
			listener.type = type;
			if (!("listenerCache" in element)) {
				element.listenerCache = [];
			}
			element.listenerCache.push(listener); 
			element.addEventListener(type, listener);
		}
		
		var removeListener = function(index) {
			var listener = element.listenerCache[index];
			element.removeEventListener(listener.type, listener);
			element.listenerCache.splice(index, 1);
			if (element.listenerCache.length == 0) {
				delete element.listenerCache;
			}
		}

		return function(change) {
			switch (change.type) {
			case "put":
				handleAttribute(change.key, change.value);
				break;
			case "listInsertNode":
				handleInsertNode(change);
				break;
			case "listRemove":
				if (change.key === "CHILDREN") {
					element.childNodes[change.index].remove();
				} else if (change.key === "LISTENERS"){
					removeListener(change.index);
				} else {
					console.log("Unsupported BasicTemplate listRemove", change);
				}
				break;
			case "listInsert":
				if (change.key === "LISTENERS") {
					addListener(change.value);
				} else {
					console.log("Unsupported BasicTemplate insert", change);
				}
				break;
			default:
				console.log("Unsupported BasicTemplate change", change);
			}
		}
	}

	var createTemplateListener = function(node, element) {
		if (node.TAG) {
			if (node.TAG === "#text") {
				return createTextElementListener(node, element);
			} else {
				return createBasicElementListener(node, element);
			}
		} else if (node.TEMPLATE) {
			return createDynamicElementListener(node, element, node.TEMPLATE);
		}
	}

	window.handleChanges = function(changes, containerElement) {
		changes.forEach(function(change) {
			var node = getNode(change.id);

			var handler = handlers[change.type];
			if (!handler) {
				console.log("No handler for " + change.type);
			} else {
				handler(node, change);
			}
		});

		var rootNode = getNode(1);
		var bodyNode = rootNode.body;
		if (bodyNode) {
			bodyListeners = nodeListeners.get(bodyNode);
			if (!bodyListeners) {
				while (containerElement.childNodes.length != 0) {
					containerElement.childNodes[0].remove();
				}

				addNodeListener(bodyNode, createTemplateListener(bodyNode,
						containerElement))

			}
		}

		changes.forEach(function(change) {
			var node = getNode(change.id);

			var listeners = nodeListeners.get(node);
			if (listeners) {
				listeners.forEach(function(listener) {
					listener(change);
				});
			}
		});

		console.log("nodes", nodes);
	}

	window.handleTemplates = function(templates) {
		if (!templates) {
			return;
		}
		Object.getOwnPropertyNames(templates).forEach(function(key) {
			var template = templates[key];
			templateMap.set(+key, template);
		})
	}


