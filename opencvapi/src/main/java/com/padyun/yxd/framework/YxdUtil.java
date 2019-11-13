package com.padyun.yxd.framework;

import android.content.Context;
import android.content.Intent;

import com.padyun.opencvapi.FindResult;
import com.padyun.opencvapi.LtLog;
import com.padyun.opencvapi.ScreenInfo;
import com.padyun.opencvapi.YpFairyConfig;
import com.padyun.opencvapi.utils.TemplateInfo;

import org.json.JSONObject;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Administrator on 2019/3/26 0026.
 */

public class YxdUtil {
    public long time = 0;
    YpYxdFairyImpl mFairy;

    public YxdUtil(YpYxdFairyImpl ypFairy) throws Exception {
        mFairy = ypFairy;
    }

    public void setTime(long time) {
        this.time = time;
    }

    /**
     *
     * 时间判定
     * @param order  0不需要先执行  1先执行一次
     * @param t
     * @return  时间到了返回true
     */
    public boolean timeJudge(long t) {
        if (time == 0) {
            time = System.currentTimeMillis();
        } else {
            if (System.currentTimeMillis() - time >= t) {
                time = System.currentTimeMillis();
                return true;
            }
        }
        return false;
    }

    private List<Integer> title_Range;
    private List<Integer> answer_Range;
    private List<Integer> pic_range;
    private String right_pic;

    public Map<String, List<Integer>> initAnswerMap() {
        Map<String, List<Integer>> mMap = new HashMap<>();
        mMap.put("title_Range", null);
        mMap.put("answer_Range", null);
        mMap.put("pic_range", null);
        mMap.put("right_pic_Relative_range", null);
        return mMap;
    }

    /**
     * @param mMap    需要的数组
     * @param picName 正确答案的图片
     * @throws Exception 示例：
     *                   Map<String,List<Integer>> answerMap= answer.initAnswerMap();
     *                   answerMap.put("title_Range",Arrays.asList(520,191,483,28));
     *                   answerMap.put("answer_Range",Arrays.asList(555,436,774,494,   840,440,1055,494,  557,516,777,577,  840,524,1056,576 ));
     *                   answerMap.put("pic_range",Arrays.asList(515,419,1070,590));
     */
    public void mAnswer(Map<String, List<Integer>> mMap, String picName) throws Exception {
        title_Range = mMap.get("title_Range");//题目范围
        answer_Range = mMap.get("answer_Range");//答案范围
        pic_range = mMap.get("pic_range");//判断正确答案识别图的范围
        right_pic = picName;//判断正确答案的识别图
        AIAnswer();
    }


    public void AIAnswer() throws Exception {
        String answerStr = "";
        String mStr;
        List<String> answerStrABCD = new ArrayList<String>();
        mStr = mFairy.ocr(title_Range.get(0), title_Range.get(1), title_Range.get(2) + title_Range.get(0), title_Range.get(3) + title_Range.get(1));
        LtLog.e(mFairy.getLineInfo("题目是=" + mStr));
        if (mStr.isEmpty()) {
            LtLog.e(mFairy.getLineInfo("没有识别到题目,默认选A"));
            mFairy.onTap(answer_Range.get(0), answer_Range.get(1), answer_Range.get(2), answer_Range.get(3), "没有识别到题目,默认选A", 2000);
            return;
        }
        for (int j = 0; j < answer_Range.size(); j = j + 4) {
            answerStr = mFairy.ocr(answer_Range.get(j), answer_Range.get(j + 1), answer_Range.get(j + 2) + answer_Range.get(j), answer_Range.get(j + 3) + answer_Range.get(j + 1));
            if (j == 0) {
                answerStrABCD.add(answerStr);
                LtLog.e(mFairy.getLineInfo("A是=" + answerStr));
            }
            if (j == 4) {
                answerStrABCD.add(answerStr);
                LtLog.e(mFairy.getLineInfo("B是=" + answerStr));
            }
            if (j == 8) {
                answerStrABCD.add(answerStr);
                LtLog.e(mFairy.getLineInfo("C是=" + answerStr));
            }
            if (j == 12) {
                answerStrABCD.add(answerStr);
                LtLog.e(mFairy.getLineInfo("D是=" + answerStr));
            }
            if (j == 16) {
                answerStrABCD.add(answerStr);
                LtLog.e(mFairy.getLineInfo("E是=" + answerStr));
            }
            if (j == 20) {
                answerStrABCD.add(answerStr);
                LtLog.e(mFairy.getLineInfo("F是=" + answerStr));
            }
        }
        String[] answer = findAnswer(mStr, YpFairyConfig.getGameID());//查找答案
        if (answer == null) {
            LtLog.e(mFairy.getLineInfo("找到题目了但没有答案,开始上传"));
            upDown(mStr, answerStrABCD);
            return;
        } else {
            for (int j = 0; j < answerStrABCD.size(); j++) {
                for (int i = 0; i < answer.length; i++) {
                    LtLog.e(mFairy.getLineInfo("answer=" + answer[i] + "," + "answerStrABCD.get==" + answerStrABCD.get(j) + "," + answer[i].equals(answerStrABCD.get(j))));
                    if (answer[i].equals(answerStrABCD.get(j))) {
                        if (j == 0) {
                            mFairy.onTap(answer_Range.get(0), answer_Range.get(1), answer_Range.get(2), answer_Range.get(3), "匹配到答案是A" + answerStrABCD.get(j), 2000);
                        }
                        if (j == 1) {
                            mFairy.onTap(answer_Range.get(4), answer_Range.get(5), answer_Range.get(6), answer_Range.get(7), "匹配到答案是B" + answerStrABCD.get(j), 2000);
                        }
                        if (j == 2) {
                            mFairy.onTap(answer_Range.get(8), answer_Range.get(9), answer_Range.get(10), answer_Range.get(11), "匹配到答案是C" + answerStrABCD.get(j), 2000);
                        }
                        if (j == 3) {
                            mFairy.onTap(answer_Range.get(12), answer_Range.get(13), answer_Range.get(14), answer_Range.get(15), "匹配到答案是D" + answerStrABCD.get(j), 2000);
                        }
                        if (j == 4) {
                            mFairy.onTap(answer_Range.get(16), answer_Range.get(17), answer_Range.get(18), answer_Range.get(19), "匹配到答案是E" + answerStrABCD.get(j), 2000);
                        }
                        if (j == 5) {
                            mFairy.onTap(answer_Range.get(20), answer_Range.get(21), answer_Range.get(22), answer_Range.get(23), "匹配到答案是F" + answerStrABCD.get(j), 2000);
                        }
                        Thread.sleep(1000);
                        return;
                    }
                }
            }
            upDown(mStr, answerStrABCD);
        }
    }

