/*
 * Copyright 2000-2014 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.client.ui;

import java.util.HashSet;
import java.util.Set;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.vaadin.client.Util;
import com.vaadin.client.communication.ServerRpcQueue;
import com.vaadin.client.ui.ui.UIConnector;
import com.vaadin.shared.extension.javascriptmanager.ExecuteJavaScriptRpc;
import com.vaadin.shared.extension.javascriptmanager.JavaScriptManagerState;

public class JavaScriptManager {
    private Set<String> currentNames = new HashSet<String>();
    private UIConnector ui;

    public JavaScriptManager(UIConnector ui) {
        this.ui = ui;
        ui.registerRpc(ExecuteJavaScriptRpc.class, new ExecuteJavaScriptRpc() {
            @Override
            public void executeJavaScript(String Script) {
                eval(Script);
            }
        });
    }

    public void onStateChanged() {
        Set<String> newNames = getState().names;

        // Current names now only contains orphan callbacks
        currentNames.removeAll(newNames);

        for (String name : currentNames) {
            removeCallback(name);
        }

        currentNames = new HashSet<String>(newNames);
        for (String name : newNames) {
            addCallback(name);
        }
    }

    // TODO Ensure we don't overwrite anything (important) in $wnd
    private native void addCallback(String name)
    /*-{
        var m = this;
        var target = $wnd;
        var parts = name.split('.');
        
        for(var i = 0; i < parts.length - 1; i++) {
            var part = parts[i];
            if (target[part] === undefined) {
                target[part] = {};
            }
            target = target[part];
        }
        
        target[parts[parts.length - 1]] = $entry(function() {
            //Must make a copy because arguments is an array-like object (not instanceof Array), causing suboptimal JSON encoding
            var args = Array.prototype.slice.call(arguments, 0);
            m.@com.vaadin.client.ui.JavaScriptManager::sendRpc(Ljava/lang/String;Lcom/google/gwt/core/client/JsArray;)(name, args);
        });
    }-*/;

    // TODO only remove what we actually added
    // TODO We might leave empty objects behind, but there's no good way of
    // knowing whether they are unused
    private native void removeCallback(String name)
    /*-{
        var target = $wnd;
        var parts = name.split('.');
        
        for(var i = 0; i < parts.length - 1; i++) {
            var part = parts[i];
            if (target[part] === undefined) {
                $wnd.console.log(part,'not defined in',target);
                // No longer attached -> nothing more to do
                return;
            }
            target = target[part];
        }

        $wnd.console.log('removing',parts[parts.length - 1],'from',target);
        delete target[parts[parts.length - 1]];
    }-*/;

    private static native void eval(String script)
    /*-{
        if(script) {
            (new $wnd.Function(script)).apply($wnd);
        }
    }-*/;

    public void sendRpc(String name, JsArray<JavaScriptObject> arguments) {
        Object[] parameters = new Object[] { name, Util.jso2json(arguments) };

        /*
         * Must invoke manually as the RPC interface can't be used in GWT
         * because of the JSONArray parameter
         */
        ServerRpcQueue rpcQueue = ServerRpcQueue.get(ui.getConnection());
        throw new RuntimeException("Sending JS RPC is no longer supported");
    }

    public JavaScriptManagerState getState() {
        return ui.getState().javascriptManager;
    }

}
