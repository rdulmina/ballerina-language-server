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

package io.ballerina.flowmodelgenerator.extension.response;

import com.google.gson.JsonElement;

/**
 * Represents the response for updating config variables.
 *
 * @since 1.0.0
 */
public class ConfigVariablesUpdateResponse extends AbstractFlowModelResponse {

    private JsonElement textEdits;

    public void setTextEdits(JsonElement textEdits) {
        this.textEdits = textEdits;
    }

    public JsonElement textEdits() {
        return textEdits;
    }
}
