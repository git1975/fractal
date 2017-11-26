package ru.android.fractal;

import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

public class RenderTask extends AsyncTask<RenderArea, RenderArea, RenderArea> {
    Handler.Callback callback;
    Rect rect;

    public RenderTask(Handler.Callback callback, Rect rect){
        super();
        this.callback = callback;
        this.rect = rect;
    }

    //MainThread
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if(callback != null){
            Message msg = new Message();
            msg.what = Messages.MSG_AREA_BEGIN_RECT;
            msg.obj = rect;
            callback.handleMessage(msg);
        }
    }

    //WorkerThread
    @Override
    protected RenderArea doInBackground(RenderArea... area) {
        if(area.length > 0) {
            area[0].doRender();
            return area[0];
        }
        return null;
    }

    //MainThread
    @Override
    protected void onProgressUpdate(RenderArea... values) {
        super.onProgressUpdate(values);
    }

    //MainThread
    @Override
    protected void onPostExecute(RenderArea result) {
        super.onPostExecute(result);
        if(callback != null){
            Message msg = new Message();
            msg.what = Messages.MSG_AREA_COMPLETE;
            msg.obj = result;
            callback.handleMessage(msg);
        }
    }
}
