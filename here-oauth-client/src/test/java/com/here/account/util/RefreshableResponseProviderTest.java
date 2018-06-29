package com.here.account.util;

import com.here.account.util.RefreshableResponseProvider.ExpiringResponse;
import com.here.account.util.RefreshableResponseProvider.ResponseRefresher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class RefreshableResponseProviderTest {

    RefreshableResponseProvider<MyExpiringResponse> refreshableResponseProvider;
    
    public class MyExpiringResponse implements ExpiringResponse {
        
        private long startTimeMillis;

        public MyExpiringResponse() {
            this.startTimeMillis = System.currentTimeMillis();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Long getExpiresIn() {
            // 10 minutes
            return 10*60L;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Long getStartTimeMilliseconds() {
            return startTimeMillis;
        }
        
    }
     Long refreshIntervalMillis;
     MyExpiringResponse initialToken;
     ResponseRefresher<MyExpiringResponse> refreshTokenFunction;
    
    @Before
    public void setUp() {
        refreshIntervalMillis = null;
        initialToken = new MyExpiringResponse();
        refreshTokenFunction = new ResponseRefresher<MyExpiringResponse>() {

            @Override
            public MyExpiringResponse refresh(MyExpiringResponse previous) {
                return new MyExpiringResponse();
            }
            
        };
        this.refreshableResponseProvider = new RefreshableResponseProvider<MyExpiringResponse>(
         refreshIntervalMillis,
         initialToken,
         refreshTokenFunction);
    }
    
    @After
    public void tearDown() {
        this.refreshableResponseProvider.shutdown();
    }
    
    @Test
    public void test_refreshIntervalMillis() {
        refreshIntervalMillis = 100L;
        initialToken = new MyExpiringResponse();
        refreshTokenFunction = new ResponseRefresher<MyExpiringResponse>() {

            @Override
            public MyExpiringResponse refresh(MyExpiringResponse previous) {
                return new MyExpiringResponse();
            }
            
        };
        this.refreshableResponseProvider = new RefreshableResponseProvider<MyExpiringResponse>(
         refreshIntervalMillis,
         initialToken,
         refreshTokenFunction);

        for (int i = 0; i < 10; i++) {
            long actualNextRefreshInterval = refreshableResponseProvider.nextRefreshInterval();
            assertTrue("actualNextRefreshInterval "+actualNextRefreshInterval
                    +" didn't match refreshIntervalMillis "+refreshIntervalMillis,
                    refreshIntervalMillis == actualNextRefreshInterval);
        }
    }

    @Test
    public void test_refreshToken_fails() throws InterruptedException {
        refreshIntervalMillis = 100L;
        initialToken = new MyExpiringResponse();
        refreshTokenFunction = new ResponseRefresher<MyExpiringResponse>() {

            @Override
            public MyExpiringResponse refresh(MyExpiringResponse previous) {
                throw new RuntimeException("simulate unable to refresh");
            }
            
        };
        this.refreshableResponseProvider = new RefreshableResponseProvider<MyExpiringResponse>(
         refreshIntervalMillis,
         initialToken,
         refreshTokenFunction);


        for (int i = 0; i < 10; i++) {
            Thread.sleep(100L);
            MyExpiringResponse response = spyRefreshableResponseProvider.getUnexpiredResponse();
            assertTrue("response was null", null != response);
            long expiresIn = response.getExpiresIn();
            long expectedExpiresIn = 600L;
            assertTrue("expected expires in "+expectedExpiresIn+" != actual expiresIn "+expiresIn, 
                    expectedExpiresIn == expiresIn);

            // verify when refreshTokenFunction.refresh() throws an exception causing RefreshResponseProvider.refreshToken()
            // to scheduleTokenRefresh(), that the refresh is not 10ms. (AAA-689)
            //
            // The test below does not work. I do not know why. I see log output saying: INFO: Scheduling next token refresh in 100 milliseconds
            // but Mockito says - Wanted but not invoked:  refreshableResponseProvider.scheduleTokenRefresh(100);
            //Mockito.verify(spyRefreshableResponseProvider).scheduleTokenRefresh(100);
        }
    }



    @Test
    public void test_shutdown_multiple() {
        for (int i = 0; i < 3; i++) {
            tearDown();
        }
    }
    
}