    //选择答案并且上传题目和答案
    public void upDown(String mAswer, List<String> answerStrABCD) throws Exception {
        LtLog.i(mFairy.getLineInfo("----------------------------upDown>"));
        FindResult result;
        String answerStr = "";
        String mStr = mAswer;
        mFairy.onTap(answer_Range.get(0), answer_Range.get(1), answer_Range.get(2), answer_Range.get(3), "找到题目了可是没有答案,默认选A", 100);
        for (int i = 0; i < 50; i++) {
            result = mFairy.findPic(pic_range.get(0), pic_range.get(1), pic_range.get(2), pic_range.get(3), right_pic);
            if (result.sim >= 0.75f) {
                if ((result.x > answer_Range.get(0) && result.y > answer_Range.get(1) && result.x < answer_Range.get(2) && result.y < answer_Range.get(3)) && answer_Range.size() > 1) {
                    LtLog.e(mFairy.getLineInfo("正确答案是A"));
                    answerStr = answerStrABCD.get(0);
                    break;
                }
                if ((result.x > answer_Range.get(4) && result.y > answer_Range.get(5) && result.x < answer_Range.get(6) && result.y < answer_Range.get(7)) && answer_Range.size() > 4) {
                    LtLog.e(mFairy.getLineInfo("正确答案是B"));
                    answerStr = answerStrABCD.get(1);
                    break;
                }

                if ((result.x > answer_Range.get(8) && result.y > answer_Range.get(9) && result.x < answer_Range.get(10) && result.y < answer_Range.get(11)) && answer_Range.size() > 8) {
                    LtLog.e(mFairy.getLineInfo("正确答案是C"));
                    answerStr = answerStrABCD.get(2);
                    break;
                }

                if ((result.x > answer_Range.get(12) && result.y > answer_Range.get(13) && result.x < answer_Range.get(14) && result.y < answer_Range.get(15)) && answer_Range.size() > 12) {
                    LtLog.e(mFairy.getLineInfo("正确答案是D"));
                    answerStr = answerStrABCD.get(3);
                    break;
                }
                if ((result.x > answer_Range.get(16) && result.y > answer_Range.get(17) && result.x < answer_Range.get(18) && result.y < answer_Range.get(19)) && answer_Range.size() > 16) {
                    LtLog.e(mFairy.getLineInfo("正确答案是E"));
                    answerStr = answerStrABCD.get(4);
                    break;
                }
                if ((result.x > answer_Range.get(20) && result.y > answer_Range.get(21) && result.x < answer_Range.get(22) && result.y < answer_Range.get(23)) && answer_Range.size() > 20) {
                    LtLog.e(mFairy.getLineInfo("正确答案是F"));
                    answerStr = answerStrABCD.get(5);
                    break;
                }
                break;
            }
            Thread.sleep(100);
        }
        if (answerStr != "") {
            UpAnswerAndTitle(mStr, answerStr, YpFairyConfig.getGameID());
        }
    }

