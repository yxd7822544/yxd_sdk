package com.padyun;


import com.padyun.opencvapi.LtLog;
import com.padyun.utils.Utils;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by litao on 2016/8/15.
 */
public abstract class YpModule {

    protected Map<Byte,Field> fields  = new HashMap<>();
    protected short type ;
    protected static final int FIELD_TYPE_UNKNOWN      = 0 ;
    protected static final int FIELD_TYPE_STRING       = 1 ;
    protected static final int FIELD_TYPE_INT          = 2 ;
    protected static final int FIELD_TYPE_SHORT        = 3 ;
    protected static final int FIELD_TYPE_BYTEBUFFER   = 4 ;
    protected static final int FIELD_TYPE_BOOLEAN      = 5 ;
    protected static final int FIELD_TYPE_ARRAY        = 6 ;
    private static final int SIZE_OF_ATTR              = 1 ;
    private static final int SIZE_OF_SIZE              = 4 ;
    private static final int SIZE_OF_COUNT             = 4 ;
    private static final int SIZE_OF_INT               = 4 ;
    private static final int SIZE_OF_TYPE              = 2 ;
    private static final int SIZE_OF_SHORT             = 2 ;
    private static final int SIZE_OF_BOOLEAN           = 1 ;

    public abstract void initField() ;
    public YpModule(byte[] data, int offset, int len){
        initField();
        parse(data, offset, len);
    }

