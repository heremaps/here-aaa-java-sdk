/*
 * Copyright (c) 2018 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.here.account.oauth2.bo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TimestampResponse {

    /**
     * timestamp.
     * The current time in seconds, since January 1, 1970.
     * This can be used when the client needs to know the correct time for any subsequent calls,
     * for client signed requests, for example.
     */
    @JsonProperty("timestamp")
    private final Long timestamp;

    public TimestampResponse() {
        this(null);
    }

    public TimestampResponse(Long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the timestamp.
     * The current time in seconds, since January 1, 1970.
     * This can be used when the client needs to know the correct time for any subsequent calls,
     * for client signed requests, for example.
     *
     * @return the timestamp.
     */
    public Long getTimestamp() {
        return timestamp;
    }

}
