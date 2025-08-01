/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
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
package io.ballerina.shell.service;

/**
 * Request format for get Result from BalShell endpoint.
 *
 * @since 1.0.0
 */
public class BalShellGetResultRequest {
    private final String source;

    public BalShellGetResultRequest(String source) {
        this.source = source;
    }

    /**
     * Returns source snippet.
     *
     * @return source snippet in the request.
     */
    public String getSource() {
        return source;
    }
}
