package ru.android.fractal;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;

public class ThreadPool {
	private Handler handler;
	private ArrayList<RenderThread> list;
	private int size = 0;

	public ThreadPool(Handler handler) {
		this.handler = handler;
		list = new ArrayList<RenderThread>();

		// setSize(8);
	}

	public void send(int m) {
		for (RenderThread thread : list) {
			Handler handler = new Handler(thread);
			Message msg = handler.obtainMessage();
			msg.arg1 = m;
			handler.sendMessage(msg);
		}
	}

	public void send(int m, int arg2) {
		for (RenderThread thread : list) {
			Handler handler = new Handler(thread);
			Message msg = handler.obtainMessage();
			msg.arg1 = m;
			msg.arg2 = arg2;
			handler.sendMessage(msg);
		}
	}

	public void send(RenderThread thread, int m) {
		Handler handler = new Handler(thread);
		Message msg = handler.obtainMessage();
		msg.arg1 = m;
		handler.sendMessage(msg);
	}

	public void stop() {
		send(Messages.MSG_STOP_RENDER);
	}

	public void start() {
		send(Messages.MSG_START_RENDER);
	}

	public void halt() {
		setSize(0);
	}

	public int getSize() {
		return size;
	}

	public void setSize(int value) {
		Log.d("ThreadPool", "setSize=" + value);
		int old = this.size;
		if (value == old) {
			return;
		}
		this.size = value;

		if (value > old) {
			for (int i = 0; i < value - old; i++) {
				list.add(new RenderThread(handler, null));
			}
		}
		if (value < old) {
			for (int i = 0; i < old - value && list.size() > 0; i++) {
				send(list.get(0), Messages.MSG_HALT_THREAD);
				list.remove(0);
			}
		}
	}

	public void setCalcDepth(int value) {
		Log.d("ThreadPool", "setCalcDepth=" + value);
		send(Messages.MSG_CALC_DEPTH, value);
	}
}
