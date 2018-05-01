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
package com.here.account.util;

import java.util.concurrent.ScheduledExecutorService;

/**
 * A SettableSystemClock starts off using the {@link Clock#SYSTEM},
 * but any time you set a "corrected" value via {@link #setCurrentTimeMillis(long)},
 * will record a correction and work off that value instead.
 * This Clock assumes that once a currentTimeMilliseconds is
 * passed in as the "correct" value, the {@link Clock#SYSTEM} Clock will
 * continue to process at the correct or near-correct pace.
 * If it doesn't, the currentTimeMilliseconds may need to be
 * set again periodically to the correct value.
 */
public class SettableSystemClock implements SettableClock {

    private long behindMillis;

    public SettableSystemClock() {
        this.behindMillis = 0L;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long currentTimeMillis() {
        return Clock.SYSTEM.currentTimeMillis() + behindMillis;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void schedule(ScheduledExecutorService scheduledExecutorService, Runnable runnable, long millisecondsInTheFutureToSchedule) {
        Clock.SYSTEM.schedule(scheduledExecutorService, runnable, millisecondsInTheFutureToSchedule);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentTimeMillis(long correctCurrentTimeMillis) {
        // currentTimeMillis is an outside source
        behindMillis = correctCurrentTimeMillis - Clock.SYSTEM.currentTimeMillis();
    }

}

