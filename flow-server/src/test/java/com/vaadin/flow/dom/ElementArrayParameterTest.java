package com.vaadin.flow.dom;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.nodefeature.ElementChildrenList;
import com.vaadin.flow.internal.StateTree;

/**
 * Test for array parameter support in Element.callJsFunction() and Element.executeJs()
 */
public class ElementArrayParameterTest {

    @Test
    public void callJsFunction_arrayParameter() {
        Element element = ElementFactory.createDiv();
        
        // Attach element to make it valid for JS function calls
        StateTree tree = new StateTree(new UI().getInternals(), ElementChildrenList.class);
        tree.getRootNode().getFeature(ElementChildrenList.class).add(0, element.getNode());
        
        // This should not throw an exception anymore
        String[] arrayParam = {"hello", "world"};
        PendingJavaScriptResult result = element.callJsFunction("testFunction", arrayParam);
        Assert.assertNotNull(result);
    }
    
    @Test
    public void executeJs_arrayParameter() {
        Element element = ElementFactory.createDiv();
        
        // Attach element to make it valid for JS execution
        StateTree tree = new StateTree(new UI().getInternals(), ElementChildrenList.class);
        tree.getRootNode().getFeature(ElementChildrenList.class).add(0, element.getNode());
        
        // This should not throw an exception anymore
        String[] arrayParam = {"foo", "bar"};
        PendingJavaScriptResult result = element.executeJs("console.log($0)", arrayParam);
        Assert.assertNotNull(result);
    }
    
    @Test
    public void callJsFunction_multipleArrayParameters() {
        Element element = ElementFactory.createDiv();
        
        // Attach element
        StateTree tree = new StateTree(new UI().getInternals(), ElementChildrenList.class);
        tree.getRootNode().getFeature(ElementChildrenList.class).add(0, element.getNode());
        
        // Multiple array parameters
        String[] stringArray = {"a", "b"};
        Integer[] intArray = {1, 2, 3};
        Boolean[] boolArray = {true, false};
        
        PendingJavaScriptResult result = element.callJsFunction("multiArrayFunction", 
                stringArray, intArray, boolArray);
        Assert.assertNotNull(result);
    }
    
    @Test
    public void executeJs_mixedParameters() {
        Element element = ElementFactory.createDiv();
        
        // Attach element
        StateTree tree = new StateTree(new UI().getInternals(), ElementChildrenList.class);
        tree.getRootNode().getFeature(ElementChildrenList.class).add(0, element.getNode());
        
        // Mix of array and non-array parameters
        String[] arrayParam = {"test"};
        String stringParam = "hello";
        Integer intParam = 42;
        
        PendingJavaScriptResult result = element.executeJs("console.log($0, $1, $2)", 
                arrayParam, stringParam, intParam);
        Assert.assertNotNull(result);
    }
}