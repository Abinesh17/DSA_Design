package cache.lru;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiter {

    private Map<String,Bucket> userVsBucketMap;
    private int maxRequests;
    private int timeWindow;
    private int maxCredits;
    private final ScheduledExecutorService cleanupService = Executors.newScheduledThreadPool(1);

    public RateLimiter(int maxRequests, int timeWindow, int maxCredits)
    {
        this.maxRequests = maxRequests;
        this.timeWindow = timeWindow;
        this.maxCredits = maxCredits;
        this.userVsBucketMap = new ConcurrentHashMap<>();

        // separate thread to cleanup stale data
        this.cleanupService.scheduleAtFixedRate(this::cleanUpBuckets , timeWindow, timeWindow, TimeUnit.SECONDS);
    }

    private void cleanUpBuckets()
    {
        long currTime = System.currentTimeMillis();
        this.userVsBucketMap.entrySet().removeIf(entry -> entry.getValue().endTime < currTime);
    }

    public synchronized boolean rateLimit(String userId)
    {
        // check for user bucket
        long currTime = System.currentTimeMillis();
        if(!userVsBucketMap.containsKey(userId))
        {
            userVsBucketMap.put(userId, new Bucket(currTime, currTime+(timeWindow * 1000L), 1, maxCredits));
            return false;
        }

        // if present, validate if its within window
        Bucket bucket = userVsBucketMap.get(userId);
        if(currTime < bucket.endTime)
        {
            if(bucket.getCurrentCount() < maxRequests)
            {
                bucket.incrementCount();
                return false;
            }
            else if(bucket.getCurrentCredits() > 0)
            {
                bucket.decrementCredits();
                return false;
            }
            else
            {
                return true;
            }
        }

        //if present, but outside of window
        bucket.reset(currTime, currTime+(timeWindow * 1000L), maxCredits);
        return false;
    }

    public static class Bucket
    {
        private long startTime;
        private long endTime;
        private AtomicInteger currentCount;
        private AtomicInteger currentCredits;
        public Bucket(long startTime, long endTime, int currentCount, int currentCredits)
        {
            this.startTime = startTime;
            this.endTime = endTime;
            this.currentCount = new AtomicInteger(currentCount);
            this.currentCredits = new AtomicInteger(currentCredits);
        }

        public void incrementCount()
        {
            this.currentCount.incrementAndGet();
        }

        public void decrementCredits()
        {
            this.currentCredits.decrementAndGet();
        }

        public int getCurrentCount() {
            return currentCount.get();
        }

        public int getCurrentCredits() {
            return currentCredits.get();
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public void reset(long startTime, long endTime, int maxCredits)
        {
            this.startTime = startTime;
            this.endTime = endTime;
            this.currentCount.set(1);
            this.currentCredits.set(maxCredits);
        }
    }

    public static void main(String[] args) {

        RateLimiter rateLimiter = new RateLimiter(5, 5, 3);
        String userID = "user1";
        for(int i=1;i<=17;i++)
        {
            if(i==10)
            {
                try {
                    Thread.sleep(6000);
                } catch(Exception ex)
                {
                    System.out.println("Exception!");
                }
            }
            System.out.println("i="+i+" - "+rateLimiter.rateLimit(userID));
        }
    }
}
