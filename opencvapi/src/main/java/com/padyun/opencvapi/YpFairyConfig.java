package com.padyun.opencvapi;

import android.os.Build;

import com.padyun.opencvapi.utils.Utils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by litao on 2018/3/9.
 */
public class YpFairyConfig {

    private static final String OPTION_FILE_PATH1 = "/sdcard/yunpai_files/uicache/task.uicfg" ;
    private static final String OPTION_FILE_PATH2 = "/data/as/task_config/task.uicfg" ;
    private static final String CONFIG_FILE = "/etc/ypconfig.ini" ;
    private static final String USER_CONFIG_PATH = "/etc/user_config.ini" ;
    private static final String KEY_URL = "api_base" ;
    private static final String KEY_ASID = "as_id" ;
    private static final String KEY_CNID = "cn_id" ;
    private static final String KEY_ASPORT= "as_port" ;
    private static final String KEY_BASE_DOWNLOAD = "base_download" ;
    private static final String KEY_GAMEID = "game_id" ;
    private static final String KEY_CHANNELID = "channel_id" ;
    private static final String KEY_UID = "uid" ;
    private static final String KEY_DID = "did" ;
    private static final String KEY_NAME = "name" ;
    private static final String KEY_TASK_NAME = "task_name" ;
    private static final String KEY_TASKID = "task_id" ;
    private static final String KEY_OPENCV_SERVICE = "opencv_service" ;
    private static String sCfg ;
    private static String sCfgFile ;

    static{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            sCfgFile = OPTION_FILE_PATH2 ;
        }else{
            sCfgFile = OPTION_FILE_PATH1 ;
        }
    }

    public static void initConfig(){
        sCfg = Utils.stringFile(sCfgFile) ;
    }
    public static String getOption(String key){
        String option = "" ;
        try {
            if(sCfg != null && sCfg.length() > 0) {
                JSONObject jsonObject = new JSONObject(sCfg);
                option = jsonObject.optString(key);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return option ;
    }
    public static void clearConfig(){
        new File(sCfgFile).delete() ;
        sCfg = "" ;
    }
    public static String getUserTaskConfig(){
        return sCfg ;
    }
    public static String getServerUrl(){
        return  getConfig(CONFIG_FILE, KEY_URL) ;
    }
    public static String getUID(){
        return  getConfig(USER_CONFIG_PATH, KEY_UID) ;
    }
    public static String getDID(){
        return  getConfig(USER_CONFIG_PATH, KEY_DID) ;
    }
    public static String getASID(){
        return  getConfig(CONFIG_FILE, KEY_ASID) ;
    }
    public static String getCNID(){
        return  getConfig(CONFIG_FILE, KEY_CNID) ;
    }
    public static String getASPort(){
        return getConfig(CONFIG_FILE, KEY_ASPORT) ;
    }
    public static String getBaseDownload(){
        return  getConfig(CONFIG_FILE, KEY_BASE_DOWNLOAD) ;
    }
    public static String getGameID(){
        return  getConfig(USER_CONFIG_PATH, KEY_GAMEID) ;
    }
    public static String getGamePackage(){
        return  getConfig(USER_CONFIG_PATH, KEY_NAME) ;
    }
    public static String getTaskPackage(){
        return  getConfig(USER_CONFIG_PATH, KEY_TASK_NAME) ;
    }
    public static String getChannelID(){
        return  getConfig(USER_CONFIG_PATH, KEY_CHANNELID) ;
    }
    public static String getServerInfo(){
        return  getConfig(CONFIG_FILE, KEY_OPENCV_SERVICE) ;
    }
    public static String getTaskID(){
        return getOption(KEY_TASKID) ;
    }
    private static String getConfig(String config, String key){
        String val = "" ;
        Properties properties = new Properties() ;
        FileInputStream in = null;
        try {
            in = new FileInputStream(config);
            properties.load(in);
            val = properties.getProperty(key) ;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(in != null){
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return  val ;
    }
}
