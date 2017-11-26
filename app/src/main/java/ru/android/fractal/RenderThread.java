package ru.android.fractal;

import android.graphics.Color;
import android.os.Handler;
import android.os.Message;

public class RenderThread extends Thread implements Handler.Callback {
	private int maxIteration = 50;

	private boolean paused = false;
	private boolean stopped = false;
	private Handler handler;
	private RenderArea area;
	private int[] calcBuffer;
	private int[] resultBuffer;

	public RenderThread(Handler handler, RenderArea area) {
		super();
		this.area = area;
		this.handler = handler;
		this.setPriority(MIN_PRIORITY);
		this.start();
	}

	@Override
	public boolean handleMessage(Message msg) {
		if (msg.arg1 == Messages.MSG_STOP_RENDER) {
			paused = true;
		}
		if (msg.arg1 == Messages.MSG_START_RENDER) {
			paused = false;
		}
		if (msg.arg1 == Messages.MSG_HALT_THREAD) {
			stopped = true;
		}
		if (msg.arg1 == Messages.MSG_CALC_DEPTH) {
			maxIteration = msg.arg2;
			// Log.d("RenderThread", "maxIteration=" + maxIteration);
		}
		return true;
	}

	@Override
	public void run() {
		//while (!stopped) {
			//RenderArea area = Queue.getInstance().getNext();
			if (area != null) {
				doRender(area);
			}
			try {
				this.sleep(10);
			} catch (Exception e) {

			}
		//}
	}

	private void send(RenderArea area, int m) {
		area.data = resultBuffer;
		Message msg = handler.obtainMessage();
		msg.arg1 = m;
		msg.obj = area;
		handler.sendMessage(msg);
	}

	private void doRender(RenderArea area) {
		// Log.d("RenderThread.doRender", "area|" + area.toString());
		int i = 0;
		int j = 0;

		// Speeds buffer
		calcBuffer = new int[area.width * area.height];
		resultBuffer = new int[area.width * area.height];
		// очищаем
		for (j = 0; j < area.height; j++) {
			for (i = 0; i < area.width; i++) {
				calcBuffer[j * area.width + i] = -1;
			}
		}
		int X1, Y1, X2, Y2;
		X1 = area.recti[0];
		Y1 = area.recti[1];
		X2 = area.recti[2];
		Y2 = area.recti[3];
		boolean b = true;
		int lastColor = getColorFPUDouble(area, 0, 0);

		send(area, Messages.MSG_AREA_BEGIN);

		b = b && renderLine(area, lastColor, X1, Y1, X2, Y2, 1, 0);
		b = b && renderLine(area, lastColor, X1, Y1, X2, Y2, 0, 1);
		b = b && renderLine(area, lastColor, X2, Y1, X2, Y2, 0, 1);
		b = b && renderLine(area, lastColor, X1, Y2, X2, Y2, 1, 0);
		// Render border
		// Log.d("doRender", "lastColor=" + lastColor);
		if (b) {
			// Log.d("renderLine=true", "lastColor=" + lastColor + "|" +
			// area.toString());
			// lastColor = 200;
			for (j = 0; j < area.height; j++) {
				for (i = 0; i < area.width; i++) {
					//calcBuffer[j * w + i] = Color.rgb(0, 0, 200);
					calcBuffer[j * area.width + i] = Color.rgb(lastColor * lastColor,
							lastColor + lastColor, lastColor);
				}
			}
			copyResultBuffer(area);
			send(area, Messages.MSG_AREA_COMPLETE);
			return;
		}

		// Расчет
		int c = 0;
		int pos = 0;
		for (j = 0; j < area.height && !paused && !b; j++) {
			for (i = 0; i < area.width && !paused && !b; i++) {
				pos = j * area.width + i;
				if (calcBuffer[pos] == -1) {
					if(area.precision == 1){
						c = getColorFPUFloat(area, i, j);
					}
					if(area.precision == 2){
						c = getColorFPUDouble(area, i, j);
					}
					calcBuffer[pos] = Color.rgb(c * c, c + c, c);
				}
			}
			if((j%20)==0 && j > 0 && area.showPartial){
				copyResultBuffer(area);
				send(area, Messages.MSG_AREA_COMPLETE_PART);

			}
		}

		if (!paused) {
			copyResultBuffer(area);
			send(area, Messages.MSG_AREA_COMPLETE);
		}
	}