    public YpModule(short type){
        initField();
        this.type = type ;
    }
    public int caleSize(){
        Field fields[] = getClass().getFields() ;
        int totalSize = SIZE_OF_TYPE ;
        for (int i = 0 ;i < fields.length ; ++i){
            try {
                Object o = fields[i].get(this) ;
                if( o != null){
                   totalSize += caleObjectSize(o) ;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return totalSize ;
    }
    private int caleObjectSize(Object o){
        int size = 0 ;
        int type = clsType(o.getClass()) ;
        switch (type){
            case FIELD_TYPE_INT :
                size = caleIntSize() ;
                break ;
            case FIELD_TYPE_SHORT :
                size = caleShortSize() ;
                break ;
            case FIELD_TYPE_STRING :
                size = caleStringSize((String) o) ;
                break ;
            case FIELD_TYPE_BYTEBUFFER :
                size = caleByteBufferSize((ByteBuffer) o) ;
                break ;
            case FIELD_TYPE_BOOLEAN :
                size = caleBooleanSize() ;
                break ;
            case FIELD_TYPE_ARRAY :
                size = caleArraySize((List) o);
                break;
        }
        return size ;
    }
    private int caleStringSize(String val){
        return  SIZE_OF_ATTR + SIZE_OF_SIZE + val.getBytes().length ;
    }
    private int caleIntSize(){
        return SIZE_OF_ATTR + SIZE_OF_INT ;
    }
    private int caleShortSize(){
        return SIZE_OF_ATTR + SIZE_OF_SHORT ;
    }
    private int caleBooleanSize(){
        return SIZE_OF_ATTR + SIZE_OF_BOOLEAN ;
    }
    private int caleByteBufferSize(ByteBuffer byteBuffer){
        return  SIZE_OF_ATTR + SIZE_OF_SIZE + byteBuffer.limit() ;
    }
    private int caleArraySize(List array){
        int size = 0 ;
        size += SIZE_OF_COUNT ;
        for(int i = 0 ;i < array.size() ; ++i){
            Object o = array.get(i) ;
            if(o != null && o instanceof YpModule) {
                size += ((YpModule) o).caleSize() ;
            }else{
                LtLog.w("cale array size error array member must be instanceof YpModule") ;
            }
        }
        return  size ;
    }
    public byte[] toProtocolData(){
        ByteBuffer byteBuffer = ByteBuffer.allocate(caleSize() + SIZE_OF_SIZE) ;
        byteBuffer.putInt(0);
        byteBuffer.putShort(type) ;
        Iterator<Map.Entry<Byte, Field> > iter = fields.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Byte,Field> entry = iter.next();
            putField(entry.getValue(), entry.getKey(), byteBuffer);
        }
        byteBuffer.putInt(0, byteBuffer.position() - 4) ;
        return byteBuffer.array() ;
    }
    //打包所有属性到bytebuffer
    public ByteBuffer packageData(ByteBuffer byteBuffer){
        Iterator<Map.Entry<Byte, Field> > iter = fields.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Byte,Field> entry = iter.next();
            putField(entry.getValue(), entry.getKey(), byteBuffer);
        }
        return byteBuffer ;
    }
    public ByteBuffer toDataWithLength(){
        ByteBuffer byteBuffer = ByteBuffer.allocate(caleSize()+ 4) ;
        byteBuffer.putInt(0) ; //占位
        toData(byteBuffer) ;
        byteBuffer.putInt(0, byteBuffer.position() -4) ;
        return byteBuffer ;
    }
    public byte[] toNormalData(){
        ByteBuffer byteBuffer = ByteBuffer.allocate(caleSize()) ;
        return toData(byteBuffer).array() ;
    }
    private ByteBuffer toData(ByteBuffer byteBuffer){
        byteBuffer.putShort(type) ;
        Iterator<Map.Entry<Byte, Field> > iter = fields.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Byte,Field> entry = iter.next();
            putField(entry.getValue(), entry.getKey(), byteBuffer);
        }
        return byteBuffer ;
    }
    public ByteBuffer toData(){
       ByteBuffer byteBuffer = ByteBuffer.allocate(caleSize()) ;
       return  toData(byteBuffer) ;
    }
//    //打包所有属性到bytebuffer
//    public ByteBuffer packageData(ByteBuffer byteBuffer){
//        Iterator<Map.Entry<Byte, Field> > iter = fields.entrySet().iterator();
//        while (iter.hasNext()) {
//            Map.Entry<Byte,Field> entry = iter.next();
//            putField(entry.getValue(), entry.getKey(), byteBuffer);
//        }
//        return byteBuffer ;
//    }
//    public byte[] toData(){
//        ByteBuffer byteBuffer = ByteBuffer.allocate(caleSize()) ;
//        byteBuffer.putShort(type) ;
//        Iterator<Map.Entry<Byte, Field> > iter = fields.entrySet().iterator();
//        while (iter.hasNext()) {
//            Map.Entry<Byte,Field> entry = iter.next();
//            putField(entry.getValue(), entry.getKey(), byteBuffer);
//        }
//
//        byte b[] = new byte[byteBuffer.position()] ;
//        byteBuffer.flip() ;
//        byteBuffer.get(b);
//        return b ;
//    }
    private int clsType(Class cls){
        if(cls.equals(String.class)) {
            return FIELD_TYPE_STRING ;
        }
        if(cls.equals(Integer.class) || cls.equals(int.class)) {
            return FIELD_TYPE_INT ;
        }
        if(cls.equals(Short.class) || cls.equals(short.class)) {
            return FIELD_TYPE_SHORT ;
        }
        if(cls.equals(ByteBuffer.class)){
            return FIELD_TYPE_BYTEBUFFER ;
        }
        if(cls.equals(Boolean.class) || cls.equals(boolean.class)){
            return FIELD_TYPE_BOOLEAN ;
        }
        if(List.class.isAssignableFrom(cls)){
            return FIELD_TYPE_ARRAY ;
        }
        return FIELD_TYPE_UNKNOWN ;
    }
    private int fieldType(Field field){
        return clsType(field.getType());
    }
    private void putField(Field field, int attr, ByteBuffer byteBuffer){
        switch (fieldType(field)){
            case FIELD_TYPE_INT :
                putInt(field, (byte) attr,byteBuffer) ;
                break ;
            case FIELD_TYPE_SHORT :
                putShort(field, (byte) attr,byteBuffer) ;
                break ;
            case FIELD_TYPE_STRING :
                putString(field, (byte) attr,byteBuffer) ;
                break ;
            case FIELD_TYPE_BOOLEAN :
                putBoolean(field, (byte) attr, byteBuffer) ;
                break;
            case FIELD_TYPE_BYTEBUFFER :
                putByteBuffer(field, (byte) attr, byteBuffer) ;
                break ;
            case FIELD_TYPE_ARRAY :
                putArray(field, (byte) attr, byteBuffer) ;
                break;
            default:
                break ;
        }
    }

    public void parse(byte[] data, int offset, int len){
        int cursor = offset ;
        while( cursor < (len + offset) ){
            byte attr = data[cursor] ;
            cursor += 1 ;
            Field field =  fields.get(attr) ;
            if(field == null){
                LtLog.w("unknown attr:"+attr) ;
                break ;
            }
            cursor = setFieldValue(field, data, cursor) ;
            if(cursor <=0){
                LtLog.w("error in parse moduel.........") ;
                break ;
            }
        }
    }

