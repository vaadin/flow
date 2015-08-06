package com.vaadin.hummingbird;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONObject;

import com.vaadin.hummingbird.kernel.AttributeBinding;
import com.vaadin.hummingbird.kernel.BasicElementTemplate;
import com.vaadin.hummingbird.kernel.BoundElementTemplate;
import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.hummingbird.kernel.ElementTemplate;
import com.vaadin.hummingbird.kernel.ForElementTemplate;
import com.vaadin.hummingbird.kernel.ModelAttributeBinding;
import com.vaadin.hummingbird.kernel.RootNode;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.hummingbird.kernel.StaticChildrenElementTemplate;
import com.vaadin.hummingbird.kernel.change.IdChange;
import com.vaadin.hummingbird.kernel.change.ListInsertChange;
import com.vaadin.hummingbird.kernel.change.ListRemoveChange;
import com.vaadin.hummingbird.kernel.change.ListReplaceChange;
import com.vaadin.hummingbird.kernel.change.NodeChangeVisitor;
import com.vaadin.hummingbird.kernel.change.ParentChange;
import com.vaadin.hummingbird.kernel.change.PutChange;
import com.vaadin.hummingbird.kernel.change.RemoveChange;

@WebServlet("hum")
public class ExampleServlet extends HttpServlet {

