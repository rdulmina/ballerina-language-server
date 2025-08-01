/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.flowmodelgenerator.extension.request;

import com.google.gson.JsonElement;

/**
 * Represents a request to get the data mapper model for types.
 *
 * @param filePath file path of the source file
 * @param codedata Details of the node
 * @param flowNode The node representation in the let variable
 * @param index    The index of the let variable in variable
 *
 * @since 1.0.0
 */
public record DataMapperSubMappingSourceRequest(String filePath, JsonElement codedata, JsonElement flowNode,
                                                int index) {
}
