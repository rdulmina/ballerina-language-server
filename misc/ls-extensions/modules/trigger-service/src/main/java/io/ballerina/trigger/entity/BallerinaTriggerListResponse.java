/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.trigger.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Response to be sent with the triggers list.
 *
 * @since 1.0.0
 */
public class BallerinaTriggerListResponse {
    private List<Trigger> central;
    private List<Trigger> local;

    public BallerinaTriggerListResponse() {
        central = new ArrayList<>();
        local = new ArrayList<>();
    }

    public BallerinaTriggerListResponse(List<Trigger> central, List<Trigger> local) {
        this.central = central;
        this.local = local;
    }

    public List<Trigger> getCentralTriggers() {
        return central;
    }

    public void setCentralTriggers(List<Trigger> central) {
        if (central != null && !central.isEmpty()) {
            this.central = central;
        }
    }

    public void addCentralTriggers(List<Trigger> central) {
        if (central != null && !central.isEmpty()) {
            this.central.addAll(central);
        }
    }

    public void addInBuiltTriggers(List<Trigger> inBuiltTriggers) {
        if (inBuiltTriggers != null && !inBuiltTriggers.isEmpty()) {
            this.central.addAll(inBuiltTriggers);
        }
    }

    public List<Trigger> getLocalTriggers() {
        return local;
    }

    public void setLocalTriggers(List<Trigger> local) {
        if (local != null && !local.isEmpty()) {
            this.local = local;
        }
    }
}
