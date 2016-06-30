package com.here.account.util;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

