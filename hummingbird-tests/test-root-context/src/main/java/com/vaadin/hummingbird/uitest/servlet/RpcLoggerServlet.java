/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.servlet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.html.Label;
import com.vaadin.hummingbird.uitest.ui.RPCLoggerUI;
import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.communication.ServerRpcHandler;
import com.vaadin.server.communication.UidlRequestHandler;
import com.vaadin.server.communication.rpc.RpcInvocationHandler;
import com.vaadin.shared.JsonConstants;
import com.vaadin.ui.UI;

import elemental.json.JsonObject;

/**
 * @author Vaadin Ltd
 *
 */
@WebServlet(asyncSupported = true, urlPatterns = { "/rpc/*" })
@VaadinServletConfiguration(ui = RPCLoggerUI.class, productionMode = false)
public class RpcLoggerServlet extends VaadinServlet {

    @Override
    protected VaadinServletService createServletService(
            DeploymentConfiguration deploymentConfiguration)
            throws ServiceException {
        RPCLoggerService service = new RPCLoggerService(this,
                deploymentConfiguration);
        service.init();
        return service;
    }

    private static class RPCLoggerService extends VaadinServletService {

        public RPCLoggerService(VaadinServlet servlet,
                DeploymentConfiguration deploymentConfiguration)
                throws ServiceException {
            super(servlet, deploymentConfiguration);
        }

        @Override
        protected List<RequestHandler> createRequestHandlers()
                throws ServiceException {
            List<RequestHandler> handlers = super.createRequestHandlers();
            handlers.add(new LoggingUidlRequestHandler());
            return handlers;
        }

    }

    private static class LoggingUidlRequestHandler extends UidlRequestHandler {
        @Override
        protected ServerRpcHandler createRpcHandler() {
            return new LoggingServerRpcHandler();
        }
    }

    private static class LoggingServerRpcHandler extends ServerRpcHandler {

        private HashMap<String, RpcInvocationHandler> handlers;

        @Override
        protected Map<String, RpcInvocationHandler> getInvocationHandlers() {
            if (handlers == null) {
                handlers = new HashMap<>();
                super.getInvocationHandlers()
                        .forEach((type, handler) -> handlers.put(type,
                                new RpcInterceptor(handler)));

            }
            return handlers;
        }

    }

    private static class RpcInterceptor implements RpcInvocationHandler {

        private final RpcInvocationHandler delegate;

        private RpcInterceptor(RpcInvocationHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getRpcType() {
            return delegate.getRpcType();
        }

        @Override
        public void handle(UI ui, JsonObject invocationJson) {
            delegate.handle(ui, invocationJson);
            StateNode node = ui.getInternals().getStateTree()
                    .getNodeById(getNodeId(invocationJson));
            Div container = new Div();
            container.addClassName("log");
            Label nodeLabel = new Label();
            if (node == null) {
                nodeLabel = new Label("Node is null");
            } else {
                nodeLabel = new Label("Node is " + node.getId());
            }
            nodeLabel.addClassName("node");
            container.add(nodeLabel);

            container.add(new Div());

            container.add(new Label("Invocation json is :"));
            Label json = new Label(invocationJson.toJson());
            json.addClassName("json");
            container.add(json);
            ui.add(container);
        }

        private static int getNodeId(JsonObject invocationJson) {
            return (int) invocationJson.getNumber(JsonConstants.RPC_NODE);
        }

    }

}
