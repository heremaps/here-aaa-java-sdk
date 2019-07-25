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

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.here.account.util.RefreshableResponseProvider.ExpiringResponse;

/**
 * Use this class if you want to always have an unexpired view of an expiring response object 
 * (within reason we expect that each request calls {@link #getUnexpiredResponse()} which is 
 * intended to execute quickly).
 * Strategy is to use a dedicated thread-scheduled executor service, to retrieve and set your 
 * ExpiringResponse to be always up-to-date.
 * 
 * <p>
 * Token provider which handles refreshing tokens on a configurable interval.
 * 
 * <p>
 * Note that this implementation does not incur any synchronization; rather it is assumed the refresh interval
 * is less than the maximum time for token so that while the token is refreshing the current token
 * continues to be valid.
 * 
 * <p>
 * Adapted from HERE DG class RefreshableTokenAuthenticationProvider.
 * 
 * @author kmccrack
 * @author ramsden
 * @author Adam Stuenkel
 */
public class RefreshableResponseProvider<T extends ExpiringResponse> {
  private static final Logger LOG = Logger.getLogger(RefreshableResponseProvider.class.getName());

  /**
   * minimum number of seconds to schedule a refresh
   */
  static final long MIN_REFRESH_SECONDS = 30;
  /**
   * number of seconds to remove from suggested token timeout
   */
  static final long REFRESH_BACKOFF_SECONDS = 60;
  /**
   * number of seconds to wait before refreshing a response when the
   * attempt to refresh failed
   */
  static final long RETRY_FAIL_SECONDS = 5;

  private final ResponseRefresher<T> refreshResponseFunction;
  private final ScheduledExecutorService scheduledExecutorService;
  /**
   * If specified, overrides the normal semantics of scheduling the next refresh 
   * close to the expires in from the refresh function, so that instead the 
   * refresh is scheduled at this fixed interval in milliseconds.
   */
  private final Long refreshIntervalMillis;
  private boolean started;
  private volatile T refreshResponse;  //volatile so consistent across threads
  private Clock clock;

  /**
   * Create a RefreshableResponseProvider with optional refreshIntervalMillis, initialResponse,
   * and refreshResponseFunction.
   * 
   * @param refreshIntervalMillis optional.  only specify during tests, not in real code.  
   *     if you want to ignore the normal response 
   *     <a href="https://tools.ietf.org/html/rfc6749#section-4.2.2">expires_in</a>, 
   *     and instead refresh on a fixed interval not set by the HERE authorization server, 
   *     specify this value in milliseconds.
   * @param initialResponse the initial value of an active response
   * @param refreshResponseFunction the ability to refresh and get a new response prior to the
   *     previous one expiring.
   */
  public RefreshableResponseProvider(
      final Long refreshIntervalMillis,
      final T initialResponse,
      final ResponseRefresher<T> refreshResponseFunction
  ) {
      this(Clock.SYSTEM, refreshIntervalMillis, 
              initialResponse, refreshResponseFunction,
              getScheduledExecutorServiceSize1()
              );
  }
  
  /**
   * Gets a here-auth-refresh ScheduledExecutorService with 1 core pool thread.
   * 
   * @return the ScheduledExecutorService size 1
   */
  public static ScheduledExecutorService getScheduledExecutorServiceSize1() {
      return Executors.newScheduledThreadPool(
              1, new ThreadFactory() {

                  @Override
                  public Thread newThread(Runnable r) {
                      Thread thread = new Thread(r, "here-auth-refresh-%s");
                      thread.setDaemon(true);
                      return thread;
                  }
            
              }
              );

  }
  
  public RefreshableResponseProvider(
          final Clock clock,
          final Long refreshIntervalMillis,
          final T initialResponse,
          final ResponseRefresher<T> refreshResponseFunction,
          final ScheduledExecutorService scheduledExecutorService
      ) {
      Objects.requireNonNull(clock, "clock cannot be null");
      Objects.requireNonNull(initialResponse, "initialResponse cannot be null");
      Objects.requireNonNull(refreshResponseFunction, "refreshResponseFunction cannot be null");
      Objects.requireNonNull(scheduledExecutorService, "scheduledExecutorService cannot be null");
      
      // expires_in cannot be null
      Objects.requireNonNull(initialResponse.getExpiresIn(), 
              "initialResponse.getExpiresIn() cannot be null");
      Objects.requireNonNull(initialResponse.getStartTimeMilliseconds(), 
              "initialResponse.getStartTimeMilliseconds() cannot be null");
      
      this.clock = clock;
      this.refreshIntervalMillis = refreshIntervalMillis;
      this.refreshResponse = initialResponse;
      this.refreshResponseFunction = refreshResponseFunction;

      this.scheduledExecutorService = scheduledExecutorService;
      this.started = true;
      scheduleTokenRefresh(nextRefreshInterval());
  }

