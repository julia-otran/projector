package us.guihouse.projector.other;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EventQueue implements Runnable {
    private Thread thread;
    private boolean running;
    private final Queue<Runnable> eventQueue = new ConcurrentLinkedQueue<>();
    private final List<Runnable> continuousRun = new ArrayList<>();

    private final Object waiter = new Object();

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

    public void onStart() {}
    public void onStop() {}

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
                    Thread.sleep(5);
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

    protected void enqueueContinuous(Runnable r) {
        continuousRun.add(r);
    }
}

