package com.xmut.shoppingcenter.okhttp.http;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * @since 2021-04-28
 * 异步请求会放到线程池中执行
 * 同步请求会阻塞当前线程
 * 一般使用异步请求居多
 */
public class OKHttpUtil {
    private static Request request = null;
    private static int TimeOut = 5;
    private static int SendTimeOut = 3000; // 2000ms == 2s;
    private static int OnceTimeOut = 20; // 2000ms == 2s;
    //单例获取ohttp3对象
    private static OkHttpClient client = null;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    /**
     * OkHttpClient的构造方法，通过线程锁的方式构造
     * @return OkHttpClient对象
     */
    private static synchronized OkHttpClient getInstance() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .readTimeout(TimeOut, TimeUnit.SECONDS)
                    .connectTimeout(TimeOut, TimeUnit.SECONDS)
                    .writeTimeout(TimeOut, TimeUnit.SECONDS)
                    .build();
        }
        return client;
    }

    /**
     * callback接口
     * 异步请求时使用
     */
    static class MyCallBack implements Callback {
        private OkHttpCallback okHttpCallBack;

        public MyCallBack(OkHttpCallback okHttpCallBack) {
            this.okHttpCallBack = okHttpCallBack;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            okHttpCallBack.onFailure(e);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            okHttpCallBack.onSuccess(response);
        }
    }
    /**
     * 获得同步get请求对象Response
     * @param url
     * @return Response
     */
    private static Response doSyncGet(String url) {
        //创建OkHttpClient对象
        client = getInstance();
        request = new Request.Builder()
                .url(url)//请求链接
                .build();//创建Request对象
        try {
            //获取Response对象
            Response response = client.newCall(request).execute();
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * 获得异步get请求对象
     * @param url      请求地址
     * @param callback 实现callback接口
     */
    private static void doAsyncGet(String url,OkHttpCallback callback) {
        MyCallBack myCallback = new MyCallBack(callback);
        client = getInstance();
        request = new Request.Builder()
                .url(url)
                .get()
                .build();
        client.newCall(request).enqueue(myCallback);
    }
    /**
     * 同步get请求
     * 例如：请求的最终地址为：http://127.0.0.1:8081/user/getUser/123
     * @param url 基本请求地址   例子： http://127.0.0.1:8081
     * @param args 请求的参数    args[]=new String[]{"user","getUser","123"}
     * @return String
     */
    public static String getSyncRequest(String url,String... args) {
        List<String> result=new ArrayList<>();//返回值
        String address=url;
        for(int i=0;i<args.length;i++){
            address=address+"/"+args[i];
        }
        final String finalAddress = address;
        new Thread(new Runnable() {
            @Override
            public void run() {
                Response finalResponse = doSyncGet(finalAddress);
                String res = null;
                try {
                    Log.d("同步get请求请求地址：",finalAddress);
                    if (finalResponse.isSuccessful()) {//请求成功
                        ResponseBody body = finalResponse.body();//拿到响应体
                        res = body.string();
                        result.add(res);
                        Log.d("HttpUtil", "同步get请求成功！");
                        Log.d("请求对象：", res);
                    } else {
                        Log.d("HttpUtil", "同步get请求失败！");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        /**因为函数返回是立刻执行的，而result要在请求完成之后才能获得
         * 所以需要等待result获得返回值之后再执行return*/
        long startTime =  System.currentTimeMillis();

        /** 获取当前的系统时间，与初始时间相减就是程序运行的毫秒数，除以1000就是秒数*/

        while (result.size()==0  ){
            try {
                TimeUnit.MILLISECONDS.sleep(OnceTimeOut);//等待xx毫秒

                if((System.currentTimeMillis()-startTime)>SendTimeOut)
                    break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(result.isEmpty()) return null;
        return result.get(0);
    }


    /**
     * 异步get请求
     * 例如：请求的最终地址为：http://127.0.0.1:8081/user/getUser/123
     * @param url 基本请求地址   例子： http://127.0.0.1:8081
     * @param args 请求的参数    args[]=new String[]{"user","getUser","123"}
     * @return String
     */
    public static String getAsyncRequest(String url,String... args){
        List<String> result=new ArrayList<>();
        String address=url;
        for(int i=0;i<args.length;i++){
            address=address+"/"+args[i];
        }
        final String finalAddress = address;
        doAsyncGet(finalAddress, new OkHttpCallback() {
            @Override
            public void onFailure(IOException e) {
                Log.d("异步get请求地址：",finalAddress);
                Log.d("HttpUtil", "异步get请求失败！");
            }
            @Override
            public void onSuccess(Response response) {
                Log.d("异步get请求地址：",finalAddress);
                String res = null;
                try {
                    res = response.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                result.add(res);
                Log.d("HttpUtil", "异步get请求成功！");
                Log.d("请求对象：", res);
            }
        });
        /**因为函数返回是立刻执行的，而result要在请求完成之后才能获得
         * 所以需要等待result获得返回值之后再执行return*/
        long startTime =  System.currentTimeMillis();

        /** 获取当前的系统时间，与初始时间相减就是程序运行的毫秒数，除以1000就是秒数*/

        while (result.size()==0  ){
            try {
                TimeUnit.MILLISECONDS.sleep(OnceTimeOut);//等待xx毫秒

                if((System.currentTimeMillis()-startTime)>SendTimeOut)
                    break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(result.isEmpty()) return null;
        return result.get(0);
    }

    /**
     * 同步post请求
     * 例如：请求的最终地址为：http://127.0.0.1:8081/user/getUser/123
     * @param url 基本请求地址   例子： http://127.0.0.1:8081
     * @param json 提交的json字符串
     * @param args 请求的参数    args[]=new String[]{"user","getUser","123"}
     * @return
     */
    public static String postSyncRequest(String url,String json,String... args){
        List<String> result=new ArrayList<>();
        String address=url;
        for(int i=0;i<args.length;i++){
            address=address+"/"+args[i];
        }
        final String finalAddress = address;
        new Thread(new Runnable() {
            @Override
            public void run() {
                client=getInstance();
                Log.d("同步post请求地址：",finalAddress);
                RequestBody body = RequestBody.create(JSON, json);

//                FormBody.Builder formBody = new FormBody.Builder();
//                formBody.add("json",json);
//                formBody.build()
                request=new Request.Builder()
                        .url(finalAddress)
                        .post(body)
                        .addHeader("device-platform", "android")
                        .build();
                try{
                    Response response=client.newCall(request).execute();
                    String res=response.body().string();
                    result.add(res);
                    Log.d("HttpUtil", "同步post请求成功！");
                    Log.d("请求对象：", res);
                }catch (Exception e){
                    Log.d("HttpUtil", "同步post请求失败！");
                    e.printStackTrace();
                }
            }
        }).start();
        /**因为函数返回是立刻执行的，而result要在请求完成之后才能获得
         * 所以需要等待result获得返回值之后再执行return*/
        long startTime =  System.currentTimeMillis();

        /** 获取当前的系统时间，与初始时间相减就是程序运行的毫秒数，除以1000就是秒数*/

        while (result.size()==0  ){
            try {
                TimeUnit.MILLISECONDS.sleep(OnceTimeOut);//等待xx毫秒

                if((System.currentTimeMillis()-startTime)>SendTimeOut)
                    break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(result.isEmpty()) return null;
        return result.get(0);
    }
    /**
     * 异步post请求
     * 例如：请求的最终地址为：http://127.0.0.1:8081/user/getUser/123
     * @param url 基本请求地址   例子： http://127.0.0.1:8081
     * @param json 提交的json字符串
     * @param args 请求的参数    args[]=new String[]{"user","getUser","123"}
     * @return
     */

    public static String postAsyncRequest(String url,String json,String... args){
        List<String> result=new ArrayList<>();
        String address=url;
        for(int i=0;i<args.length;i++){
            address=address+"/"+args[i];
        }
        final String finalAddress = address;
        Log.d("异步post请求地址：",finalAddress);
        client=getInstance();
        RequestBody body = RequestBody.create(JSON, json);
//        FormBody.Builder formBody = new FormBody.Builder();//创建表单请求体
//        formBody.add("json",json);
        request = new Request.Builder()
                .url(finalAddress)
                .post(body)
                .addHeader("device-platform", "android")
                .build();
        Call call=client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Log.d("HttpUtil","异步post请求失败！");
//                    }
//                }).start();
                Log.d("HttpUtil","异步post请求失败！");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String res = null;
                        try {
                            res = response.body().string();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        result.add(res);
                        Log.d("HttpUtil","异步post请求成功！");
                        Log.d("请求对象",res);
                    }
                }).start();
            }
        });
        /**因为函数返回是立刻执行的，而result要在请求完成之后才能获得
         * 所以需要等待result获得返回值之后再执行return*/
        long startTime =  System.currentTimeMillis();

        /** 获取当前的系统时间，与初始时间相减就是程序运行的毫秒数，除以1000就是秒数*/

        while (result.size()==0  ){
            try {
                TimeUnit.MILLISECONDS.sleep(OnceTimeOut);//等待xx毫秒

                if((System.currentTimeMillis()-startTime)>SendTimeOut)
                    break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(result.isEmpty()) return null;
        return result.get(0);
    }
    public static String uploadFile(String url, File file, String... args){
        List<String> result=new ArrayList<>();
        String address=url;
        for(int i=0;i<args.length;i++){
            address=address+"/"+args[i];
        }

        final String finalAddress = address;
        Log.d("异步post请求地址：",finalAddress);
        client=getInstance();

        RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                       .addFormDataPart("file", file.getName(), RequestBody.create(file,MediaType.parse("multipart/form-data")))//文件
                       .build();


        request = new Request.Builder()
                .url(finalAddress)
                .post(requestBody)
                .addHeader("device-platform", "android")
                .build();
        Call call=client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("HttpUtil","异步post请求失败！");
                    }
                }).start();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String res = null;
                        try {
                            res = response.body().string();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        result.add(res);
                        Log.d("HttpUtil","异步post请求成功！");
                        Log.d("请求对象",res);
                    }
                }).start();
            }
        });
        /**因为函数返回是立刻执行的，而result要在请求完成之后才能获得
         * 所以需要等待result获得返回值之后再执行return*/
        long startTime =  System.currentTimeMillis();

        /** 获取当前的系统时间，与初始时间相减就是程序运行的毫秒数，除以1000就是秒数*/

        while (result.size()==0  ){
            try {
                TimeUnit.MILLISECONDS.sleep(OnceTimeOut);//等待xx毫秒

                if((System.currentTimeMillis()-startTime)>SendTimeOut)
                    break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(result.isEmpty()) return null;
        return result.get(0);
    }
    public static String postparamter(String url, Map<String, String> paramsMap,String... args){
        List<String> result=new ArrayList<>();
        String address=url;
        for(int i=0;i<args.length;i++){
            address=address+"/"+args[i];
        }

        final String finalAddress = address;
        Log.d("异步post请求地址：",finalAddress);
        client=getInstance();

        FormBody.Builder builder = new FormBody.Builder();//创建表单请求体
        for (String key : paramsMap.keySet()) {
            //追加表单信息
            builder.add(key, paramsMap.get(key));
        }

        RequestBody formBody=builder.build();
        request = new Request.Builder()
                .url(finalAddress)
                .post(formBody)
                .addHeader("device-platform", "android")
                .build();
        Call call=client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("HttpUtil","异步post请求失败！");
                    }
                }).start();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String res = null;
                        try {
                            res = response.body().string();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        result.add(res);
                        Log.d("HttpUtil","异步post请求成功！");
                        Log.d("请求对象",res);
                    }
                }).start();
            }
        });
        /**因为函数返回是立刻执行的，而result要在请求完成之后才能获得
         * 所以需要等待result获得返回值之后再执行return*/
        long startTime =  System.currentTimeMillis();

        /** 获取当前的系统时间，与初始时间相减就是程序运行的毫秒数，除以1000就是秒数*/

        while (result.size()==0  ){
            try {
                TimeUnit.MILLISECONDS.sleep(OnceTimeOut);//等待xx毫秒

                if((System.currentTimeMillis()-startTime)>SendTimeOut)
                    break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(result.isEmpty()) return null;
        return result.get(0);
    }

    public static String getAsyncRequest(String url,Map<String, String> paramsMap,String... args){
        List<String> result=new ArrayList<>();
        String address=url;
        for(int i=0;i<args.length;i++){
            address=address+"/"+args[i];
        }
        address = address+'?';
        for (String key : paramsMap.keySet()) {
            //追加表单信息
            address = address+ key+'='+paramsMap.get(key)+'&';
        }
        final String finalAddress = address;
        doAsyncGet(finalAddress, new OkHttpCallback() {
            @Override
            public void onFailure(IOException e) {
                Log.d("异步get请求地址：",finalAddress);
                Log.d("HttpUtil", "异步get请求失败！");
            }
            @Override
            public void onSuccess(Response response) {
                Log.d("异步get请求地址：",finalAddress);
                String res = null;
                try {
                    res = response.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                result.add(res);
                Log.d("HttpUtil", "异步get请求成功！");
                Log.d("请求对象：", res);
            }
        });
        /**因为函数返回是立刻执行的，而result要在请求完成之后才能获得
         * 所以需要等待result获得返回值之后再执行return*/
        long startTime =  System.currentTimeMillis();

        /** 获取当前的系统时间，与初始时间相减就是程序运行的毫秒数，除以1000就是秒数*/

        while (result.size()==0  ){
            try {
                TimeUnit.MILLISECONDS.sleep(OnceTimeOut);//等待xx毫秒

                if((System.currentTimeMillis()-startTime)>SendTimeOut)
                    break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(result.isEmpty()) return null;
        return result.get(0);
    }

    /**
     * 异步delete请求
     * 例如：请求的最终地址为：http://127.0.0.1:8081/user/getUser/123
     * @param url 基本请求地址   例子： http://127.0.0.1:8081
     * @param json 提交的json字符串
     * @param args 请求的参数    args[]=new String[]{"user","getUser","123"}
     * @return
     */
    public static String deleteAsyncRequest(String url,String json,String... args){
        List<String> result=new ArrayList<>();
        String address=url;
        for(int i=0;i<args.length;i++){
            address=address+"/"+args[i];
        }
        final String finalAddress = address;
        Log.d("异步delete请求地址：",finalAddress);
        client=getInstance();
        RequestBody body = RequestBody.create(JSON, json);
//        FormBody.Builder formBody = new FormBody.Builder();//创建表单请求体
//        formBody.add("json",json);
        request = new Request.Builder()
                .url(finalAddress)
                .delete(body)
                .addHeader("device-platform", "android")
                .build();
        Call call=client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("HttpUtil","异步delete请求失败！");
                    }
                }).start();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String res = null;
                        try {
                            res = response.body().string();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        result.add(res);
                        Log.d("HttpUtil","异步delete请求成功！");
                        Log.d("请求对象",res);
                    }
                }).start();
            }
        });
        long startTime =  System.currentTimeMillis();

        /** 获取当前的系统时间，与初始时间相减就是程序运行的毫秒数，除以1000就是秒数*/

        while (result.size()==0  ){
            try {
                TimeUnit.MILLISECONDS.sleep(OnceTimeOut);//等待xx毫秒

                if((System.currentTimeMillis()-startTime)>SendTimeOut)
                    break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(result.isEmpty()) return null;
        return result.get(0);
    }
    /**
     * 异步put请求
     * 例如：请求的最终地址为：http://127.0.0.1:8081/user/getUser/123
     * @param url 基本请求地址   例子： http://127.0.0.1:8081
     * @param json 提交的json字符串
     * @param args 请求的参数    args[]=new String[]{"user","getUser","123"}
     * @return
     */
    public static String putAsyncRequest(String url,String json,String... args) {
        List<String> result = new ArrayList<>();
        String address = url;
        for (int i = 0; i < args.length; i++) {
            address = address + "/" + args[i];
        }
        final String finalAddress = address;
        Log.d("异步put请求地址：", finalAddress);
        client = getInstance();
        RequestBody body = RequestBody.create(JSON, json);
//        FormBody.Builder formBody = new FormBody.Builder();//创建表单请求体
//        formBody.add("json",json);
        request = new Request.Builder()
                .url(finalAddress)
                .put(body)
                .addHeader("device-platform", "android")
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("HttpUtil", "异步put请求失败！");
                    }
                }).start();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String res = null;
                        try {
                            res = response.body().string();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        result.add(res);
                        Log.d("HttpUtil", "异步put请求成功！");
                        Log.d("请求对象", res);
                    }
                }).start();
            }
        });
        /**因为函数返回是立刻执行的，而result要在请求完成之后才能获得
         * 所以需要等待result获得返回值之后再执行return*/
        long startTime = System.currentTimeMillis();

        /** 获取当前的系统时间，与初始时间相减就是程序运行的毫秒数，除以1000就是秒数*/

        while (result.size() == 0) {
            try {
                TimeUnit.MILLISECONDS.sleep(OnceTimeOut);//等待xx毫秒

                if ((System.currentTimeMillis() - startTime) > SendTimeOut)
                    break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (result.isEmpty()) return null;
        return result.get(0);
    }

    public static String putparamter(String url, Map<String, String> paramsMap,String... args){
        List<String> result=new ArrayList<>();
        String address=url;
        for(int i=0;i<args.length;i++){
            address=address+"/"+args[i];
        }

        final String finalAddress = address;
        Log.d("异步post请求地址：",finalAddress);
        client=getInstance();

        FormBody.Builder builder = new FormBody.Builder();//创建表单请求体
        for (String key : paramsMap.keySet()) {
            //追加表单信息
            builder.add(key, paramsMap.get(key));
        }

        RequestBody formBody=builder.build();
        request = new Request.Builder()
                .url(finalAddress)
                .put(formBody)
                .addHeader("device-platform", "android")
                .build();
        Call call=client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("HttpUtil","异步post请求失败！");
                    }
                }).start();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String res = null;
                        try {
                            res = response.body().string();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        result.add(res);
                        Log.d("HttpUtil","异步post请求成功！");
                        Log.d("请求对象",res);
                    }
                }).start();
            }
        });
        /**因为函数返回是立刻执行的，而result要在请求完成之后才能获得
         * 所以需要等待result获得返回值之后再执行return*/
        long startTime =  System.currentTimeMillis();

        /** 获取当前的系统时间，与初始时间相减就是程序运行的毫秒数，除以1000就是秒数*/

        while (result.size()==0  ){
            try {
                TimeUnit.MILLISECONDS.sleep(OnceTimeOut);//等待xx毫秒

                if((System.currentTimeMillis()-startTime)>SendTimeOut)
                    break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(result.isEmpty()) return null;
        return result.get(0);
    }

}
