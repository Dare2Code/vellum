/*
       Vellum by Evan Summers under Apache Software License 2.0 from ASF.

       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements. See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.  
       
 */
package vellum.datatype;

import vellum.format.ArgFormats;
import java.util.HashMap;

/**
 *
 * @author evan.summers
 */
public class UniqueMap<K, V> extends HashMap<K, V> {

    public V put(K key, V value) {
        if (key == null) {
            throw new NullPointerException(ArgFormats.formatter.formatArgs(key, value));
        }
        V previous = super.put(key, value);
        if (previous != null) {
            throw new IllegalArgumentException(key.toString());
        }
        return previous;
    }
}