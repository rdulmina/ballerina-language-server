/*
 *  Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

package io.ballerina.architecturemodelgenerator.core.model.service;

import io.ballerina.architecturemodelgenerator.core.diagnostics.ArchitectureModelDiagnostic;
import io.ballerina.architecturemodelgenerator.core.model.ModelElement;
import io.ballerina.architecturemodelgenerator.core.model.SourceLocation;

import java.util.List;

/**
 * Represents resource funstion parameter information.
 *
 * @since 1.0.0
 */
public class ResourceParameter extends ModelElement {

    private final List<String> type;
    private final String name;
    private final String in;
    private final boolean isRequired;

    public ResourceParameter(List<String> type, String name, String in, boolean isRequired,
                             SourceLocation elementLocation, List<ArchitectureModelDiagnostic> diagnostics) {
        super(elementLocation, diagnostics);
        this.type = type;
        this.name = name;
        this.in = in;
        this.isRequired = isRequired;

    }

    public List<String> getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getIn() {
        return in;
    }

    public boolean isRequired() {
        return isRequired;
    }
}
