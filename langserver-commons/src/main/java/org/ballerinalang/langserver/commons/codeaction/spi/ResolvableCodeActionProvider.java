/*
 * Copyright (c) 2022, WSO2 LLC. (http://wso2.com) All Rights Reserved.
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
package org.ballerinalang.langserver.commons.codeaction.spi;

import org.ballerinalang.langserver.commons.CodeActionResolveContext;
import org.ballerinalang.langserver.commons.codeaction.ResolvableCodeAction;
import org.eclipse.lsp4j.CodeAction;

/**
 * Represents the SPI interface for the Ballerina Resolvable Code Action Provider.
 *
 * @since 1.0.0
 */
public interface ResolvableCodeActionProvider extends LSCodeActionProvider {

    /**
     * Returns the resolvable code action.
     *
     * @param codeAction     Resolvable code action
     * @param resolveContext Code action resolve context
     * @return code action
     */
    default CodeAction resolve(ResolvableCodeAction codeAction,
                               CodeActionResolveContext resolveContext) {
        return codeAction;
    }
}
