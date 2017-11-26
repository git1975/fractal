package ru.android.fractal;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;

public class RenderArea /*implements Runnable*/ {
    /**
     * ????????<br/>
     * 1 - rectf<br/>
     * 2 - rectd<br/>
     */
    private Handler handler;
    private Handler.Callback callback = null;

    public int precision = 1;
    public long startTime = 0;

    public double[] rectd = new double[4];
    public float[] rectf = new float[4];
    public int[] recti = new int[4];
    public double stepx;
    public double stepy;
    public int[] data = null;
    public int width;
    public int height;
    public boolean showPartial = false;
    public int calcDepth;

    Bitmap buffBitmap;

    public boolean working = false;

    public RenderArea(Handler handler, Handler.Callback callback) {
        this.handler = handler;
        this.callback = callback;
    }

    @Override
    public String toString() {
        String result = "p=" + precision;
        result = result + ";rectd=[" + rectd[0] + "," + rectd[1] + "," + rectd[2] + "," + rectd[3] + "]";
        result = result + ";recti=[" + recti[0] + "," + recti[1] + "," + recti[2] + "," + recti[3] + "]";

        return result;
    }

    //@Override
    public void run() {
        working = true;
        try {
            new Renderer(handler).doRender(this);
            //buffBitmap = Bitmap.createBitmap(this.data,
            //        this.recti[2] - this.recti[0], this.recti[3] - this.recti[1], Bitmap.Config.ARGB_8888);
            //new RenderThread(handler, this).start();
        } finally {
            working = false;
            if(callback != null){
                Message msg = new Message();
                msg.what = Messages.MSG_AREA_COMPLETE;
                msg.obj = this;
                callback.handleMessage(msg);
            }
        }
    }

    public void doRender() {
        new Renderer(null).doRender(this);
    }
}