  /*---- public -------------------------------------------------------------*/

  /**
   * You can call a response refresher to refresh a previous ExpiringResponse, 
   * to keep it unexpired.
   * 
   * @author kmccrack
   *
   * @param <T> the response type that expires periodically
   */
  public interface ResponseRefresher<T extends ExpiringResponse> {

    /**
     * Invoked when on a specified interval to refresh the token.
     *
     * @param previous the previous token.  Make sure your implementation 
     *      can handle the initial case where previous is null.
     * @return a new token
     */
    T refresh(T previous);
  }
  
  /**
   * Various tokens and keys might be set up in the system to be expiring.
   * This interface allows visibility into the startTime when the object was received, 
   * and the expiresIn the interval after startTime after which the object will be considered expired.
   * 
   * @author kmccrack
   *
   */
  public interface ExpiringResponse {

      /**
       * Seconds until expiration, at time of receipt of this object.
       * 
       * @return the seconds until expiration
       */
      Long getExpiresIn();

      /**
       * Current time milliseconds UTC at the time of construction of this object.
       * In practice, this can generally be considered to be close to the time of receipt of 
       * the object from the server.
       * 
       * @return the start time in milliseconds UTC when this object was received.
       */
      Long getStartTimeMilliseconds();

  }
  
  /**
   * Shutdown the background threads
   */
  public void shutdown() {
    if (started) {
      try {
        LOG.info("Shutting down refresh token thread");
        scheduledExecutorService.shutdown();
      } finally {
        started = false;
      }
    }
  }

  /*---- TokenAuthenticationProvider ----------------------------------------*/

  /*@Override
  public void writeAuthentication(HttpRequest request) {
    TokenUtil.setBearerToken(request, refreshResponse.getAccessToken());
  }*/

  /**
   * Gets the current unexpired response.
   * Obviously you have to use the data within reason, such as within a few 
   * seconds, on your request.
   * It is assumed that your code always comes back to this method, for every 
   * API request.
   * 
   * @return the unexpired response
   */
  public T getUnexpiredResponse() {
      return refreshResponse;
  }

  /*---- private ------------------------------------------------------------*/

  /**
   * Determine the interval the schedule the next refresh
   */
  //@VisibleForTesting
  long nextRefreshInterval() {
    //remove a few seconds to give time to refresh before token expires
    long nextRefreshUsingExpiresIn = TimeUnit.SECONDS.toMillis(
              Math.max(refreshResponse.getExpiresIn() - REFRESH_BACKOFF_SECONDS, MIN_REFRESH_SECONDS)
      );
    if (refreshIntervalMillis != null) {
      return Math.min(nextRefreshUsingExpiresIn, refreshIntervalMillis);
    }

    return nextRefreshUsingExpiresIn;
  }

  /**
   * Schedule the next refresh with the specified timeout duration
   *
   * @param millis  time (msec) in the future to schedule the refresh
   */
  protected void scheduleTokenRefresh(long millis) {
    if (!started) {
      LOG.info("Refresh token thread shutdown, not scheduling");
      return;
    }

    LOG.info("Scheduling next token refresh in " + millis + " milliseconds");
    clock.schedule(scheduledExecutorService, new Runnable() {
        @Override
        public void run() {
          refreshToken();
        }
      }, millis);
  }

  /**
   * Execute the token refresh.
   */
  private void refreshToken() {
    LOG.info(
          String.format(
              "Refreshing HERE auth token (last successful response %s seconds)",
              TimeUnit.SECONDS.convert(clock.currentTimeMillis() - refreshResponse.getStartTimeMilliseconds(), TimeUnit.MILLISECONDS)
          )
    );

    try {
      this.refreshResponse = refreshResponseFunction.refresh(refreshResponse);
      scheduleTokenRefresh(nextRefreshInterval());
    } catch (Exception exp) {
      LOG.warning("Failed to refresh HERE token " + exp);
      scheduleTokenRefresh(
          //try again within time window if call failed
          Math.min(nextRefreshInterval(), TimeUnit.SECONDS.toMillis(RETRY_FAIL_SECONDS))
      );
    }
  }
}