	@Override
	protected void service(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException, IOException {
		synchronized (this) {
			super.service(arg0, arg1);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String step = req.getParameter("step");
		HttpSession session = req.getSession();
		RootNode rootNode = processStep(step, session);

		JSONArray changes = new JSONArray();
		JSONObject newTemplates = new JSONObject();

		rootNode.commit(new NodeChangeVisitor() {
			private JSONObject createChange(StateNode node, String type) {
				JSONObject change = new JSONObject();
				change.put("type", type);
				change.put("id", node.getId());
				changes.put(change);
				return change;
			}

			@Override
			public void visitRemoveChange(StateNode node, RemoveChange removeChange) {
				JSONObject change = createChange(node, "remove");
				change.put("key", removeChange.getKey());
			}

			@Override
			public void visitPutChange(StateNode node, PutChange putChange) {
				JSONObject change;
				Object key = putChange.getKey();
				Object value = putChange.getValue();
				if (value instanceof StateNode) {
					if (key instanceof ElementTemplate) {
						change = createChange(node, "putOverride");

						ElementTemplate template = (ElementTemplate) key;
						key = Integer.valueOf(template.getId());
						ensureTemplateSent(template, session, newTemplates);
					} else {
						change = createChange(node, "putNode");
					}
					change.put("value", ((StateNode) value).getId());
				} else {
					change = createChange(node, "put");
					if (value instanceof ElementTemplate) {
						ElementTemplate template = (ElementTemplate) value;
						value = Integer.valueOf(template.getId());
						ensureTemplateSent(template, session, newTemplates);
					}
					change.put("value", value);
				}
				change.put("key", key);
			}

			@Override
			public void visitParentChange(StateNode node, ParentChange parentChange) {
				// Ignore
			}

			@Override
			public void visitListReplaceChange(StateNode node, ListReplaceChange listReplaceChange) {
				JSONObject change;
				Object value = listReplaceChange.getNewValue();
				if (value instanceof StateNode) {
					change = createChange(node, "listReplaceNode");
					change.put("value", ((StateNode) value).getId());

				} else {
					change = createChange(node, "listReplace");
					change.put("value", value);
				}
				change.put("index", listReplaceChange.getIndex());
				change.put("key", listReplaceChange.getKey());
			}

			@Override
			public void visitListRemoveChange(StateNode node, ListRemoveChange listRemoveChange) {
				JSONObject change = createChange(node, "listRemove");
				change.put("index", listRemoveChange.getIndex());
				change.put("key", listRemoveChange.getKey());
			}

			@Override
			public void visitListInsertChange(StateNode node, ListInsertChange listInsertChange) {
				JSONObject change;
				Object value = listInsertChange.getValue();
				if (value instanceof StateNode) {
					change = createChange(node, "listInsertNode");
					change.put("value", ((StateNode) value).getId());
				} else {
					change = createChange(node, "listInsert");
					change.put("value", value);
				}
				change.put("index", listInsertChange.getIndex());
				change.put("key", listInsertChange.getKey());
			}

			@Override
			public void visitIdChange(StateNode node, IdChange idChange) {
				// Ignore
			}
		});

		resp.setContentType("application/json");
		PrintWriter writer = resp.getWriter();
		JSONObject response = new JSONObject();
		response.put("changes", changes);
		if (newTemplates.length() != 0) {
			response.put("templates", newTemplates);
		}
		writer.print(response.toString());
	}

	private static void ensureTemplateSent(ElementTemplate template, HttpSession session, JSONObject newTemplates) {
		@SuppressWarnings("unchecked")
		Set<Integer> sentTemplates = (Set<Integer>) session.getAttribute("sentTemplates");
		if (sentTemplates == null) {
			sentTemplates = new HashSet<>();
		}

		if (!sentTemplates.contains(Integer.valueOf(template.getId()))) {
			newTemplates.put(Integer.toString(template.getId()), serializeTemplate(template, session, newTemplates));
		}

		session.setAttribute("sentTemplates", sentTemplates);
	}

	private static JSONObject serializeTemplate(ElementTemplate template, HttpSession session,
			JSONObject newTemplates) {
		JSONObject serialized = new JSONObject();
		serialized.put("type", template.getClass().getSimpleName());
		serialized.put("id", template.getId());

		if (template.getClass() == BoundElementTemplate.class) {
			serializeBoundElementTemplate(serialized, (BoundElementTemplate) template);
		} else if (template.getClass() == StaticChildrenElementTemplate.class) {
			serializeStaticChildrenElementEmplate(serialized, (StaticChildrenElementTemplate) template, session,
					newTemplates);
		} else if (template.getClass() == ForElementTemplate.class) {
			serializeForTemplate(serialized, (ForElementTemplate) template, session, newTemplates);
		} else {
			throw new RuntimeException(template.toString());
		}
		return serialized;
	}

	private static void serializeForTemplate(JSONObject serialized, ForElementTemplate template, HttpSession session,
			JSONObject newTemplates) {
		serialized.put("modelKey", template.getModelProperty());

		ElementTemplate childTemplate = template.getChildTemplate();
		ensureTemplateSent(childTemplate, session, newTemplates);
		serialized.put("childTemplate", childTemplate.getId());

		serializeBoundElementTemplate(serialized, template);
	}

	private static void serializeStaticChildrenElementEmplate(JSONObject serialized,
			StaticChildrenElementTemplate template, HttpSession session, JSONObject newTemplates) {
		JSONArray children = new JSONArray();
		serialized.put("children", children);
		for (BoundElementTemplate childTemplate : template.getChildren()) {
			ensureTemplateSent(childTemplate, session, newTemplates);
			children.put(childTemplate.getId());
		}
		serializeBoundElementTemplate(serialized, template);
	}

	private static void serializeBoundElementTemplate(JSONObject serialized, BoundElementTemplate bet) {
		JSONObject attributeBindings = new JSONObject();
		for (AttributeBinding attributeBinding : bet.getAttributeBindings().values()) {
			if (attributeBinding instanceof ModelAttributeBinding) {
				ModelAttributeBinding mab = (ModelAttributeBinding) attributeBinding;
				attributeBindings.put(mab.getModelPath(), mab.getAttributeName());
			} else {
				// Not yet supported
				throw new RuntimeException(attributeBinding.toString());
			}
		}

		JSONObject defaultAttributes = new JSONObject();
		bet.getDefaultAttributeValues().forEach(defaultAttributes::put);

		serialized.put("attributeBindings", attributeBindings).put("defaultAttributes", defaultAttributes).put("tag",
				bet.getTag());
	}

	private static RootNode processStep(String step, HttpSession session) throws ServletException {
		RootNode rootNode = (RootNode) session.getAttribute(RootNode.class.getName());
		switch (step) {
		case "0":
			rootNode = createRootNode();
			break;
		case "1":
			doStep1(rootNode);
			break;
		case "2":
			doStep2(rootNode);
			break;
		default:
			throw new ServletException("Step " + step + " not implemented");
		}

		session.setAttribute(RootNode.class.getName(), rootNode);
		return rootNode;
	}

	private static void doStep2(RootNode rootNode) {
		Element body = getBodyElement(rootNode);

		body.getChild(0).removeFromParent();
	}

	private static void doStep1(RootNode rootNode) {
		Element body = getBodyElement(rootNode);

		Element h1 = body.getChild(1);
		h1.setAttribute("style", "color: blue");

		Element text = h1.getChild(0);
		text.setAttribute("content", "A blue Hummingbird!");

		Element inputHolder = body.getChild(2);
		inputHolder.setAttribute("style", "border: 1px solid black");
		List<Object> inputs = inputHolder.getNode().getMultiValued("inputs");
		inputs.remove(2);
		((StateNode) inputs.get(1)).put("inputValue", "Updated value");

		Element bodyText = Element.createText("Just some body text");
		body.insertChild(body.getChildCount(), bodyText);
	}

	private static Element getBodyElement(RootNode rootNode) {
		Element body = Element.getElement(BasicElementTemplate.get(), rootNode.get("body", StateNode.class));
		return body;
	}

	private static RootNode createRootNode() {
		Element button = new Element("button");
		button.insertChild(0, Element.createText("Next step"));

		Element h1 = new Element("h1");
		h1.insertChild(0, Element.createText("Hello Hummingbird!"));

		Element body = new Element("body");

		body.insertChild(0, button);
		body.insertChild(1, h1);

		BoundElementTemplate inputTemplate = new BoundElementTemplate("input",
				Arrays.asList(new ModelAttributeBinding("value", "inputValue")),
				Collections.singletonMap("type", "text"));
		StateNode node = StateNode.create();

		ForElementTemplate inputHolderTemplate = new ForElementTemplate("div", Collections.emptyList(),
				Collections.emptyMap(), "inputs", inputTemplate);

		List<Object> inputs = node.getMultiValued("inputs");
		for (int i = 0; i < 5; i++) {
			StateNode input = StateNode.create();
			input.put("inputValue", "Some value " + i);
			inputs.add(input);
		}
		Element inputHolder = Element.getElement(inputHolderTemplate, node);
		body.insertChild(body.getChildCount(), inputHolder);

		RootNode rootNode = new RootNode();
		rootNode.put("body", body.getNode());

		return rootNode;
	}
}
