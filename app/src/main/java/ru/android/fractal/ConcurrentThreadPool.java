package ru.android.fractal;

import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ConcurrentThreadPool implements ExecutorService, Runnable, Handler.Callback {
	private final ConcurrentLinkedQueue<RenderArea> queue = null;
	private boolean isTerminated = false;
	private ArrayList<RenderArea> runningTasks = new ArrayList<RenderArea>();
	private ArrayList<RenderArea> waitingTasks = new ArrayList<RenderArea>();
	private int size = 0;
	Handler.Callback callback;

	public ConcurrentThreadPool(int size, Handler.Callback callback){
		this.size = size;
		this.callback = callback;
		new Thread(this).start();
	}

	@Override
	public void run() {
		while(!isTerminated){
			if(runningTasks.size() < getSize()) {
				//RenderArea item = queue.poll();
				RenderArea item = null;
				if(waitingTasks.size() > 0) {
					item = waitingTasks.get(0);
					waitingTasks.remove(0);
				}
				if (item != null) {
					runningTasks.add(item);
					//new Thread(item).start();
					new RenderTask(this, new Rect(item.recti[0], item.recti[1],
							item.recti[2], item.recti[3])).execute(item);
				}
			}

			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		if(msg.what == Messages.MSG_AREA_BEGIN_RECT) {
			if(callback != null){
				callback.handleMessage(msg);
			}
		} else if(msg.what == Messages.MSG_AREA_COMPLETE) {
			runningTasks.remove(msg.obj);
			if(callback != null){
				callback.handleMessage(msg);
			}
			return true;
		}

		return false;
	}

	public void stop() {
		while(runningTasks.size() > 0){
			runningTasks.remove(0);
		}
		while(waitingTasks.size() > 0){
			waitingTasks.remove(0);
		}
	}

	@Override
	public void shutdown() {

	}

	@Override
	public List<Runnable> shutdownNow() {
		return null;
	}

	@Override
	public boolean isShutdown() {
		return false;
	}

	@Override
	public boolean isTerminated() {
		return isTerminated;
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return false;
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return null;
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		return null;
	}

	@Override
	public Future<?> submit(Runnable task) {
		return null;
	}

	public void submit(RenderArea task) {
		//queue.add(task);
		waitingTasks.add(task);
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return null;
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
		return null;
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		return null;
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return null;
	}

	@Override
	public void execute(Runnable command) {

	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

}