    private int setStringValue(Field field, byte[]data, int offset){
        int valLen = Utils.bytesToInt(data, offset) ;
        offset += 4 ;
        try {
            field.set(this, Utils.bytesToString(data, offset, valLen));
            offset += valLen ;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return offset ;
    }
    private int setBooleanValue(Field field, byte[] data, int offset){
        try {
            field.set(this, (data[offset] != 0));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        offset += 1 ;
        return  offset ;
    }
    private boolean putBoolean(Field field, byte attr, ByteBuffer byteBuffer){
        boolean ret = false ;
        try {
            boolean val = (boolean) field.get(this);
            if(attr != 0){
                byteBuffer.put(attr) ;
                byteBuffer.put((byte) (val?1:0)) ;
                ret = true ;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return ret ;
    }
    private int setByteBufferValue(Field field, byte[] data, int offset){
        int valLen = Utils.bytesToInt(data, offset) ;
        offset += 4 ;
        ByteBuffer byteBuffer = ByteBuffer.allocate(valLen) ;
        byteBuffer.put(data, offset, valLen) ;
        offset += valLen ;
        try {
            field.set(this, byteBuffer);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return offset ;
    }
    /*
    public int setArrayValue(Field field, byte[] data, int offset){
        int arraySize = Utils.bytesToInt(data, offset) ;
        offset += 4 ;
        List<YpModule> list = new ArrayList<>(arraySize) ;
        for(int i = 0 ;i < arraySize ; ++i){
            int moduleLength = Utils.bytesToInt(data, offset) ;
            offset += 4 ;
            short type = Utils.bytesToShort(data, offset) ;
        }
        return  offset ;
    }*/
    private boolean putArray(Field field, byte attr, ByteBuffer byteBuffer){
        boolean ret = false ;
        try {
            List<YpModule> list = (List<YpModule>) field.get(this);
            if(attr != 0){
                byteBuffer.put(attr) ;
                byteBuffer.putInt(list.size()) ;
                Iterator<YpModule> iter = list.iterator() ;
                while(iter.hasNext()){
                    YpModule ypModule = iter.next() ;
                    ypModule.packageData(byteBuffer) ;
                }
                ret = true ;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return  ret ;
    }
    private boolean putByteBuffer(Field field, byte attr, ByteBuffer byteBuffer){
        boolean ret = false ;
        try {
            ByteBuffer val = (ByteBuffer) field.get(this);
            if (attr != 0){
                byteBuffer.put(attr) ;
                byteBuffer.putInt(val.limit()) ;
                byteBuffer.put(val) ;
                ret = true ;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return ret ;
    }
    private boolean putString(Field field, byte attr, ByteBuffer byteBuffer){
        boolean ret = false ;
        try {
                String val = (String) field.get(this);
                if(val != null && attr != 0) {
                    byteBuffer.put(attr);
                    byteBuffer.putInt(val.getBytes().length) ;
                    byteBuffer.put(val.getBytes()) ;
                    ret = true ;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        return ret ;
    }

    private int setIntValue(Field field, byte[] data, int offset){
        try {
            field.set(this, Utils.bytesToInt(data, offset));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        offset += 4 ;
        return offset ;
    }
    private boolean putInt(Field field, byte attr, ByteBuffer byteBuffer){
        boolean ret = false ;
        try {
            int val = field.getInt(this) ;
            if(attr != 0){
                byteBuffer.put(attr) ;
                byteBuffer.putInt(val);
                ret = true ;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return ret ;
    }

    private int setShortValue(Field field, byte[] data, int offset){
        try {
            field.setShort(this,  Utils.bytesToShort(data, offset));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        offset += 2 ;
        return offset ;
    }
    private boolean putShort(Field field, byte attr, ByteBuffer byteBuffer){
        boolean ret = false ;
        try {
            short val = field.getShort(this) ;
            if(attr != 0){
                byteBuffer.put(attr) ;
                byteBuffer.putShort(val) ;
                ret = true ;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return ret ;
    }
    private int setFieldValue(Field field, byte[]data, int offset){
        switch (fieldType(field)){
            case FIELD_TYPE_INT :
                return setIntValue(field, data, offset) ;
            case FIELD_TYPE_SHORT :
                return setShortValue(field, data, offset) ;
            case FIELD_TYPE_STRING :
                return setStringValue(field, data, offset) ;
            case FIELD_TYPE_BOOLEAN :
                return setBooleanValue(field, data, offset) ;
            case FIELD_TYPE_BYTEBUFFER :
                return setByteBufferValue(field, data, offset) ;
            case FIELD_TYPE_ARRAY :
//                return setArrayValue(field, data, offset) ;
            default:
                break ;
        }
        return -1 ;
    }

}