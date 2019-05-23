package io.wisetime.connector.logging.aws;

import com.amazonaws.services.logs.model.InputLogEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author thomas.haines
 */
public class BufferedLogWriter {

  private final ConcurrentLinkedQueue<InputLogEvent> messageQueue = new ConcurrentLinkedQueue<>();
  private final ScheduledExecutorService scheduledExecutor;
  private final Consumer<List<InputLogEvent>> eventLogConsumer;
  private final int flushIntervalInSeconds = 4;

  @SuppressWarnings("Duplicates")
  BufferedLogWriter(Consumer<List<InputLogEvent>> eventLogConsumer) {
    this.eventLogConsumer = eventLogConsumer;

    // SCHEDULER
    ThreadFactory threadFactory = r -> {
      Thread thread = Executors.defaultThreadFactory().newThread(r);
      thread.setName("wise-log-" + UUID.randomUUID().toString().substring(0, 7));
      thread.setDaemon(true);
      return thread;
    };
    scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);

    scheduledExecutor.scheduleWithFixedDelay(
        this::flushLogs,
        flushIntervalInSeconds,
        flushIntervalInSeconds,
        TimeUnit.SECONDS);
  }

  public void stop() throws InterruptedException {
    scheduledExecutor.shutdown();
    scheduledExecutor.awaitTermination(6 * flushIntervalInSeconds, TimeUnit.SECONDS);
    flushLogs();
  }

  void addMessageToQueue(InputLogEvent msg) {
    messageQueue.offer(msg);
  }

  void flushLogs() {
    try {
      boolean sentLimit;
      do {
        sentLimit = processToLimit();
      } while (sentLimit);
    } catch (Exception e) {
      System.err.println("AWS lib internal error " + e);
    }
  }

  private boolean processToLimit() {
    // process up to X messages per POST
    final AtomicBoolean limitReached = new AtomicBoolean(false);
    List<InputLogEvent> eventList = createListFromQueue(limitReached);
    eventLogConsumer.accept(eventList);
    return limitReached.get();
  }

  /**
   * <pre>
   *   a. The maximum batch size is 1,048,576 bytes, and this size is calculated as the sum of all event messages in UTF-8,
   *      plus 26 bytes for each log event.
   *   b.
   * <pre>
   * @param limitReached Set to true if limit reached
   * @return List to send to AWS
   */
  private List<InputLogEvent> createListFromQueue(AtomicBoolean limitReached) {
    final List<InputLogEvent> eventList = new ArrayList<>();
    // The maximum number of log events in a batch is 10,000.
    final int maxLogEvents = 8000;
    final AtomicInteger byteCount = new AtomicInteger();

    InputLogEvent logEvent;
    while ((logEvent = messageQueue.poll()) != null) {
      if (logEvent.getMessage() != null) {
        eventList.add(logEvent);
        if (eventList.size() >= maxLogEvents) {
          // log row limit reached
          limitReached.set(true);
          return eventList;
        }

        int logBundleSize = byteCount.addAndGet(logEvent.getMessage().getBytes(StandardCharsets.UTF_8).length + 26);
        final int maxAwsPutSize = 1_048_576 - 48_000;
        if (logBundleSize > maxAwsPutSize) {
          // message size in bytes limit reached
          limitReached.set(true);
          return eventList;
        }
      }
    }

    return eventList;
  }


}
