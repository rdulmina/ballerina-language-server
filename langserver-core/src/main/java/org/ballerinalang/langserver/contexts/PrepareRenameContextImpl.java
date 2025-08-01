/*
 * Copyright (c) 2021, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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
package org.ballerinalang.langserver.contexts;

import org.ballerinalang.langserver.LSContextOperation;
import org.ballerinalang.langserver.commons.LSOperation;
import org.ballerinalang.langserver.commons.LanguageServerContext;
import org.ballerinalang.langserver.commons.PrepareRenameContext;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;

/**
 * Implementation of {@link PrepareRenameContext}.
 *
 * @since 1.0.0
 */
public class PrepareRenameContextImpl extends ReferencesContextImpl implements PrepareRenameContext {

    PrepareRenameContextImpl(LSOperation operation,
                             String fileUri,
                             WorkspaceManager wsManager,
                             Position cursorPos,
                             LanguageServerContext serverContext,
                             CancelChecker cancelChecker) {
        super(operation, fileUri, wsManager, cursorPos, serverContext, cancelChecker);
    }

    /**
     * Represents Language server signature help context Builder.
     *
     * @since 1.0.0
     */
    protected static class PrepareRenameContextBuilder extends PositionedOperationContextBuilder<PrepareRenameContext> {

        public PrepareRenameContextBuilder(LanguageServerContext serverContext) {
            super(LSContextOperation.TXT_PREPARE_RENAME, serverContext);
        }

        @Override
        public PrepareRenameContext build() {
            return new PrepareRenameContextImpl(this.operation,
                    this.fileUri,
                    this.wsManager,
                    this.position,
                    this.serverContext,
                    this.cancelChecker);
        }

        @Override
        public PrepareRenameContextBuilder self() {
            return this;
        }
    }
}
