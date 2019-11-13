package com.padyun.opencvapi.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.padyun.YpModule2;
import com.padyun.network.ClientNio;
import com.padyun.opencvapi.LtLog;
import com.padyun.opencvapi.ScreenInfo;
import com.padyun.opencvapi.module.ScreenInfoModule;
import com.padyun.opencvapi.module.TypeModule;

import org.opencv.core.Mat;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;

import static org.opencv.core.CvType.CV_8U;

/**
 * Created by litao on 2018/7/19.
 */
public class YpScreencap2 extends YpScreencap {
    public static final short TYPE_REQUEST_IMAGE = 0x0010;
    public static final short TYPE_IAMGE = 0x0011;

    public static final byte ATTR_IMG = 0X01;
    public static final byte ATTR_WIDTH = 0X02;
    public static final byte ATTR_HEIGHT = 0X03;
    public static final byte ATTR_TIMESTAMP = 0x04;
    private static final int SCREENCAP_PORT = 11413;
    public static final String KEY_SCREENSERVER = "screen_server";
    private ClientNio mNetWork;
    private Context mContext;

    public YpScreencap2(final Context context, Intent intentResult) {
        super(context, intentResult);
        mContext = context;
        mNetWork = new ClientNio(5000);
        connectService(context);
    }

    private void connectService(final Context context) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                for (int i = 0; i < 5; ++i) {
                    success = mNetWork.connect(System.getProperty(KEY_SCREENSERVER, "127.0.0.1"), SCREENCAP_PORT);
                    LtLog.i("connect ypservice :" + success);
                    if (success) {
                        break;
                    }
                    mNetWork.disconnect();
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.padyun.ypservice", "com.padyun.ypservice.YpService"));
                    mContext.startService(intent);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private ScreenInfo screencap(ScreenInfo screenInfo) {
        synchronized (this) {
            TypeModule typeModule = new TypeModule(TYPE_REQUEST_IMAGE);
            try {
                int ret = mNetWork.send(typeModule.toDataWithLen());
                if (ret == -1) {
                    LtLog.i("screencap send error reconnect....");
                    mNetWork.disconnect();
                    connectService(mContext);
                    return null;
                }
                ByteBuffer byteBuffer = mNetWork.readPackage();
                if (byteBuffer != null) {
                    byte[] data = byteBuffer.array();
                    int len = data.length;
                    int offset = 0;
                    short type = Utils.bytesToShort(data, offset);
                    offset += YpModule2.SIZE_OF_TYPE;
                    len -= com.padyun.YpModule2.SIZE_OF_TYPE;

                    switch (type) {
                        case TYPE_IAMGE:
                            ScreenInfoModule module = new ScreenInfoModule(data, offset, len);
                            screenInfo.width = module.width;
                            screenInfo.height = module.height;
                            screenInfo.raw = module.img.array();
                            screenInfo.timestamp = module.timestamp;
                            break;
                        default:
                            LtLog.i("screencap package type error :" + type);
                            break;

                    }
                } else {
                    LtLog.i("screencap read package error reconnect");
                    mNetWork.disconnect();
                    connectService(mContext);
                }
            } catch (TimeoutException e) {
                LtLog.i("screencap read package timeout reconnect");
                e.printStackTrace();
                mNetWork.disconnect();
                connectService(mContext);
            } catch (YpModule2.YpModuleException e) {
                e.printStackTrace();
                LtLog.i("screencap read package exception:" + e.getMessage());
                mNetWork.disconnect();
                connectService(mContext);
            }
        }
        return screenInfo;
    }

    public ScreenInfo captureRawScreen(final ScreenInfo screenInfo) {
        if (Utils.isInMainThread()) {
            final Object obj = new Object();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    screencap(screenInfo);
                    synchronized (obj) {
                        obj.notify();
                    }
                }
            }).start();
            synchronized (obj) {
                try {
                    obj.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return screenInfo;
        } else {
            return screencap(screenInfo);
        }
    }


    private Mat  matcap() {
        Mat mat2=null;
        synchronized (this) {
            TypeModule typeModule = new TypeModule(TYPE_REQUEST_IMAGE);
            try {
                int ret = mNetWork.send(typeModule.toDataWithLen());
                if (ret == -1) {
                    LtLog.i("screencap send error reconnect....");
                    mNetWork.disconnect();
                    connectService(mContext);
                    return null;
                }
                ByteBuffer byteBuffer = mNetWork.readPackage();
                if (byteBuffer != null) {
                    WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
                    DisplayMetrics metrics = new DisplayMetrics();
                    windowManager.getDefaultDisplay().getRealMetrics(metrics);
                    byte[] data = byteBuffer.array();
                    mat2 = new Mat(metrics.widthPixels, metrics.heightPixels, CV_8U);
                    mat2.put(0, 0,data);
                } else {
                    LtLog.i("screencap read package error reconnect");
                    mNetWork.disconnect();
                    connectService(mContext);
                }
            } catch (TimeoutException e) {
                LtLog.i("screencap read package timeout reconnect");
                e.printStackTrace();
                mNetWork.disconnect();
                connectService(mContext);
            }
        }
        return mat2;
    }
    public Mat captureMat() {
        if (Utils.isInMainThread()) {
            final Object obj = new Object();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    matcap();
                    synchronized (obj) {
                        obj.notify();
                    }
                }
            }).start();
            synchronized (obj) {
                try {
                    obj.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return   matcap();
        } else {
            return  matcap();
        }
    }


    private byte[]  bitmapcap() {
        byte[] data=null;
        synchronized (this) {
            TypeModule typeModule = new TypeModule(TYPE_REQUEST_IMAGE);
            try {
                int ret = mNetWork.send(typeModule.toDataWithLen());
                if (ret == -1) {
                    LtLog.i("screencap send error reconnect....");
                    mNetWork.disconnect();
                    connectService(mContext);
                    return null;
                }
                ByteBuffer byteBuffer = mNetWork.readPackage();
                if (byteBuffer != null) {
                    data = byteBuffer.array();
                } else {
                    LtLog.i("screencap read package error reconnect");
                    mNetWork.disconnect();
                    connectService(mContext);
                }
            } catch (TimeoutException e) {
                LtLog.i("screencap read package timeout reconnect");
                e.printStackTrace();
                mNetWork.disconnect();
                connectService(mContext);
            }
        }
        return data;
    }


    public byte[] captureBitmap() {
        if (Utils.isInMainThread()) {
            final Object obj = new Object();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    bitmapcap();
                    synchronized (obj) {
                        obj.notify();
                    }
                }
            }).start();
            synchronized (obj) {
                try {
                    obj.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return   bitmapcap();
        } else {
            return  bitmapcap();
        }
    }
}
