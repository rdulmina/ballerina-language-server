/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

module io.ballerina.LSExtensions.FlowService {
    requires io.ballerina.language.server.commons;
    requires io.ballerina.lang;
    requires org.eclipse.lsp4j.jsonrpc;
    requires org.eclipse.lsp4j;
    requires com.google.gson;
    requires io.ballerina.diagram.util;
    requires io.ballerina.flow.model.generator;
    requires io.ballerina.openapi.core;
    requires io.ballerina.language.server.core;
    requires io.ballerina.parser;
    requires io.ballerina.tools.api;
    requires io.ballerina.jsonmapper;
    requires io.ballerina.formatter.core;
    requires io.ballerina.model.generator.commons;
    requires io.ballerina.toml;
    requires java.net.http;
}
