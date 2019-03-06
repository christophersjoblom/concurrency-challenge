package sven.workshop.concurrency;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DangerousAgeCacheTest {

  static final int THREADS = 2;
  static final CountDownLatch latch = new CountDownLatch(THREADS);
  static final AgeCache cache = new DangerousAgeCache(1000);

  @Test
  void expectAssertionError() {
    final var service = Executors.newFixedThreadPool(THREADS+1);
    final var futures = new ArrayList<Future<?>>();
    for (int i = 0; i < THREADS; i++) {
      futures.add(service.submit(new AdjustCacheTask()));
    }
    futures.add(service.submit(this::getFromCache));

    Awaitility.await().until(() -> futures.stream().anyMatch(Future::isDone));
    final var doneFuture = futures.stream().filter(Future::isDone).findAny();
    service.shutdown();
    Assertions.assertThrows(ExecutionException.class, () -> doneFuture.get().get());
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
