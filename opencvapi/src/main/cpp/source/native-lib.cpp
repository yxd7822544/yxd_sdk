//
// Created by Administrator on 2019-10-16.
//
#include "opencv2/opencv.hpp"
#include "opencv2/imgcodecs.hpp"
#include <jni.h>
#include <iostream>
#include <android/log.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <cstring>
#include <string>

using namespace cv;

#define  LOG_TAG    "native-dev"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
using namespace std;
//#ifndef _Included_cn_scut_dongxia_opencvdemo_MainActivity
//#define _Included_cn_scut_dongxia_opencvdemo_MainActivity
//#ifndef _Included_com_padyun_lxh_jxqy_MainActivity
//#define _Included_com_padyun_lxh_jxqy_MainActivity
//#ifndef _Included_com_padyun_lxh_jxqy_Jxqy
//#define _Included_com_padyun_lxh_jxqy_Jxqy


#ifdef __cplusplus
extern "C" {
#endif


JNIEXPORT jfloatArray JNICALL
Java_com_padyun_lxh_jxqy_PublicFunction_FindPic(JNIEnv *, jobject, jint, jint, jint, jint,
                                                jbyteArray, jobject, jstring);
JNIEXPORT jfloatArray JNICALL
Java_com_padyun_lxh_jxqy_PublicFunction_FindPicMat(JNIEnv *, jobject, jint, jint, jint, jint, jlong,
                                                   jobject);
JNIEXPORT void JNICALL
Java_com_padyun_yxd_framewor_YpYxdFairyImpl_multipointFindColor(JNIEnv *env, jobject obj, jint, jint,
                                                            jint, jint, jbyteArray,
                                                            jlong, jstring, jstring, jdouble,
                                                            jintArray);
JNIEXPORT void JNICALL Java_com_padyun_lxh_jxqy_Jxqy_color(JNIEnv *, jobject);


//JNIEXPORT void JNICALL Java_com_padyun_lxh_jxqy_MainActivity_color__(JNIEnv* ,jobject);
void calculateSimilarity(Vec3b rgb1, int *rgb2);
vector<string> split(const string &, const string &);
#ifdef __cplusplus
}
#endif


JNIEXPORT jfloatArray JNICALL
Java_com_padyun_lxh_jxqy_PublicFunction_FindPicMat(JNIEnv *env, jobject obj, jint x1, jint y1,
                                                   jint x2, jint y2, jlong imgMat, jobject AM) {
    LOGI("======FindPicMat====================================================================");
    AAssetManager *mgr = AAssetManager_fromJava(env, AM);

    AAsset *asset = AAssetManager_open(mgr, "activity.png", AASSET_MODE_UNKNOWN);

    size_t fileLength = AAsset_getLength(asset);
    char *dataBuffer2 = (char *) malloc(fileLength);
    AAsset_read(asset, dataBuffer2, fileLength);
    AAsset_close(asset);
    std::vector<char> vec2(dataBuffer2, dataBuffer2 + fileLength);
    cv::Mat img1 = cv::imdecode(vec2, CV_LOAD_IMAGE_COLOR);

    cv::Mat mGr = (*((cv::Mat *) imgMat));

    LOGI("======FindPicMat====================================================================%d",
         mGr.channels());
    cv::Mat dstImg;


    dstImg.create(mGr.dims, mGr.size, mGr.type());
    cv::matchTemplate(img1, mGr, dstImg, cv::TM_CCOEFF_NORMED);
    cv::Point minPoint;
    cv::Point maxPoint;
    double minVal = 0;
    double maxVal = 0;
    cv::minMaxLoc(dstImg, &minVal, &maxVal, &minPoint, &maxPoint);

    jfloat *value = new jfloat[3];
    value[0] = (jfloat) 0.1;
    value[1] = (jfloat) 0.1;
    value[2] = (jfloat) 0.1;

    jfloatArray jarr = env->NewFloatArray(3);

    env->SetFloatArrayRegion(jarr, 0, 3, value);

    return jarr;

}


