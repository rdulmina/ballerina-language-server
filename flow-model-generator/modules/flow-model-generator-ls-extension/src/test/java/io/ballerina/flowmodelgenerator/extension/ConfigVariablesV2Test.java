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

package io.ballerina.flowmodelgenerator.extension;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.extension.request.ConfigVariableGetRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.ballerinalang.langserver.util.TestUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Test class for 'getConfigVariables()' API in config API V2.
 *
 * @since 1.0.0
 */
public class ConfigVariablesV2Test extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        ConfigVariablesTestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath),
                ConfigVariablesTestConfig.class);

        String projectPath = sourceDir.resolve(testConfig.project()).toAbsolutePath().toString();

        boolean includeImports = !projectPath.endsWith("simple_type_configs_exclude_imports");
        ConfigVariableGetRequest request = new ConfigVariableGetRequest(projectPath, includeImports);
        ConfigVariableResponse actualResponse = gson.fromJson(getResponse(request), ConfigVariableResponse.class);

        if (!actualResponse.configVariables().equals(testConfig.configVariables())
                || !Objects.equals(actualResponse.errorMsg(), testConfig.errorMsg())) {
//            updateConfig(configJsonPath, new ConfigVariablesTestConfig(
//                    testConfig.project(),
//                    actualResponse.configVariables(),
//                    actualResponse.errorMsg(),
//                    actualResponse.stacktrace()));
            Assert.fail(String.format("Failed test: '%s'", configJsonPath));
        }
    }

    @Override
    protected JsonObject getResponse(Object request, String api) {
        CompletableFuture<?> result = serviceEndpoint.request(api, request);
        String response = TestUtil.getResponseString(result);
        return JsonParser.parseString(response).getAsJsonObject().getAsJsonObject("result");
    }

    @Override
    protected String getResourceDir() {
        return "configurable_variables_v2_get";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return ConfigVariablesV2Test.class;
    }

    @Override
    protected String getApiName() {
        return "getConfigVariables";
    }

    @Override
    protected String getServiceName() {
        return "configEditorV2";
    }

    private record ConfigVariablesTestConfig(String project,
                                             Map<String, Map<String, List<FlowNode>>> configVariables,
                                             String errorMsg,
                                             String stacktrace) {

    }

    private record ConfigVariableResponse(Map<String, Map<String, List<FlowNode>>> configVariables, String errorMsg,
                                          String stacktrace) {

    }
}
