package sven.workshop.concurrency;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.jupiter.api.Test;

class UglyAgeCacheTest {

  private static final int THREADS = 10;
  static final AgeCache cache = new UglyAgeCache(1000);
  static final CountDownLatch latch = new CountDownLatch(THREADS);

  @Test
  void expectAssertionError() {
    final var service = Executors.newFixedThreadPool(THREADS + 1);
    final var futures = new ArrayList<Future<?>>();
    for (int i = 0; i < THREADS; i++) {
      futures.add(service.submit(new AdjustCacheTask()));
    }
    futures.add(service.submit(this::getFromCache));

    Awaitility.with()
        .pollDelay(Duration.FIVE_SECONDS)
        .and()
        .pollInterval(Duration.TEN_SECONDS)
        .until(() -> futures.stream().noneMatch(Future::isDone));
    service.shutdown();
  }

  private class AdjustCacheTask implements Callable<Void> {

    @Override
    public Void call() throws InterruptedException {
      latch.countDown();
      try {
        latch.await();
        while (true) {
          cache.increase(1);
          cache.decrease(1);
        }
      } catch (final InterruptedException e) {
        throw e;
      }
    }
  }

  private void getFromCache() {
    while (true) {
      System.out.println(cache.getAge());
    }
  }
}
