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

package io.ballerina.designmodelgenerator.extension.response;

import io.ballerina.designmodelgenerator.core.model.DesignModel;

/**
 * Represents the response for the design model getDesignModel API.
 *
 * @since 1.0.0
 */
public class GetDesignModelResponse extends AbstractResponse {
    private DesignModel designModel;

    public DesignModel getDesignModel() {
        return designModel;
    }

    public void setDesignModel(DesignModel designModel) {
        this.designModel = designModel;
    }
}
