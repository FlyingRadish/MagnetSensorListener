package com.example.sensorapp.service;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import com.example.sensorapp.util.logger.Log;

import java.util.LinkedList;
import java.util.List;

/**
 * Des:
 * Created by houxg on 2015/2/26.
 */
public class MagnetListener implements SensorEventListener {
    final String TAG = "MagnetListener";
    private final static float MAGNET_TRIGGER_VALUE = 800;
    private final static int COVER_OPEN = 1;
    private final static int COVER_CLOSE = 2;
    private final static int NOTHING = 0;

    int[] lastVal = null;
    SensorService.OnCoverStateChanged listener;
    List<Float> bufferX, bufferY, bufferZ;
    final static int BUFFER_SIZE = 5;

    public MagnetListener(SensorService.OnCoverStateChanged listener) {
        this.listener = listener;
        bufferX = new LinkedList<>();
        bufferY = new LinkedList<>();
        bufferZ = new LinkedList<>();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int[] dVal = new int[]{
                fifoFilter(bufferX, event.values[0]),
                fifoFilter(bufferY, event.values[1]),
                fifoFilter(bufferZ, event.values[2])};

        if (lastVal == null) {
            lastVal = new int[3];
            updateVal(dVal);
        }
//            postVal(event.values);
        int result = isTrigger(dVal);
        if (listener != null) {
            if (result == COVER_OPEN) {
                listener.onChanged(true, Sensor.TYPE_MAGNETIC_FIELD);
                Log.i(TAG, "magnet cover open");
            } else if (result == COVER_CLOSE) {
                Log.i(TAG, "magnet cover close");
                listener.onChanged(false, Sensor.TYPE_MAGNETIC_FIELD);
            }
        }
        updateVal(dVal);
    }


    private int fifoFilter(List<Float> buffer, float val) {
        buffer.add(0, val);
        //返回标量的变化值，得到二次波形
        //通过FIFO滤波，将波形近似成方波
        float dx = Math.abs(buffer.get(buffer.size() - 1)) - Math.abs(buffer.get(0));

        if (buffer.size() >= BUFFER_SIZE) {
            buffer.remove(buffer.size() - 1);
        }
        return (int) dx;
    }

    private void updateVal(int[] val) {
        for (int i = 0; i < val.length; i++) {
            lastVal[i] = val[i];
        }
    }

    private int isTrigger(int[] newVal) {
        for (int i = 0; i < lastVal.length; i++) {
            int dx = Math.abs(newVal[i]) - Math.abs(lastVal[i]);    //取上升段
            if (dx > MAGNET_TRIGGER_VALUE) {
                if (newVal[i] - lastVal[i] > 0) {   //根据正负即可判断开关闭合
                    return COVER_OPEN;
                } else {
                    return COVER_CLOSE;
                }
            }
        }
        return NOTHING;
    }

    void postVal(float[] vals) {
        String value = "";
        for (float val : vals) {
            value += val + ", ";
        }
        Log.i("houxg", "magnet val:" + value);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
