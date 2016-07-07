package com.here.account.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.here.account.util.RefreshableResponseProvider.ExpiringResponse;
import com.here.account.util.RefreshableResponseProvider.ResponseRefresher;

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
    public void test_shutdown_multiple() {
        for (int i = 0; i < 3; i++) {
            tearDown();
        }
    }
    
}
