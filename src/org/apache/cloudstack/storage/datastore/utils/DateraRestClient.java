package org.apache.cloudstack.storage.datastore.utils;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DateraRestClient {

 private Gson gson = new GsonBuilder().create();
 private LoginResponse respLogin = new LoginResponse();
 private String managementIp;
 private int managementPort;
 private String userName;
 private String password;

 public DateraRestClient(String ip, int port,String user, String pass)
 {
  managementIp = ip;
  managementPort = port;
  userName = user;
  password = pass;
 }

 public void getInitiators()
 {
       HttpGet getRequest = new HttpGet("/v2/initiators");
       getRequest.setHeader("auth-token",respLogin.getKey());
       execute(getRequest,null);
 }
 public void createVolume()
 {
  DateFormat df = new SimpleDateFormat("dd-MM-yy-HH-mm-ss");
  Date dateobj = new Date();
  System.out.println(df.format(dateobj));
  String appName = "App-"+ df.format(dateobj).toString();

       HttpPost postRequest = new HttpPost("/v2/app_instances");
       postRequest.setHeader("Content-type","application/json");
       postRequest.setHeader("auth-token",respLogin.getKey());

       String payload = "{"
         + "\"name\": \""+appName+"\","
         + "\"access_control_mode\": \"deny_all\","
         + " \"storage_instances\": {"
         + "\"storage-1\": {"
         + " \"name\": \"storage-1\","
         + "\"volumes\": {"
         + " \"volume-1\": {"
         + "\"name\": \"volume-1\","
         + " \"size\": 2,"
         + " \"replica_count\": 3,"
         + "\"snapshot_policies\": {}"
         + "}"
         + "}"
         + "}"
         + "}"
         + "}";



  try {
   StringEntity params = new StringEntity(payload);
        postRequest.setEntity(params);
  } catch (UnsupportedEncodingException e) {
   // TODO Auto-generated catch block
   e.printStackTrace();
  }

       execute(postRequest,null);

 }

 public void doLogin()
 {
     HttpPut postRequest = new HttpPut("/v2/login");
     postRequest.setHeader("Content-Type","application/json");

  try {
   StringEntity params = new StringEntity("{\"name\":\"admin\",\"password\":\"password\"}");
        postRequest.setEntity(params);
  } catch (UnsupportedEncodingException e) {
   // TODO Auto-generated catch block
   e.printStackTrace();
  }

  respLogin = (LoginResponse)execute(postRequest,respLogin);

  System.out.println("The session key :"+respLogin.getKey());

 }
 private Object execute(HttpRequest request, Object response)
 {
  DefaultHttpClient httpclient = new DefaultHttpClient();
 try {
 // specify the host, protocol, and port
 HttpHost target = new HttpHost(managementIp, managementPort, "http");

      // specify the get request
      System.out.println("executing request to " + target);

      HttpResponse httpResponse = httpclient.execute(target, request);
      HttpEntity entity = httpResponse.getEntity();

      System.out.println("----------------------------------------");
      System.out.println(httpResponse.getStatusLine());
      Header[] headers = httpResponse.getAllHeaders();
      for (int i = 0; i < headers.length; i++) {
        System.out.println(headers[i]);
      }
      System.out.println("----------------------------------------");

      String resp = EntityUtils.toString(entity);
      try
      {
       if(null != response)
       {
        response = gson.fromJson(resp,  response.getClass());
       }
      }
      catch(Exception e)
      {
       e.printStackTrace();
      }

      if (entity != null) {
        System.out.println(resp);
      }

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      // When HttpClient instance is no longer needed,
      // shut down the connection manager to ensure
      // immediate deallocation of all system resources
      httpclient.getConnectionManager().shutdown();
    }

 return response;
 }

}
