package us.guihouse.projector.other;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EventQueue implements Runnable {
    private Thread thread;
    private boolean running;
    private final Queue<Runnable> eventQueue = new ConcurrentLinkedQueue<>();
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
                synchronized (waiter) {
                    waiter.wait(1000);
                }
                do {
                    r = eventQueue.poll();
                    if (r != null) {
                        r.run();
                    }
                } while (r != null);
            } catch (InterruptedException e) {
                e.printStackTrace();
                running = false;
            }
        }

        onStop();
    }
}
