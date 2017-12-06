package com.muxistudio.testhuashiapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by kolibreath on 17-12-1.
 */
public class MainActivity extends AppCompatActivity {
   // private
    private String valueOfLt,valueOfExe;
    private String JSESSIONID_LOGIN_IN = null;
    CookieJar cookieJar1 = new CookieJar() {
        List<Cookie> cookies;
        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            this.cookies = cookies;
            for (int i = 0; i < cookies.size(); i++) {
              if(cookies.get(i).name().equals("JSESSIONID")){
                  JSESSIONID_LOGIN_IN = cookies.get(i).value();
                  break;
              }
            }
        }
        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            if (cookies != null)
                return cookies;
            return new ArrayList<Cookie>();
        }
    };
    CookieJar cookieJar2 = new CookieJar() {
        List<Cookie> cookies = new ArrayList<>();
        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            this.cookies.addAll(cookies);
        }
        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
                return cookies;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        login1();
    }
    private void login1() {
        LoginTask task = new LoginTask();
        task.execute();
    }
    private void educationSystemLogin(){
        EducationSystemLoginTask task = new EducationSystemLoginTask();
        task.execute();
    }

    //需要在第一步中获取ltvalue 和 execucationid等信息
    class LoginTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            OkHttpClient firstLoginClient = new OkHttpClient.Builder()
                    .cookieJar(cookieJar1)
                    .build();

            Request firstRequest = initRequestBuilder()
                    .url("https://account.ccnu.edu.cn/cas/login")
                    .get()
                    .build();

            firstLoginClient.newCall(firstRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {}
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String str1= "<input type=\"hidden\" name=\"lt\" value=\"(.*)\" />";
                    String str2= "<input type=\"hidden\" name=\"execution\" value=\"(.*)\" />";
                    //<input type="hidden" name="lt" value="LT-31315-O4Nt1gZeHUSnmzr4DALQwyn3xNyir6-account.ccnu.edu.cn" />
                    //<input type="hidden" name="execution" value="e1s1" />
                    String bigString = response.body().string();
                    Pattern r = Pattern.compile(str1);
                    Matcher m = r.matcher(bigString);
                    Pattern r2 = Pattern.compile(str2);
                    Matcher m2 = r2.matcher(bigString);

                    String keyLine1 = null;
                    String keyLine2 = null;

                    while(m.find()){
                        keyLine1 = m.group();
                    }
                    while(m2.find()){
                        keyLine2 = m2.group();
                    }
                    valueOfLt = keyLine1.split("value=\"")[1].split("\" />")[0];
                    valueOfExe = keyLine2.split("value=\"")[1].split("\" />")[0];

                    FinalLoginTask finalLoginTask = new FinalLoginTask();
                    finalLoginTask.execute();

                }
            });

            return null;
        }
    }
    class FinalLoginTask extends AsyncTask<Void,Void,Void>{
        @Override
        protected Void doInBackground(Void... voids) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                    .cookieJar(cookieJar2)
                    .build();

            RequestBody body = new FormBody.Builder()
                    .add("username","2016210897")
                    .add("password","szypride")
                    .add("lt",valueOfLt)
                    .add("execution",valueOfExe)
                    .add("_eventId","submit")
                    .add    ("submit","LOGIN")
                    .build();

            final Request request = initRequestBuilder()
                    .url("https://account.ccnu.edu.cn/cas/login;jsessionid="+JSESSIONID_LOGIN_IN)
                    .addHeader("host", "account.ccnu.edu.cn")
                    .addHeader("cookie", "JSESSIONID_LOGIN_IN="+JSESSIONID_LOGIN_IN)
                    .post(body)
                    .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        educationSystemLogin();
                    }
                });
            return null;
        }
    }
    class EducationSystemLoginTask extends AsyncTask<Void,Void,Void>{
        //直接登录，每一次的时候回去调用拦截器里面的逻辑
        @Override
        protected Void doInBackground(Void... voids) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                    .cookieJar(cookieJar2)
                    //  .followRedirects(false)
                    .build();
            Request requestOfFirstStep = initRequestBuilder()
                    .get()
                    .url("http://xk.ccnu.edu.cn/ssoserver/login?ywxt=jw&url=xtgl/index_initMenu.html")
                    .addHeader("host","xk.ccnu.edu.cn")
                    .build();
            {
                try {
                    client.newCall(requestOfFirstStep).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
    //提取header的公用字段
    private Request.Builder initRequestBuilder(){
        return  new Request.Builder()
                .addHeader("accept", "text/html,application/xhtml+xml" +
                        ",application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                .addHeader("accept-encoding", "gzip, deflate, br")
                .addHeader("accept-language", "en,zh-CN;q=0.9,zh;q=0.8")
                .addHeader("cache-control", "no-cache")
                .addHeader("connection", "keep-alive")
                .addHeader("pragma", "no-cache")
                .addHeader("upgrade-insecure-requests", "1")
                .addHeader( "user-agent",
                        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.75 Safari/537.36")
                ;

    }

}