JNIEXPORT jfloatArray JNICALL
Java_com_padyun_lxh_jxqy_PublicFunction_FindPic(JNIEnv *env, jobject obj, jint x1, jint y1, jint x2,
                                                jint y2, jbyteArray imgBytes, jobject AM,
                                                jstring fileName) {

    jbyte *img = env->GetByteArrayElements(imgBytes, NULL);
    cv::Mat imgMat(720, 1280, CV_8UC4, (unsigned char *) img);
    cv::cvtColor(imgMat, imgMat, cv::COLOR_BGRA2RGB);

    cv::Mat rectMat(imgMat, CvRect(x1, y1, x2 - x1, y2 - y1));

    cv::Mat dstImg;
    dstImg.create(rectMat.dims, rectMat.size, rectMat.type());

    AAssetManager *mgr = AAssetManager_fromJava(env, AM);

    const char *str = env->GetStringUTFChars(fileName, JNI_FALSE);
    string s(str);
    vector<string> AllStr;
    if (s.find("|") != string::npos) {
        AllStr = split(str, "|");
    } else {
        AllStr.push_back(s);
    }
    jfloat *value = new jfloat[3];
    jfloat minSim = 0.0;
    for (int i = 0; i < AllStr.size(); ++i) {
        AAsset *asset = AAssetManager_open(mgr, AllStr[i].c_str(), AASSET_MODE_UNKNOWN);
        size_t fileLength = AAsset_getLength(asset);
        char *dataBuffer2 = (char *) malloc(fileLength);
        AAsset_read(asset, dataBuffer2, fileLength);
        AAsset_close(asset);
        std::vector<char> vec2(dataBuffer2, dataBuffer2 + fileLength);
        Mat img1 = cv::imdecode(vec2, CV_LOAD_IMAGE_COLOR);
//        UMat imgU,rectMatU,UdstImg;
//        imgU.copyTo(img1);
//        rectMatU.copyTo(rectMat);
//        UdstImg.copyTo(dstImg);
        LOGI("======FindPicMat====================================================================match");
        cv::matchTemplate(img1, rectMat, dstImg, cv::TM_CCOEFF_NORMED);
        cv::Point minPoint;
        cv::Point maxPoint;
        double minVal = 0;
        double maxVal = 0;
        cv::minMaxLoc(dstImg, &minVal, &maxVal, &minPoint, &maxPoint);
//        LOGI("======FindPicMat====================================================================%lf" ,maxVal);
        if ((jfloat) maxVal > minSim) {
            minSim = (jfloat) maxVal;
            value[0] = (jfloat) maxVal;
            value[1] = (jfloat) maxPoint.x + x1;
            value[2] = (jfloat) maxPoint.y + y1;
        }
        img1.release();
    }


    jfloatArray jarr = env->NewFloatArray(3);
    env->SetFloatArrayRegion(jarr, 0, 3, value);

    free(value);
    rectMat.release();

    imgMat.release();
    dstImg.release();
//    LOGI("======FindPic=============return" );
    return jarr;

}


vector<string> split(const string &str, const string &delim) {
    vector<string> res;
    if ("" == str) return res;
    //先将要切割的字符串从string类型转换为char*类型
    char *strs = new char[str.length() + 1]; //不要忘了
    strcpy(strs, str.c_str());

    char *d = new char[delim.length() + 1];
    strcpy(d, delim.c_str());

    char *p = strtok(strs, d);
    while (p) {
        string s = p; //分割得到的字符串转换为string类型
        res.push_back(s); //存入结果数组
        p = strtok(NULL, d);
    }

    return res;
}

