/*
 * Copyright (c) 2011 Yahoo! Inc. All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *          http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License. See accompanying LICENSE file. 
 */
package io.s4.example.counter;

import io.s4.core.Event;

public class CountEvent extends Event {

    private String key;
    private long count;
    
    public CountEvent() {
        
    }

    CountEvent(String key, long count) {
        this.key = key;
        this.count = count;
    }

    CountEvent(String key, long count, long time) {
        super(time);
        this.key = key;
        this.count = count;
    }

   
    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the count
     */
    public long getCount() {
        return count;
    }

    public String toString() {
        return String.format("Key: " + key + ", Count: %08d", count);
    }
}
