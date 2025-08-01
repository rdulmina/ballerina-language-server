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

package io.ballerina.sequencemodelgenerator.ls.extension;

/**
 * Represents the model diagnostic which consist the validity of the model and the error message.
 *
 * @since 1.0.0
 */

public class ModelDiagnostic {
    private final boolean isIncompleteModel;
    private final String errorMsg;

    public ModelDiagnostic(boolean isIncompleteModel, String errorMsg) {
        this.isIncompleteModel = isIncompleteModel;
        this.errorMsg = errorMsg;
    }

    public boolean isIncompleteModel() {
        return isIncompleteModel;
    }

    public String getErrorMsg() {
        return errorMsg;
    }
}