void calculateSimilarity(Vec3b rgb1, int *rgb2) {
    double trageSim = 0.9;
    double c = 25.5;
    int c1 = rgb1[0] - rgb2[0];
//    if (c1>c){
//        return;
//    }
//    int c2=abs(rgb1[1]-rgb2[1]);
//    if(c2>c){
//        return;
//    }
//    int c3=abs(rgb1[2]-rgb2[2]);
//    if(c3>c){
//        return;
//    }



//    double sim__ =1-(c1+c2+c3)/765;
//    double sim__ =765-(c1+c2+c3);

/*
    int rgb1_rgb2_1=abs(rgb1[0]-rgb2[0]);
    if (rgb1_rgb2_1>c){
        return;
    }

//    double sim1=fabs((rgb1[0]/255) - (rgb2[0]/255));

    double sim1=rgb1_rgb2_1/255;
//    if (sim1/3>c){
//        return;
//    }
//    double sim2=fabs((rgb1[1]/255) - (rgb2[1]/255));

    int rgb1_rgb2_2=abs(rgb1[1]-rgb2[1]);
//    if (rgb1_rgb2_2>c){
//        return;
//    }
    double sim2=rgb1_rgb2_2/255;

//    if((sim1+sim2)/3>c){
//        return;
//    }
//    double sim3=fabs((rgb1[2]/255) - (rgb2[2]/255));

    double sim3=fabs((rgb1[2]-rgb2[2])/255);
    double sim = 1-((sim1+sim2+sim3)/3);

//    LOGI("======multipointFindColor====================================================================%d%d%d",rgb2[0],rgb2[1],rgb2[2] );
//    LOGI("======multipointFindColor====================================================================%d%d%d" ,rgb1[0],rgb1[1],rgb1[2]);
//    double sim =0.0;
//    for (int i = 0; i <3 ; ++i) {
////      sim=sim+pow(pow((int)rgb1[i]/255 -  (int)rgb2[i]/255,2),0.5);
//        sim=sim+fabs((rgb1[i]/255) - (rgb2[i]/255));
//    }

//    if (sim>trageSim){
//        LOGI("======multipointFindColor===================================================================sim=%lf" ,sim);
//        LOGI("======multipointFindColor===================================================================sim1=%lf" ,sim1);
//        LOGI("======multipointFindColor===================================================================sim2=%lf" ,sim2);
//        LOGI("======multipointFindColor===================================================================sim3=%lf" ,sim3);
//    }
*/
}

int myAdd(int num1, int num2) {
    if (num2 == 0) return num1;
    int sum = 0, carry = 0;
    sum = num1 ^ num2;    // 按位抑或
    carry = (num1 & num2) << 1;
    return myAdd(sum, carry);
}


int myMinus(int num1, int num2) {
    return myAdd(num1, myAdd(~num2, 1));
}


