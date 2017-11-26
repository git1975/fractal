package ru.android.fractal;

import java.util.concurrent.ThreadFactory;

/**
 * Created by den on 21.10.2017.
 */

public class RenderThreadFactory implements ThreadFactory {
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setPriority(Thread.MIN_PRIORITY);

        return t;
    }
}