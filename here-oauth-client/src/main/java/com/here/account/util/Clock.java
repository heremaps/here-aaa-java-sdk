/*
 * Copyright (c) 2016 HERE Europe B.V.
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
package com.here.account.util;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An interface to a clock that can be used to read the 
 * {@link #currentTimeMillis()} and that can be used to 
 * {@link #schedule(ScheduledExecutorService, Runnable, long)} a Runnable 
 * for execution against it.
 * 
 * @author kmccrack
 *
 */
public interface Clock {
    
    /**
     * java.lang.System Clock (digital approximation of wall clock).
     */
    Clock SYSTEM = new Clock() {
        
        /**
         * {@inheritDoc}
         */
        @Override
        public long currentTimeMillis() {
            return System.currentTimeMillis();
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public void schedule(ScheduledExecutorService scheduledExecutorService, 
                Runnable runnable,
                long millisecondsInTheFutureToSchedule
                ) {
            scheduledExecutorService.schedule(
                    runnable,
                    millisecondsInTheFutureToSchedule,
                    TimeUnit.MILLISECONDS
                );

        }
    };

    /**
     * Returns the milliseconds UTC since the epoch.
     * 
     * @return this clock's currentTimeMillis in UTC since the epoch.
     */
    long currentTimeMillis();

    /**
     * Schedules <tt>runnable</tt> the specified <tt>millisecondsInTheFutureToSchedule</tt>
     * using <tt>scheduledExecutorService</tt>.
     * 
     * @param scheduledExecutorService the ScheduledExecutorService to submit the runnable to
     * @param runnable the runnable to execute on a schedule
     * @param millisecondsInTheFutureToSchedule the schedule of milliseconds in the future, 
     *      approximating when the runnable should run.
     */
    void schedule(ScheduledExecutorService scheduledExecutorService, 
            Runnable runnable,
            long millisecondsInTheFutureToSchedule
            );
}

