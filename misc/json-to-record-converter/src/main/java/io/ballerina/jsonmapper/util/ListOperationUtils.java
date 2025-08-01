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

package io.ballerina.jsonmapper.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Util methods for JSON to record direct converter.
 *
 * @since 1.0.0
 */
public final class ListOperationUtils {

    private ListOperationUtils() {}

    /**
     * This method returns the intersection of Key, Value pairs based on the keys of the pairs.
     *
     * @param mapOne First set of Key, Value pairs to be compared with other
     * @param mapTwo Second set of Key, Value pairs to be compared with other
     * @return {@link Map} Intersection of first and second set of Key Value pairs
     */
    public static <K, V> Map<K, Map.Entry<V, V>> intersection(Map<K, V> mapOne, Map<K, V> mapTwo) {
        Map<K, Map.Entry<V, V>> intersection = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry: mapOne.entrySet()) {
            if (mapTwo.containsKey(entry.getKey())) {
                intersection.put(entry.getKey(), Map.entry(entry.getValue(), mapTwo.get(entry.getKey())));
            }
        }
        return intersection;
    }

    /**
     * This method returns the union of Key, Value pairs based on the keys of the pairs.
     *
     * @param mapOne First set of Key, Value pairs to be compared with other
     * @param mapTwo Second set of Key, Value pairs to be compared with other
     * @return {@link Map} Union of first and second set of Key Value pairs
     */
    public static <K, V> Map<K, V> union(Map<K, V> mapOne, Map<K, V> mapTwo) {
        Map<K, V> union = new LinkedHashMap<>(mapOne);
        for (Map.Entry<K, V> entry: mapTwo.entrySet()) {
            if (!mapOne.containsKey(entry.getKey())) {
                union.put(entry.getKey(), entry.getValue());
            }
        }
        return union;
    }

    /**
     * This method returns the difference of Key, Value pairs based on the keys of the pairs.
     *
     * @param mapOne First set of Key, Value pairs to be compared with other
     * @param mapTwo Second set of Key, Value pairs to be compared with other
     * @return {@link Map} Difference of first and second set of Key Value pairs
     */
    public static <K, V> Map<K, V> difference(Map<K, V> mapOne, Map<K, V> mapTwo) {
        Map<K, V> unionMap = union(mapOne, mapTwo);
        Map<K, Map.Entry<V, V>> intersectionMap = intersection(mapOne, mapTwo);
        for (K key: intersectionMap.keySet()) {
            unionMap.remove(key);
        }
        return unionMap;
    }
}