	private int getColorFPUDouble(RenderArea area, int vX, int vY) {
		int result, vC = 0;
		double vP, vQ, DeltaP, DeltaQ, Pmin, Pmax, Qmin, Qmax;
		Pmin = area.rectd[0];
		Pmax = area.rectd[2];
		Qmin = area.rectd[1];
		Qmax = area.rectd[3];
		DeltaP = (Pmax - Pmin) / (area.width - 1);
		DeltaQ = (Qmax - Qmin) / (area.height - 1);

		vP = Pmin + DeltaP * vX;
		vQ = Qmin + DeltaQ * vY;

		vC = renderPointDouble(vP, vQ) * 4;

		if (vC == maxIteration * 4) {
			result = 0;
		} else {
			result = vC;
		}

		return result;
	}
	
	private int getColorFPUFloat(RenderArea area, int vX, int vY) {
		int result, vC = 0;
		float vP, vQ, DeltaP, DeltaQ, Pmin, Pmax, Qmin, Qmax;
		Pmin = area.rectf[0];
		Pmax = area.rectf[2];
		Qmin = area.rectf[1];
		Qmax = area.rectf[3];
		DeltaP = (Pmax - Pmin) / (area.width - 1);
		DeltaQ = (Qmax - Qmin) / (area.height - 1);

		vP = Pmin + DeltaP * vX;
		vQ = Qmin + DeltaQ * vY;

		vC = renderPointFloat(vP, vQ) * 4;

		if (vC == maxIteration * 4) {
			result = 0;
		} else {
			result = vC;
		}

		return result;
	}

	private int renderPointDouble(double P, double Q) {
		if (P * P + Q * Q > 4.0) {
			return 0;
		}
		if ((P + 0.25) * (P + 0.25) + Q * Q < 0.24) {
			return 0;
		}

		double x, y, x1, y1, R;
		int n, result;
		n = 0;
		x1 = P;
		y1 = Q;

		do {
			x = x1;
			y = y1;
			x1 = x * x - y * y + P;
			y1 = 2 * x * y + Q;
			R = x1 * x1 + y1 * y1;
			n++;
		} while (n < maxIteration && R < 4.0);

		if (n >= maxIteration) {
			result = 0;
		} else {
			result = n;
		}
		return result;
	}
	
	private int renderPointFloat(float P, float Q) {
		if (P * P + Q * Q > 4.0) {
			return 0;
		}
		if ((P + 0.25) * (P + 0.25) + Q * Q < 0.24) {
			return 0;
		}

		float x, y, x1, y1, R;
		int n, result;
		n = 0;
		x1 = P;
		y1 = Q;

		do {
			x = x1;
			y = y1;
			x1 = x * x - y * y + P;
			y1 = 2 * x * y + Q;
			R = x1 * x1 + y1 * y1;
			n++;
		} while (n < maxIteration && R < 4.0);

		if (n >= maxIteration) {
			result = 0;
		} else {
			result = n;
		}
		return result;
	}

	private boolean renderLine(RenderArea area, int lastColor, int x1, int y1,
			int x2, int y2, int offsetX, int offsetY) {
		boolean result = true;
		int cX = x1;
		int cY = y1;
		int calcPixels = 1;
		int c = 0;
		int x = x2 - x1;
		int y = y2 - y1;

		while (cX <= x2 && cY <= y2) {
			if(area.precision == 1){
				c = getColorFPUFloat(area, x, y);
			}
			if(area.precision == 2){
				c = getColorFPUDouble(area, x, y);
			}
			
			result = result && (c == lastColor);
			if (!result) {
				break;
			}

			if (offsetX == 1) {
				cX += calcPixels * offsetX;
				x += 1;
			}
			if (offsetY == 1) {
				cY += calcPixels * offsetY;
				y += 1;
			}
		}

		/*
		 * if (result) { Log.d("renderLine.true", area.toString() + ";c=" + c +
		 * ";last=" + lastColor); } else { Log.d("renderLine.false",
		 * area.toString() + ";c=" + c + ";last=" + lastColor); }
		 */

		return result;
	}
	
	private void copyResultBuffer(RenderArea area){
		int w = area.recti[2] - area.recti[0];
		int h = area.recti[3] - area.recti[1];
		for (int j = 0; j < h; j++) {
			for (int i = 0; i < w; i++) {
				if(calcBuffer[j * w + i] != -1){
					resultBuffer[j * w + i] = calcBuffer[j * w + i];
				}
			}
		}
	}
}
