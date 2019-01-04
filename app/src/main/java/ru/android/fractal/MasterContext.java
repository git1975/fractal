package ru.android.fractal;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MasterContext extends SurfaceView implements
        SurfaceHolder.Callback, Handler.Callback, ScaleHandler {
    private RenderArea globalArea;
    //private ThreadPool threadPool = null;
    private Handler handler;
    private ConcurrentThreadPool renderPool;
    private java.util.concurrent.ConcurrentLinkedQueue<RenderArea> queue;
    private boolean inProgress = false;
    private long globalCounter = 0;
    private long startTime = 0;
    private float mouseDownX;
    private float mouseDownY;
    private MainOptions mainOptions = new MainOptions();
    private static final String logHeader = "MasterContext";
    Bitmap masterBitmap = null;
    private ScaleGestureDetector scaleDetector;
    private float scaleFactor = 1.f;

    public MasterContext(Context context, AttributeSet attrs) {
        super(context, attrs);

        Log.d("MasterContext", "MasterContext");

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        holder.setFormat(PixelFormat.RGBA_8888);
        // holder.setType(android.view.SurfaceHolder.SURFACE_TYPE_HARDWARE);
        handler = new Handler(this);
        //threadPool = new ThreadPool(handler);
        //queue = new LinkedBlockingQueue();
        queue = new ConcurrentLinkedQueue<RenderArea>();
        renderPool = new ConcurrentThreadPool(mainOptions.threadCount, this);
        globalArea = new RenderArea(handler, renderPool);

        globalArea.rectd = new double[]{-2.2, -1.9, 0.7, -0.6};

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener(this));
    }

    public void loadOptions() {
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(getContext());
        mainOptions.showWhile = sharedPrefs.getBoolean("optShowWhile", true);
        mainOptions.showPartial = sharedPrefs
                .getBoolean("optShowPartial", true);
        mainOptions.threadCount = Integer.parseInt(sharedPrefs.getString(
                "optThreadCount", "4"));
        mainOptions.quadCount = Integer.parseInt(sharedPrefs.getString(
                "optQuadCount", "3"));
        mainOptions.calcDepth = Integer.parseInt(sharedPrefs.getString(
                "optCalcDepth", "50"));
        mainOptions.startType = Integer.parseInt(sharedPrefs.getString(
                "optStartType", "2"));
        mainOptions.precision = Integer.parseInt(sharedPrefs.getString(
                "optPrecision", "1"));

        Log.d("loadOptions", mainOptions.toString());
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        Log.d("MasterContext", "surfaceChanged");
        /*
		 * stop(); threadPool.halt(); loadOptions();
		 * threadPool.setSize(mainOptions.threadCount);
		 * threadPool.setCalcDepth(mainOptions.calcDepth);
		 */
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(logHeader, "surfaceCreated");
        loadOptions();
        renderPool.setSize(mainOptions.threadCount);

        if(masterBitmap == null) {
            masterBitmap = Bitmap.createBitmap(this.getWidth(), this.getHeight(), Bitmap.Config.ARGB_8888);
        }
        panCanvas(0, 0);
        //renderPool = new ConcurrentThreadPool(mainOptions.threadCount, queue);
        //renderPool = new ThreadPoolExecutor(mainOptions.threadCount, mainOptions.threadCount, 1, TimeUnit.SECONDS, queue);
        //, new RenderThreadFactory());
        //threadPool.setSize(mainOptions.threadCount);
        //threadPool.setCalcDepth(mainOptions.calcDepth);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(logHeader, "surfaceDestroyed");
        stop();
        //threadPool.halt();
    }

    public void start() {
		/*
		 * x1=-1,62060474657233 y1=0,00683621582605354 x2=-1,62060473755607
		 * y2=0,00683622332515456 stepx=1,36610039253027E-10
		 * stepy=1,26413941096168E-10
		 */
        setInProgress(true);
        startTime = (new Date()).getTime();
        globalArea.startTime = startTime;
        globalCounter = 0;
        getProgressBar().setProgress(0);
        //getButtonStart().setText(R.string.btnStop);
        getButtonStart().setBackgroundResource(R.drawable.s6);

        // clearContext();
        globalArea.width = this.getWidth();
        globalArea.height = this.getHeight();
        globalArea.recti = new int[]{0, 0, this.getWidth(), this.getHeight()};
        // globalArea.recti = new int[] { 0, 0, 400, 800 };

        globalArea.rectd[3] = (globalArea.rectd[3] - globalArea.rectd[0])
                * this.getHeight() / this.getWidth() + globalArea.rectd[1];

        int divider = mainOptions.quadCount;
        int maxX = (int) Math.round(Math.pow(2, divider));
        int maxY = Math.round(maxX * this.getHeight() / this.getWidth()) + 1;// Math.round(Math.pow(2,
        // divider));
        int StepX = Math.round((globalArea.recti[2] - globalArea.recti[0] + 1)
                / maxX);
        int StepY = StepX;// Math.round((globalArea.recti[3] -
        // globalArea.recti[1] + 1)
        // / maxY);

        Log.d("start", "globalArea=" + globalArea);
        Log.d("start", "maxX=" + maxX + ";maxY=" + maxY + ";StepX=" + StepX
                + ";StepY=" + StepY);

		/*
		 * RenderArea area = new RenderArea(); area.precision = 1; area.recti =
		 * new int[] { 0, 0, 100, 100 }; area.rectd = globalArea.rectd;
		 * Queue.getInstance().add(area);
		 */

        double stepDx = Math.abs(globalArea.rectd[2] - globalArea.rectd[0])
                / (globalArea.recti[2] - globalArea.recti[0]);
        double stepDy = stepDx;// Math.abs(globalArea.rectd[3] -
        // globalArea.rectd[1])/ (globalArea.recti[3] -
        // globalArea.recti[1]);

        ArrayList<RenderArea> tmpArray = new ArrayList<RenderArea>();
        int i;
        int j;
        for (j = 0; j < maxY; j++) {
            for (i = 0; i < maxX; i++) {
                RenderArea a = new RenderArea(handler, renderPool);
                a.startTime = globalArea.startTime;
                a.showPartial = mainOptions.showPartial;
                a.precision = mainOptions.precision;
                a.calcDepth = mainOptions.calcDepth;

                a.recti[0] = globalArea.recti[0] + StepX * i;
                a.recti[1] = globalArea.recti[1] + StepY * j;
				/*
				 * if(a.recti[0] > 0){ a.recti[0] += 1; } if(a.recti[1] > 0){
				 * a.recti[1] += 1; }
				 */
                a.recti[2] = a.recti[0] + StepX - 1;
                a.recti[3] = a.recti[1] + StepY - 1;
				/*
				 * if(a.recti[0] > 0){ a.recti[2] -= 1; } if(a.recti[1] > 0){
				 * a.recti[3] -= 1; }
				 */

                if (a.recti[0] > 0)
                    a.recti[0] = a.recti[0] - 1;
                if (a.recti[1] > 0)
                    a.recti[1] = a.recti[1] - 1;

                if (a.recti[0] > globalArea.recti[2])
                    a.recti[0] = globalArea.recti[2] - 1;
                if (a.recti[1] > globalArea.recti[3])
                    a.recti[1] = globalArea.recti[3] - 1;

                if (a.recti[2] > globalArea.recti[2])
                    a.recti[2] = globalArea.recti[2] - 1;
                if (a.recti[3] > globalArea.recti[3])
                    a.recti[3] = globalArea.recti[3] - 1;

                a.width = a.recti[2] - a.recti[0];
                a.height = a.recti[3] - a.recti[1];

                a.rectd[0] = globalArea.rectd[0] + a.recti[0] * stepDx;
                a.rectd[1] = globalArea.rectd[1] + a.recti[1] * stepDy;
                a.rectd[2] = globalArea.rectd[0] + a.recti[2] * stepDx;
                a.rectd[3] = globalArea.rectd[1] + a.recti[3] * stepDy;

                if (mainOptions.precision == 1) {
                    a.rectf[0] = (float) a.rectd[0];
                    a.rectf[1] = (float) a.rectd[1];
                    a.rectf[2] = (float) a.rectd[2];
                    a.rectf[3] = (float) a.rectd[3];
                }

                tmpArray.add(a);

            }
        }
        //Queue.getInstance().clear();
        queue.clear();
        // Draw bottom to top
        if (mainOptions.startType == 1) {
            for (RenderArea item : tmpArray) {
                //Queue.getInstance().add(item);
                renderPool.submit(item);
            }
        }
        // Draw from center
        if (mainOptions.startType == 2) {
            int cx;
            cx = tmpArray.size() / 2;
            for (i = 0; i < cx; i++) {
                //Queue.getInstance().add(tmpArray.get(cx + i));
                //Queue.getInstance().add(tmpArray.get(cx - i));
                renderPool.submit(tmpArray.get(cx + i));
                renderPool.submit(tmpArray.get(cx - i));
            }
            if (tmpArray.size() > 0) {
                //Queue.getInstance().add(tmpArray.get(0));
                renderPool.submit(tmpArray.get(0));
            }
        }
        // Draw random
        if (mainOptions.startType == 3) {
            while (tmpArray.size() > 0) {
                int pos = (int) Math.round(tmpArray.size() * Math.random()) - 1;
                if (pos < 0) {
                    pos = 0;
                }
                //Queue.getInstance().add(tmpArray.get(pos));
                renderPool.submit(tmpArray.get(pos));
                tmpArray.remove(pos);
            }
        }
        //threadPool.start();
    }

    public void stop() {
        if (isInProgress()) {
            Log.d(logHeader, "stop");
            setInProgress(false);
            renderPool.stop();
            //queue.clear();
            //Queue.getInstance().clear();
            //threadPool.stop();
            //getButtonStart().setText(R.string.btnRefresh);
            getButtonStart().setBackgroundResource(R.drawable.r5);
            long tm = (new Date()).getTime() - startTime;
            SimpleDateFormat sdf = new SimpleDateFormat("mm:ss.SSS");
            setStatus(sdf.format(new Date(tm)));
        }
    }

    public void zoom(boolean zoomIn, int factor) {
        Log.d(logHeader, "zoomin=" + zoomIn);
        stop();

        RenderArea zoomArea = new RenderArea(handler, renderPool);
        long m, zf;
        double zx, zy, fx, fy, factorX = 0, factorY = 0;

        if (zoomIn) {
            m = 1;
        } else {
            m = -1;
        }

        zf = factor * Math.round(this.getWidth() / 100.);

        if (factorX == 0) {
            zoomArea.stepx = (globalArea.rectd[2] - globalArea.rectd[0])
                    / this.getWidth();
            zoomArea.stepy = zoomArea.stepx;// (globalArea.rectd[3] -
            // globalArea.rectd[1])/
            // this.getHeight();
        }
        fx = factorX + m * zf;
        fy = fx;// fx * this.getHeight() / this.getWidth();

        if (fx > this.getWidth() / 2) {
            return;
        }
        if (fy > this.getHeight() / 2) {
            return;
        }
        if (fx < -this.getWidth() - 10) {
            return;
        }
        if (fy < -this.getHeight() - 10) {
            return;
        }

        factorX = fx;
        factorY = fy;
        zx = zoomArea.stepx * factorX;
        zy = zoomArea.stepy * factorY;

        zoomArea.rectd[0] = globalArea.rectd[0] + zx;
        zoomArea.rectd[2] = globalArea.rectd[2] - zx;
        zoomArea.rectd[1] = globalArea.rectd[1] + zy;
        zoomArea.rectd[3] = globalArea.rectd[3] - zy;

        globalArea.rectd[0] = zoomArea.rectd[0];
        globalArea.rectd[1] = zoomArea.rectd[1];
        globalArea.rectd[2] = zoomArea.rectd[2];
        globalArea.rectd[3] = zoomArea.rectd[3];

        start();
    }

    public void pan(int x, int y) {
        stop();

        double stepDx = Math.abs(globalArea.rectd[2] - globalArea.rectd[0])
                / (globalArea.recti[2] - globalArea.recti[0]);

        globalArea.rectd[0] = globalArea.rectd[0] - stepDx * x;
        globalArea.rectd[1] = globalArea.rectd[1] - stepDx * y;
        globalArea.rectd[2] = globalArea.rectd[2] - stepDx * x;
        globalArea.rectd[3] = globalArea.rectd[3] - stepDx * y;

        start();
    }

    private void panCanvas(int x, int y) {
        //Bitmap bitmap = this.getDrawingCache();

        SurfaceHolder holder = getHolder();
        Canvas c = null;
        try {
            Rect r = new Rect(0, 0, this.getWidth()-1, this.getHeight()-1);
            c = holder.lockCanvas(r);
            synchronized (holder) {
                if (c != null) {
                    Paint p = new Paint();
                    p.setColor(Color.rgb(0, 0, 0));
                    p.setStyle(Style.FILL);
                    c.drawRect(r, p);

                    c.drawBitmap(masterBitmap, x, y, null);
                }
            }
        } finally {
            if (c != null) {
                holder.unlockCanvasAndPost(c);
            }
        }
    }

    private void scaleCanvas(float value) {
        //Bitmap bitmap = this.getDrawingCache();

        SurfaceHolder holder = getHolder();
        Canvas c = null;
        try {
            Matrix matrix = new Matrix();
            matrix.setScale(value, value);
            Rect r = new Rect(0, 0, this.getWidth()-1, this.getHeight()-1);
            c = holder.lockCanvas(r);
            synchronized (holder) {
                if (c != null) {
//                    Paint p = new Paint();
//                    p.setColor(Color.rgb(127, 127, 127));
//                    p.setStyle(Style.FILL);
//                    c.drawRect(r, p);

                    c.drawBitmap(masterBitmap, matrix, null);
                }
            }
        } finally {
            if (c != null) {
                holder.unlockCanvasAndPost(c);
            }
        }
    }

    @Override
    public void doTouchScale(float value){
        Log.d("doTouchScale", "value=" + value);
        scaleCanvas(value);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        if(isInProgress()){
            return true;
        }

        MasterContext mc = (MasterContext) this.findViewById(R.id.surfaceView1);
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            Log.d("onTouchEvent", "ACTION_DOWN");
            mouseDownX = event.getX();
            mouseDownY = event.getY();
        }
        if (action == MotionEvent.ACTION_UP) {
            Log.d("onTouchEvent", "ACTION_UP");
            if (isInProgress()) {
                return true;
            }
            float x = event.getX() - mouseDownX;
            float y = event.getY() - mouseDownY;
            if (Math.abs(x) < 10 && Math.abs(y) < 10) {
                return true;
            }
            mc.pan(Math.round(x), Math.round(y));
        }
        if (action == MotionEvent.ACTION_MOVE) {
            Log.d("onTouchEvent", "ACTION_MOVE");
            float x = event.getX() - mouseDownX;
            float y = event.getY() - mouseDownY;
            panCanvas(Math.round(x), Math.round(y));
        }

        //scaleDetector.onTouchEvent(event);

        return true;
    }

    public Button getButtonStart() {
        View v = (View) this.getParent();
        return (Button) v.findViewById(R.id.button1);
    }

    public ProgressBar getProgressBar() {
        View v = (View) this.getParent();
        return (ProgressBar) v.findViewById(R.id.progressBar1);
    }

    public TextView getStatusText() {
        View v = (View) this.getParent();
        return (TextView) v.findViewById(R.id.textView1);
    }

    public void setStatus(String value) {
        getStatusText().setText(value);
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == Messages.MSG_POINT_COMPLETE) {

        }
        if (msg.what == Messages.MSG_AREA_COMPLETE) {
            RenderArea area = (RenderArea) msg.obj;
            if (globalArea.startTime != area.startTime) {
                return true;
            }

            //globalCounter += (area.recti[2] - area.recti[0])
            //		* (area.recti[3] - area.recti[1]);
            //int total = (globalArea.recti[2] - globalArea.recti[0])
            //		* (globalArea.recti[3] - globalArea.recti[1]);

            globalCounter += (area.width + 1) * (area.height + 1);
            long total = globalArea.width * globalArea.height;

            getProgressBar().setProgress(
                    Math.round(globalCounter * 100 / total));
            if (isInProgress()) {
                setStatus("working " + getProgressBar().getProgress() + "%");
            }
            // if (Queue.getInstance().isEmpty()) {
            if (getProgressBar().getProgress() >= 100) {
                stop();
            }

            // Log.d("handleMessage.MSG_AREA_COMPLETE", area.toString());

            SurfaceHolder holder = getHolder();
            Canvas c = null;
            try {
                c = holder.lockCanvas(new Rect(area.recti[0], area.recti[1],
                        area.recti[2], area.recti[3]));
                Bitmap buffBitmap = Bitmap.createBitmap(area.data,
                        area.recti[2] - area.recti[0], area.recti[3]
                                - area.recti[1], Bitmap.Config.ARGB_8888);
//                synchronized (holder) {
//                    if (c != null && buffBitmap != null) {
//                        c.drawBitmap(buffBitmap, area.recti[0], area.recti[1],
//                                null);
//                    }
//                }

                Canvas canvas = new Canvas(this.masterBitmap);
                canvas.drawBitmap(buffBitmap, area.recti[0], area.recti[1],
                        null);
                synchronized (holder) {
                    c.drawBitmap(masterBitmap, 0, 0, null);
                }
            } finally {
                if (c != null) {
                    holder.unlockCanvasAndPost(c);
                }
            }
        }

        if (msg.what == Messages.MSG_AREA_COMPLETE_PART) {
            RenderArea area = (RenderArea) msg.obj;
            SurfaceHolder holder = getHolder();
            Canvas c = null;
            try {
                c = holder.lockCanvas(new Rect(area.recti[0], area.recti[1],
                        area.recti[2], area.recti[3]));
                Bitmap buffBitmap = Bitmap.createBitmap(area.data,
                        area.recti[2] - area.recti[0], area.recti[3]
                                - area.recti[1], Bitmap.Config.ARGB_8888);
                synchronized (holder) {
                    if (c != null && buffBitmap != null) {
                        c.drawBitmap(buffBitmap, area.recti[0], area.recti[1],
                                null);
                    }
                }
            } finally {
                if (c != null) {
                    holder.unlockCanvasAndPost(c);
                }
            }
        }

        if (msg.what == Messages.MSG_AREA_BEGIN || msg.what == Messages.MSG_AREA_BEGIN_RECT) {
            if (!mainOptions.showWhile) {
                return true;
            }
            Rect rect = null;
            if (msg.what == Messages.MSG_AREA_BEGIN) {
                RenderArea area = (RenderArea) msg.obj;
                rect = new Rect(area.recti[0], area.recti[1],
                        area.recti[2], area.recti[3]);
            }
            if (msg.what == Messages.MSG_AREA_BEGIN_RECT) {
                rect = (Rect) msg.obj;
            }
            Paint p = new Paint();
            //p.setColor(Color.argb(200, 200, 200, 200));
            //p.setStyle(Style.STROKE);
            p.setColor(Color.rgb(127, 127, 127));
            p.setStyle(Style.FILL);

            SurfaceHolder holder = getHolder();
            Canvas c = null;
            try {
                c = holder.lockCanvas(rect);
                synchronized (holder) {
                    if (c != null) {
                        c.drawRect(rect, p);
						/*
						 * c.drawLine(area.recti[0], area.recti[1],
						 * area.recti[2], area.recti[3], p);
						 * c.drawLine(area.recti[2], area.recti[1],
						 * area.recti[0], area.recti[3], p);
						 */
						/*p.setStyle(Style.FILL);
						int cx = 15;
						int cnt = 0, cnt2 = 0;
						int i = area.recti[0], j = area.recti[1];
						while (j <= area.recti[3]) {
							while (i <= area.recti[2] - 1) {
								if ((cnt % 2) == 0) {
									p.setColor(Color.rgb(150, 150, 150));
								} else {
									p.setColor(Color.rgb(200, 200, 200));
								}
								c.drawRect(i, j, i + cx, j + cx, p);
								i = i + cx;
								cnt++;
							}
							i = area.recti[0];
							cnt2++;
							cnt = cnt2;
							j = j + cx;
						}*/
                    }
                }
            } finally {
                if (c != null) {
                    holder.unlockCanvasAndPost(c);
                }
            }
        }

        return true;
    }

    public void clearContext() {
        SurfaceHolder holder = getHolder();
        Canvas c = null;
        try {
            c = holder.lockCanvas();
            synchronized (holder) {
                c.drawColor(Color.argb(250, 171, 171, 171));
            }
        } finally {
            if (c != null) {
                holder.unlockCanvasAndPost(c);
            }
        }
    }

    public boolean isInProgress() {
        return inProgress;
    }

    public void setInProgress(boolean inProgress) {
        this.inProgress = inProgress;
        if (isInProgress()) {
            getProgressBar().setVisibility(View.VISIBLE);
        } else {
            getProgressBar().setVisibility(View.INVISIBLE);
        }
    }

    private class ScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private ScaleHandler scaleHandler = null;
        public ScaleListener(ScaleHandler scaleHandler){
            this.scaleHandler = scaleHandler;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f));

            scaleHandler.doTouchScale(scaleFactor);
            return true;
        }
    }
}
