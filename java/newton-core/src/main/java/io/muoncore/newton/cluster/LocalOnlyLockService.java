package io.muoncore.newton.cluster;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class LocalOnlyLockService implements LockService {

  private Executor pool = Executors.newFixedThreadPool(100);

  @Override
  public void executeAndRepeatWithLock(String name, LockedTask exec) {
    log.info("Starting to wait on the lock " + name);

    pool.execute(() -> {
      Thread.currentThread().setName("Lock-" + name);
      log.info("In executor " + name);
      Lock lock = new ReentrantLock();

      while(true) {
        log.info("Waiting on the lock " + name);

        lock.lock();
        CountDownLatch latch = new CountDownLatch(1);
        try {
          log.info("Obtained global lock '{}', executing local task on this node", name);
          exec.execute(() -> {
            synchronized (lock) {
              try {
                lock.unlock();
                latch.countDown();
              } catch (IllegalMonitorStateException e) {
                log.info("{} is already unlocked", name);
                latch.countDown();
              }
            }
          });
          latch.await();
        } catch (Exception ex) {
          log.warn("Locked process has failed with an exception {}, and {} has been unlocked", ex.getMessage(), name);
          log.debug("Locking Process failed with exception", ex);
        } finally {
          synchronized (lock) {
            lock.unlock();
          }
          log.info("Global lock '{}' released! Will try again in 500ms", name);
          try {
            Thread.sleep(500);
          } catch (InterruptedException e) {
            log.error("Failure while waiting", e);
          }
        }
      }
    });
  }
}
