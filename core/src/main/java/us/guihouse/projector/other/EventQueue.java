package us.guihouse.projector.other;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EventQueue implements Runnable {
    private final long sleepInterval;
    private Thread thread;
    private boolean running;
    private final Queue<Runnable> eventQueue = new ConcurrentLinkedQueue<>();
    private final List<Runnable> continuousRun = new ArrayList<>();

    private final Object waiter = new Object();
    private Runnable startRunnable;
    private Runnable stopRunnable;

    public void setStartRunnable(Runnable r) {
        this.startRunnable = r;
    }

    public void setStopRunnable(Runnable r) {
        this.stopRunnable = r;
    }

    public EventQueue(long sleepInterval) {
        this.sleepInterval = sleepInterval;
    }

    public EventQueue() {
        this(5);
    }

    public void init() {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        if (!running) {
            return;
        }

        running = false;
        synchronized (waiter) {
            waiter.notify();
        }
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        continuousRun.clear();
        eventQueue.clear();
    }

    public void enqueueForRun(Runnable r) {
        eventQueue.add(r);
        synchronized (waiter) {
            waiter.notify();
        }
    }

    public void onStart() {
        if (startRunnable != null) {
            startRunnable.run();
        }
    }
    public void onStop() {
        if (stopRunnable != null) {
            stopRunnable.run();
        }
    }

    @Override
    public void run() {
        onStart();

        Runnable r;

        while (running) {
            try {
                if (continuousRun.isEmpty()) {
                    synchronized (waiter) {
                        waiter.wait(500);
                    }
                } else {
                    Thread.sleep(sleepInterval);
                }

                do {
                    r = eventQueue.poll();
                    if (r != null) {
                        r.run();
                    }
                } while (r != null);

                for (Runnable continuous : continuousRun) {
                    continuous.run();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
                running = false;
            }
        }

        onStop();
    }

    public void enqueueContinuous(Runnable r) {
        continuousRun.add(r);
    }

    public void removeContinuous(Runnable r) {
        continuousRun.remove(r);
    }
}

