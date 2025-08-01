/*
 * Copyright (c) 2023, WSO2 LLC. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ballerinalang.langserver.codeaction;

import org.ballerinalang.langserver.commons.capability.InitializationOptions;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.util.FileUtils;
import org.ballerinalang.langserver.util.TestUtil;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Test class to test the functionality of the extract to function code action with quick pick and positional
 * rename capability.
 * @since 1.0.0
 */
public class ExtractToFunctionCodeActionWithPositionalRenameAndQuickPickTest extends AbstractCodeActionTest {

    @Override
    protected void setupLanguageServer(TestUtil.LanguageServerBuilder builder) {
        builder.withInitOption(InitializationOptions.KEY_QUICKPICK_SUPPORT, true);
        builder.withInitOption(InitializationOptions.KEY_POSITIONAL_RENAME_SUPPORT, true);
    }

    @Override
    protected Path getConfigJsonPath(String configFilePath) {
        return FileUtils.RES_DIR.resolve("codeaction")
                .resolve(getResourceDir())
                .resolve("config-positional-rename-and-quickpick")
                .resolve(configFilePath);
    }

    @Test(dataProvider = "codeaction-data-provider")
    @Override
    public void test(String config) throws IOException, WorkspaceDocumentException {
        super.test(config);
    }

    @DataProvider(name = "codeaction-data-provider")
    @Override
    public Object[][] dataProvider() {
        return new Object[][]{
                {"extractToFunctionExpr1.json"},
                {"extractToFunctionExpr2.json"},
                {"extractToFunctionExpr3.json"},
                {"extractToFunctionExpr4.json"},
                {"extractToFunctionExpr5.json"},
                {"extractToFunctionExpr6.json"},
                {"extractToFunctionExpr7.json"},
                {"extractToFunctionExpr8.json"}
        };
    }

    @Override
    public String getResourceDir() {
        return "extract-to-function";
    }
}
