package com.here.account.util;

import java.util.ArrayList;
import java.util.List;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledExecutorService;

import com.here.account.util.Clock;

public class MyClock implements Clock {
    private final long startTimeMillis;
    private long currentTimeMillis;
    
    public MyClock() {
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
