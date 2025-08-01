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

package io.ballerina.flowmodelgenerator.extension.embeddingprovidermanager;

import com.google.gson.JsonObject;
import io.ballerina.flowmodelgenerator.extension.request.SearchRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Test for searching embedding providers.
 *
 * @since 1.1.0
 */
public class EmbeddingProviderSearchTest extends AbstractLSTest {
    private static final String EMBEDDING_PROVIDER_KIND_NAME = "EMBEDDING_PROVIDER";

    @DataProvider(name = "data-provider")
    @Override
    protected Object[] getConfigsList() {
        return new Object[][]{
                {Path.of("embedding_providers.json")},
                {Path.of("embedding_providers_search_openai.json")}
        };
    }

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        String filePath = sourceDir.resolve(testConfig.source()).toAbsolutePath().toString();
        Map<String, String> queryMap = getQueryMap(testConfig);
        SearchRequest searchRequest = new SearchRequest(EMBEDDING_PROVIDER_KIND_NAME, filePath, null, queryMap);
        JsonObject searchResult = getResponse(searchRequest);
        if (!searchResult.equals(testConfig.expectedEmbeddingProviders())) {
            TestConfig updatedConfig = new TestConfig(testConfig.source(), testConfig.query(), searchResult);
            // updateConfig(configJsonPath, updatedConfig);
            Assert.fail("Test failed. Updated the expected output in " + configJsonPath);
        }
    }

    private static Map<String, String> getQueryMap(TestConfig testConfig) {
        Map<String, String> queryMap = null;
        if (testConfig.query != null) {
            queryMap = new HashMap<>();
            queryMap.put("q", testConfig.query);
        }
        return queryMap;
    }

    @Override
    protected String getResourceDir() {
        return "embedding_provider_manager";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return EmbeddingProviderSearchTest.class;
    }

    @Override
    protected String getApiName() {
        return "search";
    }

    /**
     * Represents the test configuration for the flow model getNodeTemplate API.
     *
     * @param source                     The source file path
     * @param query                      The query string to search
     * @param expectedEmbeddingProviders The expected set of model providers
     */
    private record TestConfig(String source, String query, JsonObject expectedEmbeddingProviders) {

    }
}
