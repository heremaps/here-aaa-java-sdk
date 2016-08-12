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

import java.util.ArrayList;
import java.util.List;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledExecutorService;


/**
 * A TestClock starts at currentTimeMillis = System.currentTimeMillis(), but you can call 
 * {@link #setCurrentTimeMillis(long)} at any time to directly set the observed currentTimeMillis, to any value 
 * past or present.  Also, calls to {@link #schedule(ScheduledExecutorService, Runnable, long)} a Runnable are 
 * held in-memory against the clock.  Whenever {@link #setCurrentTimeMillis(long)} is called, the "scheduled" runnables 
 * that have a "scheduled" time in the "past", execute immediately before the method returns.  This way you can 
 * schedule something for 12 hours in the future, then move the clock forward 12 hours and 1 minute, and your Runnable 
 * will have been run.
 * 
 * @author kmccrack
 *
 */
public class TestClock implements Clock {
    private final long startTimeMillis;
    private long currentTimeMillis;
    
    public TestClock() {
        this.startTimeMillis = System.currentTimeMillis();
        this.currentTimeMillis = startTimeMillis;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }
    
    public void setCurrentTimeMillis(long currentTimeMillis) {
        this.currentTimeMillis = currentTimeMillis;
        runAllPending();
    }
    
    protected void runAllPending() {
        List<Entry<Runnable, Long>> newList = new ArrayList<Entry<Runnable, Long>>();
        for (int i = 0; i < scheduledRunnables.size(); i++) {
            Entry<Runnable, Long> entry = scheduledRunnables.get(i);
            long runAt = entry.getValue();
            if (currentTimeMillis() >= runAt) {
                Runnable runnable = entry.getKey();
                runnable.run();
            } else {
                newList.add(entry);
            }
        }
        scheduledRunnables = newList;
    }

    @Override
    public long currentTimeMillis() {
        return currentTimeMillis;
    }
    
    private List<Entry<Runnable, Long>> scheduledRunnables = new ArrayList<Entry<Runnable, Long>>();

    @Override
    public void schedule(ScheduledExecutorService scheduledExecutorService, Runnable runnable,
            long millisecondsInTheFutureToSchedule) {
        Entry<Runnable, Long> entry = new SimpleImmutableEntry<Runnable, Long>(runnable, 
                currentTimeMillis + millisecondsInTheFutureToSchedule);
        scheduledRunnables.add(entry);
    }
    
}