    //查找题目
    public String[] findAnswer(String title, String game_id) throws Exception {
        OkHttpClient client = new OkHttpClient();
        String resultStr = null;
        JSONObject jsonObject;
        Request request = new Request.Builder()
                .url("http://API.padyun.com/Yunpai/V1/IntelligentAnswer/FindTheAnswer?title=" + title + "&game_id=" + game_id)
                .get()
                .build();

        Response response = client.newCall(request).execute();
        resultStr = response.body().string();
        resultStr = new org.json.JSONTokener(resultStr).nextValue().toString();
        jsonObject = new JSONObject(resultStr);
        if (jsonObject.getString("data").equals("false")) {
            LtLog.i(mFairy.getLineInfo("-----------+++---------not title"));
        } else {
            String arr;
            arr = jsonObject.getString("data").replaceAll("\\[", "");
            arr = arr.replaceAll("\\]", "");
            arr = arr.replaceAll("\"", "");
            String[] array = arr.split(",");
            LtLog.i(mFairy.getLineInfo("-----------+++---------array=" + array));
            return array;
        }
        return null;
    }

    //上传题目和答案
    public void UpAnswerAndTitle(String title, String answer, String game_id) throws InterruptedException {
        String resultStr = null;
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "title=" + title + "&game_id=" + game_id + "&answer=" + answer);
        Request request = new Request.Builder()
                .url("http://API.padyun.com/Yunpai/V1/IntelligentAnswer/AddTitle")
                .post(body)
                .build();
        try {
            Response response = client.newCall(request).execute();
            resultStr = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Thread.sleep(100);
    }

    private Mat mat1, mat2;
    private long timex, time1;
    public long mMatTime(int x_1, int y_1, int width, int height, double sim) throws Exception {
        /*
         返回两个图片相等的时间
         */
        boolean matSim = false;
        ScreenInfo screenInfo=mFairy.capture();
        if (screenInfo.height > 720) {
            LtLog.e("----screenInfo error ---");
            return 0;
        }
        if (mat1 != null) {
            mat1.release();
        }
        mat1 =  mFairy.getScreenMat(x_1, y_1, width, height, 1, 0, 0, 1);
        if (mat2 != null && mat1 != null) {
            // LtLog.e(getLineInfo(  "----------------------------mat1.rows=>" + mat1.rows()  + ",mat2.rows="+ mat2.rows()));
           /* try {*/
            matSim = judgeMatAndMatChange(sim, mat1, mat2);

         /*   } catch (Exception e) {
//                LtLog.i(publicFunction.getLineInfo() + "----------------------------matSim>" + e.toString());
            }*/
            mat1.release();
            //判断两个矩阵的相似度大于 sim 则返回 true;
        }
        if (matSim) {
            // LtLog.e(getLineInfo("目前区域没有变化"));
//            LtLog.i(publicFunction.getLineInfo() + "----------------------------matSim>" + matSim + ",timex=" + timex + ",time=" + time);
        } else {
            //如果两个矩阵不相等 重置时间
//            LtLog.i(publicFunction.getLineInfo() + "----------------------------matSim>" + matSim );
            time1 = System.currentTimeMillis() / 1000;
            if (mat2 != null) {
                mat2.release();
            }
            mat2 = mFairy.getScreenMat(x_1, y_1, width, height, 1, 0, 0, 1);
            return 0;
        }
        timex = System.currentTimeMillis() / 1000 - time1;
        return timex;
    }

    /**
     * 初始化MatTime的时间
     */
    public void initMatTime() {
        if (mat2 != null) {
            mat2.release();
        }
        mat2 = null;
    }

    /**
     * 判断两个矩阵的相似度大于 sim 则返回 true;
     */
    private boolean judgeMatAndMatChange(double sim, Mat mat, Mat tempMat) throws Exception {
        //判断两个矩阵的相似度大于 sim 则返回 true;
        boolean state = false;
        Mat dstMat = new Mat(), dst1 = new Mat(), dst2 = new Mat();
        if (mat.channels() == 3 || tempMat.channels() == 3) {
            Imgproc.cvtColor(mat, dst1, Imgproc.COLOR_RGB2HLS);
            Imgproc.cvtColor(tempMat, dst2, Imgproc.COLOR_RGB2HLS);
        }
        Imgproc.matchTemplate(dst1, dst2, dstMat, Imgproc.TM_CCOEFF_NORMED);
        Core.MinMaxLocResult mmr;
        mmr = Core.minMaxLoc(dstMat);
        if (mmr.maxVal >= sim) {
            state = true;
        }
        dstMat.release();
        dst1.release();
        dst2.release();
        return state;
    }
}