//JNIEXPORT void JNICALL Java_com_padyun_lxh_jxqy_MainActivity_color__(JNIEnv *env, jobject obj){
JNIEXPORT void JNICALL
Java_com_padyun_yxd_framewor_YpYxdFairyImpl_multipointFindColor(JNIEnv *env, jobject obj, jint x_1,
                                                            jint y_1, jint x_2, jint y_2,
                                                            jbyteArray imgBytes, jlong imgMat,
                                                            jstring colorStr_start,
                                                            jstring colorStr_sub, jdouble targetSim,
                                                            jintArray xy) {


    int return_value[2] = {-1, -1};

    const char *str_start = env->GetStringUTFChars(colorStr_start, JNI_FALSE);
    const char *str_sub = env->GetStringUTFChars(colorStr_sub, JNI_FALSE);
    int rect_w=x_2-x_1;
    int rect_y=y_2-y_1;
    cv::Mat allMat = (*((cv::Mat *) imgMat));
    cv::Mat mGr(allMat, CvRect(x_1, y_1,rect_w, rect_y));


    vector<string> str_start_arr;
    int *str_start_rgb = new int[3];
    str_start_arr = split(str_start, ",");
    str_start_rgb[0] = stoi(str_start_arr[0]);
    str_start_rgb[1] = stoi(str_start_arr[1]);
    str_start_rgb[2] = stoi(str_start_arr[2]);
    vector<string> str_sub_arr;
    vector<string> str_sub_xy_rgb;
    char *str_sub_arr_sub;
    str_sub_arr = split(str_sub, "&");
    int str_sub_rgb[str_sub_arr.size()][3];
    int str_sub_xy[str_sub_arr.size()][2];
    vector<string> str_sub_rgb_all;
    for (int i = 0; i < str_sub_arr.size(); ++i) {
        str_sub_arr_sub = (char *) str_sub_arr[i].c_str();
        str_sub_xy_rgb = split(str_sub_arr_sub, "|");
        str_sub_xy[i][0] = stoi(str_sub_xy_rgb[0]);
        str_sub_xy[i][1] = stoi(str_sub_xy_rgb[1]);
        str_sub_rgb_all = split(str_sub_xy_rgb[2], ",");
        str_sub_rgb[i][0] = stoi(str_sub_rgb_all[0]);
        str_sub_rgb[i][1] = stoi(str_sub_rgb_all[1]);
        str_sub_rgb[i][2] = stoi(str_sub_rgb_all[2]);
    }
    double target = targetSim;
    int c1, c2, c3, c4;
    double sim;

    uchar *d;
    int index;

//    d = mGr.ptr<uchar>(487);
//    LOGI("======multipointFindColor==========================1=============================%d ",d[1224 *3]);
//    LOGI("======multipointFindColor==========================1=============================%d ",d[1224 *3+1]);
//    LOGI("======multipointFindColor==========================1=============================%d ",d[1224 *3+2]);
//    d = mGr.ptr<uchar>(565);
//    LOGI("======multipointFindColor==========================1=============================%d ",d[1254 *3]);
//    LOGI("======multipointFindColor==========================1=============================%d ",d[1254 *3+1]);
//    LOGI("======multipointFindColor==========================1=============================%d ",d[1254 *3+2]);

    for (int i = 0; i < mGr.rows; ++i) {
        d = mGr.ptr<uchar>(i);
        for (int j = 0; j < mGr.cols; ++j) {
            index = j * 3;
            c1 = d[index] - str_start_rgb[0];
            c1 = (c1 ^ (c1 >> 31)) - (c1 >> 31);
            c2 = d[index + 1] - str_start_rgb[1];
            c2 = (c2 ^ (c2 >> 31)) - (c2 >> 31);
            c3 = d[index + 2] - str_start_rgb[2];
            c3 = (c3 ^ (c3 >> 31)) - (c3 >> 31);
            c4 = 765 - c1 - c2 - c3;
            sim = c4 / 765.0;
            if (sim > target) {
//                LOGI("======multipointFindColor==========================1=============================sim=%lf %d %d ",sim, j,i);
                int x = j;
                int y = i;
                bool check = true;
                for (size_t k = 0; k < sizeof(str_sub_xy) / sizeof(*str_sub_xy); k++) {
                    int row = i + str_sub_xy[k][1];
                    int col = j + str_sub_xy[k][0];
//                    LOGI("======multipointFindColor==========================1==============================%d %d ",row,col);
                    if (row < 0 || row > rect_y || col < 0 || col > rect_w) {
                        check = false;
                        break;
                    }
                    index = col * 3;
                    int r1 = mGr.ptr<uchar>(row)[index];
                    int g1 = mGr.ptr<uchar>(row)[index + 1];
                    int b1 = mGr.ptr<uchar>(row)[index + 2];
                    c1 = r1 - str_sub_rgb[k][0];
                    c1 = (c1 ^ (c1 >> 31)) - (c1 >> 31);
                    c2 = g1 - str_sub_rgb[k][1];
                    c2 = (c2 ^ (c2 >> 31)) - (c2 >> 31);
                    c3 = b1 - str_sub_rgb[k][2];
                    c3 = (c3 ^ (c3 >> 31)) - (c3 >> 31);
                    c4 = c1 + c2 + c3;
                    sim = (765 - c4) / 765.0;
//                    LOGI("======multipointFindColor==========================1==============================%d %d %d %lf",r1,b1,g1,sim);
                    if (sim < target) {
                        check = false;
                        break;
                    }
                }
//                LOGI("======multipointFindColor==========================1=============================sim=%lf ",sim);
                if (check) {
//                    LOGI("======multipointFindColor==========================2=============================sim=%lf ",sim);
                    delete str_start_rgb;
                    str_start_arr.clear();
                    mGr.release();
                    str_sub_arr.clear();
                    str_sub_xy_rgb.clear();
                    str_sub_rgb_all.clear();
                    return_value[0] = x+x_1;
                    return_value[1] = y+y_1;
                    env->SetIntArrayRegion(xy, 0, 2, return_value);
                    return;
                }
            }
        }
    }


    delete str_start_rgb;
    str_start_arr.clear();
    mGr.release();
    str_sub_arr.clear();
    str_sub_xy_rgb.clear();
    str_sub_rgb_all.clear();
    env->SetIntArrayRegion(xy, 0, 2, return_value);
//    LOGI("======multipointFindColor==========================return============================= ");
    return;
}

//#endif

