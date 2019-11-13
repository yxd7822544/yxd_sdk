package com.padyun.opencvapi.utils;

import android.util.DisplayMetrics;
import android.view.MotionEvent;

import java.nio.ByteBuffer;

/**
 * Created by litao on 2016/7/20.
 */
public class TouchProtocol {


    public static final short VK_PIXEL    = 0x0410 ;
    public static final short VK_KEY      = 0x0406 ;
    public static final short VK_TOUCH    = 0x0407 ;


    public static class MutilTouchEvent {
        public int action ;
        public int count ;
        public MotionEvent.PointerCoords[] coords ;
        public int[] ids ;
        public void fromData(byte[] data, int start, int count){
            ByteBuffer buffer = ByteBuffer.wrap(data, start, count) ;
            action = buffer.getInt() ;
            count = buffer.getInt() ;
            coords = new MotionEvent.PointerCoords[count] ;
            ids = new int[count] ;
            for(int i = 0 ;i < count ; ++i){
                MotionEvent.PointerCoords c = new MotionEvent.PointerCoords() ;
                coords[i] = c ;
                ids[i] = buffer.getInt();
                coords[i].x = buffer.getInt() ;
                coords[i].y = buffer.getInt() ;
            }
        }


        public byte[] toData(){
            ByteBuffer buffer = ByteBuffer.allocate(1024*10) ;
            buffer.putShort(VK_TOUCH) ;
            buffer.putInt(action) ;
            buffer.putInt(count) ;
            for(int i = 0 ;i < count ; ++i){
                buffer.putInt(ids[i]) ;
                buffer.putInt((int) coords[i].x);
                buffer.putInt((int) coords[i].y) ;
            }
            byte b[] = new byte[buffer.position()] ;
            buffer.flip() ;
            buffer.get(b);
            return b ;
        }

    }
    public static class KeyEvent{
        public int action ;
        public int key ;
        public void fromData(byte[] data, int start, int count){
            ByteBuffer buffer = ByteBuffer.wrap(data, start, count) ;
            action = buffer.getInt() ;
            key = buffer.getInt() ;
        }
        public byte[] toData(){
            ByteBuffer buffer = ByteBuffer.allocate(1024*10) ;
            buffer.putShort(VK_KEY) ;
            buffer.putInt(action) ;
            buffer.putInt(key) ;

            byte b[] = new byte[buffer.position()] ;
            buffer.flip() ;
            buffer.get(b);
            return b ;
        }
    }
    public static class ClientPixel{
        public DisplayMetrics dm = new DisplayMetrics() ;

        public byte[] toData(){
            ByteBuffer buffer = ByteBuffer.allocate(1024*10) ;
            buffer.putShort(VK_PIXEL) ;
            buffer.putInt(dm.widthPixels) ;
            buffer.putInt(dm.heightPixels) ;

            byte b[] = new byte[buffer.position()] ;
            buffer.flip() ;
            buffer.get(b);
            return b ;
        }
        public void fromData(byte[] data, int start, int count){
            ByteBuffer buffer = ByteBuffer.wrap(data, start, count) ;
            dm.widthPixels = buffer.getInt() ;
            dm.heightPixels = buffer.getInt() ;
        }
    }



}
