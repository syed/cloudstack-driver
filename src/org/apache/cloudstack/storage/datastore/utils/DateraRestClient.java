package org.apache.cloudstack.storage.datastore.utils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.cloudstack.storage.datastore.driver.DateraPrimaryDataStoreDriver;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

public class DateraRestClient {
    private static final Logger s_logger = Logger.getLogger(DateraPrimaryDataStoreDriver.class);

    private Gson gson = new GsonBuilder().create();
    private LoginResponse respLogin = new LoginResponse();
    private String managementIp;
 private int managementPort;
 private String userName;
 private String password;
 public final String defaultStorageName = "storage-1";
 public final String defaultVolumeName = "volume-1";

 public DateraRestClient(String ip, int port,String user, String pass)
 {
  managementIp = ip;
  managementPort = port;
  userName = user;
  password = pass;
  doLogin();
 }


 private class InitiatorGroup
 {
  public String name;
  public List<String> members = null;

  public InitiatorGroup(String paramName, List<String> paramMembers)
  {
   this.name = paramName;
   this.members = paramMembers;
  }
 }

 private class LoginModel
 {
  public String name;
  public String password;
 }

 public class ACLPolicyModel
 {
  public List<String> initiators; // e.g. "/initiators/iqn.1993-08.org.debian:01:6e3ca7bd74e1"
  @SerializedName("initiator_groups")
  public List<String> initiatorGroups; // e.g. "/initiator_groups/cluster2_initiator_group"

  public ACLPolicyModel(List<String> paramInitiators, List<String> paramInitiatorGroups)
  {
   initiators = paramInitiators;
   initiatorGroups = paramInitiatorGroups;
  }
 }
 public class AppModel
 {
  public String name;
  @SerializedName("access_control_mode")
  public String accessControlMode;
  @SerializedName("storage_instances")
  public StorageInstanceModel storageInstances;

  public AppModel(String appInstanceName, String accessControlMode, StorageInstanceModel inst)
  {
   name = appInstanceName;
   accessControlMode = accessControlMode;
   storageInstances = inst;
  }
 }

 public class StorageInstanceModel
 {
  @SerializedName(defaultStorageName)
  public StorageModel storage1;

  public StorageInstanceModel(StorageModel st)
  {
   storage1 = st;
  }
 }

 public class StorageModel
 {
  public String name = defaultStorageName;
  public VolumeInstanceModel volumes;
  @SerializedName("acl_policy")
  public ACLPolicyModel aclPolicy;
  @SerializedName("ip_pool")
  public String ipPool;
  public StorageModel(String networkPoolName, VolumeInstanceModel vols, ACLPolicyModel policy)
  {
   volumes = vols;
   aclPolicy = policy;
   ipPool = networkPoolName;
  }
 }

 public class VolumeInstanceModel
 {
  @SerializedName(defaultVolumeName)
  public VolumeModel volume1;

  public VolumeInstanceModel(VolumeModel vol)
  {
   volume1 = vol;
  }
 }
 public class VolumeModel {

  public String name = defaultVolumeName;
  public int size;
  @SerializedName("replica_count")
  public int replicaCount;
  //snapshot policies

  public VolumeModel(String volName,int volSize, int volReplicaSize)
  {
   name =  volName;
   size = volSize;
   replicaCount = volReplicaSize;
  }
 }

 public class StorageResponse
 {
  public class AccessControl
  {
   public List<String> ips;
   public String iqn;
  }

  public AccessControl access;

 }
 public class AdminPreviledge
 {
   @SerializedName("admin_state")
   public String adminState;

   public AdminPreviledge(String paramAdminState)
   {
    adminState = paramAdminState;
   }
 }

 public class GenericResponse
 {
  public String name;
  public String id;
 }

 public class VolumeResize
 {
     public int size;
     public VolumeResize(int sz)
     {
       size = sz;
     }
 }

 public class AppModelEx
 {
     public String name;

     public AppModelEx(String appName)
     {
       name = appName;
     }
 }

 public class StorageModelEx
 {
     public String name;
     @SerializedName("ip_pool")
     public String ipPool;

     public StorageModelEx(String storageName, String ipPool)
     {
       name = storageName;
       this.ipPool = ipPool;
     }
 }
 public class InitiatorModel
 {
    public String id;
    public String name;
   public InitiatorModel(String label, String iqn)
   {
     name = label;
     id = iqn;
   }
 }
   public AppInstanceInfo.VolumeInfo getVolumeInfo(String appInstance, String storageInstance, String volumeName)
   {
       String restPath = String.format("/v2/app_instances/%s/storage_instances/%s/volumes/%s", appInstance,storageInstance,volumeName);
       HttpGet getRequest = new HttpGet(restPath);
       getRequest.setHeader("auth-token",respLogin.getKey());
       String response = execute(getRequest);
       AppInstanceInfo.VolumeInfo resp = gson.fromJson(response, AppInstanceInfo.VolumeInfo.class);
       return resp;
   }
   public String createNextVolume(String appInstance, String storageInstance, int volSize)
   {
        List<AppInstanceInfo.VolumeInfo> volumes = getVolumes(appInstance, storageInstance);
        String volumeName = generateNextVolumeName(volumes,"volume");
        return createVolume(appInstance, storageInstance,volumeName,volSize) ? volumeName : "";
   }
   public List<AppInstanceInfo.VolumeInfo> getVolumes(String appInstance, String storageInstance) {
       String restPath = String.format("/v2/app_instances/%s/storage_instances/%s/volumes", appInstance,storageInstance);
       HttpGet getRequest = new HttpGet(restPath);
       getRequest.setHeader("auth-token",respLogin.getKey());
       String response = execute(getRequest);
       return getVolumeList(response);

  }
 public boolean updateStorageWithInitiator(String appInstance, String storageInstance, List<String> initiators)
 {
      //setAdminState(appInstance, false);
      StorageInitiator storage = new StorageInitiator(initiators,null);

      HttpPut putRequest = new HttpPut("/v2/app_instances/"+appInstance+"/storage_instances/"+storageInstance);
      putRequest.setHeader("Content-Type","application/json");
      putRequest.setHeader("auth-token",respLogin.getKey());
      String payload = gson.toJson(storage);

      setPayload(putRequest, payload);

      String response = execute(putRequest);
      GenericResponse respObj = gson.fromJson(response, GenericResponse.class);
      return respObj.name.equals(storageInstance) ? true : false;

 }
 public boolean unregisterInitiator(String iqn)
 {
    HttpDelete deleteRequest = new HttpDelete("/v2/initiators/"+iqn);
    deleteRequest.setHeader("Content-Type","application/json");
    deleteRequest.setHeader("auth-token",respLogin.getKey());
    String response = execute(deleteRequest);
    s_logger.info("DateraRestClient.unregisterInitiator response ="+response);
    GenericResponse resp = gson.fromJson(response, GenericResponse.class);

    return resp.id.equals(iqn) ? true : false;
 }
 public boolean registerInitiator(String labelName, String iqn)
 {
    HttpPost postRequest = new HttpPost("/v2/initiators");
    postRequest.setHeader("Content-Type","application/json");
    postRequest.setHeader("auth-token",respLogin.getKey());

    InitiatorModel initiator = new InitiatorModel(labelName, iqn);
    String payload = gson.toJson(initiator);
    setPayload(postRequest, payload);
    String response = execute(postRequest);
    s_logger.info("DateraRestClient.registerInitiator response ="+response);
    GenericResponse resp = gson.fromJson(response, GenericResponse.class);

    return resp.name.equals(labelName) ? true : false;
 }
   public boolean isAppInstanceExists(String appName)
   {
      HttpGet getRequest = new HttpGet("/v2/app_instances/"+appName);
      getRequest.setHeader("Content-Type","application/json");
      getRequest.setHeader("auth-token",respLogin.getKey());
      String response = execute(getRequest);
      GenericResponse resp = gson.fromJson(response, GenericResponse.class);

      return resp.name.equals(appName) ? true : false;
   }

 public boolean createVolume(String appName, String storageInstance, String volName, int volSize)
 {
     HttpPost postRequest = new HttpPost("/v2/app_instances/"+appName+"/storage_instances/"+storageInstance+"/volumes");
     postRequest.setHeader("Content-Type","application/json");
     postRequest.setHeader("auth-token",respLogin.getKey());
     VolumeModel vol = new VolumeModel(volName,volSize,3);
     String payload = gson.toJson(vol);
     setPayload(postRequest,payload);
     String response = execute(postRequest);
     GenericResponse resp = gson.fromJson(response, GenericResponse.class);

     return resp.name.equals(volName) ? true : false;
 }
 public boolean createStorageInstance(String appName, String storageInstance, String networkPoolName)
 {
    HttpPost postRequest = new HttpPost("/v2/app_instances/"+appName+"/storage_instances");
    postRequest.setHeader("Content-Type","application/json");
    postRequest.setHeader("auth-token",respLogin.getKey());
    networkPoolName = "/access_network_ip_pools/"+networkPoolName;
    StorageModelEx storage = new StorageModelEx(storageInstance,networkPoolName);
    String payload = gson.toJson(storage);
    setPayload(postRequest,payload);
    String response = execute(postRequest);
    GenericResponse resp = gson.fromJson(response, GenericResponse.class);

    return resp.name.equals(appName) ? true : false;
 }
 public boolean createAppInstance(String appName)
 {
    HttpPost postRequest = new HttpPost("/v2/app_instances");
    postRequest.setHeader("Content-Type","application/json");
    postRequest.setHeader("auth-token",respLogin.getKey());
    AppModelEx app = new AppModelEx(appName);
    String payload = gson.toJson(app);
    setPayload(postRequest,payload);
    String response = execute(postRequest);
    GenericResponse resp = gson.fromJson(response, GenericResponse.class);

    return resp.name.equals(appName) ? true : false;
 }
 public boolean setAdminState(String appInstance,boolean online)
 {
  boolean ret = false;
  AdminPreviledge prev = new AdminPreviledge( online ? "online" : "offline");

  HttpPut putRequest = new HttpPut("/v2/app_instances/"+appInstance);
  putRequest.setHeader("Content-Type","application/json");
  putRequest.setHeader("auth-token",respLogin.getKey());
  String payload = gson.toJson(prev);

   setPayload(putRequest, payload);

  String response = execute(putRequest);
  GenericResponse respObj = gson.fromJson(response, GenericResponse.class);
  return respObj.name.equals(appInstance) ? true : false;
 }
private void setPayload(HttpPut request, String payload) {
    try {
         StringEntity params = new StringEntity(payload);
         request.setEntity(params);
        } catch (UnsupportedEncodingException e) {
         e.printStackTrace();
       }
}

 public boolean resizeVolume(String appInstance, String storageInstance, String volumeInstance, int newSize)
 {
    String restPath = String.format("/v2/app_instances/%s/storage_instances/%s/volumes/%s", appInstance,storageInstance,volumeInstance);
    HttpPut putRequest = new HttpPut(restPath);
    putRequest.setHeader("Content-Type","application/json");
    putRequest.setHeader("auth-token",respLogin.getKey());

    VolumeResize vol = new VolumeResize(newSize);
    String payload = gson.toJson(vol);

    setPayload(putRequest, payload);
     String response = execute(putRequest);
     GenericResponse resp = gson.fromJson(response,GenericResponse.class);

     return resp.name.equals(volumeInstance) ? true : false;
 }

 public boolean deleteAppInstance(String appInstance)
 {
  HttpDelete deleteRequest = new HttpDelete("/v2/app_instances/"+appInstance);
  deleteRequest.setHeader("auth-token",respLogin.getKey());
  String response = execute(deleteRequest);

  GenericResponse respObj = gson.fromJson(response, GenericResponse.class);
  return respObj.name.equals(appInstance) ? true : false;
 }
 public boolean deleteVolume(String appInstance, String storageInstance, String volumeInstance)
 {
  boolean ret = false;
  String restPath = String.format("/v2/app_instances/%s/storage_instances/%s/volumes/%s", appInstance,storageInstance,volumeInstance);

  HttpDelete deleteRequest = new HttpDelete(restPath);
  deleteRequest.setHeader("auth-token",respLogin.getKey());
  String response = execute(deleteRequest);

  GenericResponse respObj = gson.fromJson(response, GenericResponse.class);
  return respObj.name.equals(volumeInstance) ? true : false;

 }
 public AppInstanceInfo.StorageInstance getStorageInfo(String appInstance, String storageInstance)
 {
  String restPath = String.format("/v2/app_instances/%s/storage_instances/%s", appInstance,storageInstance);
  HttpGet getRequest = new HttpGet(restPath);
        getRequest.setHeader("auth-token",respLogin.getKey());
        String response = execute(getRequest);

        AppInstanceInfo.StorageInstance storageInfo = gson.fromJson(response, AppInstanceInfo.StorageInstance.class);

        return storageInfo;
 }

 public void getInitiatorGroup()
 {

 }

 public void createInitiatorGroup(String name, List<String> initiators)
 {
  HttpPost postRequest = new HttpPost("/v2/initiator_groups");
  postRequest.setHeader("Content-Type","application/json");
  postRequest.setHeader("auth-token",respLogin.getKey());

  InitiatorGroup intrGroup = new InitiatorGroup(name,initiators);
  String payload = gson.toJson(intrGroup);

  setPayload(postRequest, payload);
  execute(postRequest);
 }
private void setPayload(HttpPost request, String payload) {
   try {
         StringEntity params = new StringEntity(payload);
          request.setEntity(params);
        } catch (UnsupportedEncodingException e) {

       e.printStackTrace();
       }
}

 public void getInitiators()
 {
       HttpGet getRequest = new HttpGet("/v2/initiators");
       getRequest.setHeader("auth-token",respLogin.getKey());
       execute(getRequest);
 }
 public AppInstanceInfo createVolume(String appInstanceName, List<String> initiators, List<String> initiatorGroups,int volumeGB, int volReplica, String accessControlMode, String networkPoolName)
 {
       HttpPost postRequest = new HttpPost("/v2/app_instances");
       postRequest.setHeader("Content-type","application/json");
       postRequest.setHeader("auth-token",respLogin.getKey());

       networkPoolName = "/access_network_ip_pools/"+networkPoolName;
       String payload = generateVolumePayload(appInstanceName,initiators,initiatorGroups,volumeGB,volReplica,accessControlMode,networkPoolName);

       s_logger.info("DateraRestClient.createVolume payload ="+ payload);
       setPayload(postRequest, payload);

       String response = execute(postRequest);

       s_logger.info("DateraRestClient.createVolume response ="+ response);
       AppInstanceInfo resp = gson.fromJson(response, AppInstanceInfo.class);

       return resp;
 }

 public String generateVolumePayload(String appInstanceName,
   List<String> initiators, List<String> initiatorGroups, int volumeGB, int volReplica, String accessControlMode, String networkPoolName) {
  // TODO Auto-generated method stub
  String payload = "";

  AppModel app = new AppModel(appInstanceName,accessControlMode,
    new StorageInstanceModel(
      new StorageModel(networkPoolName,
        new VolumeInstanceModel(
          new VolumeModel(defaultVolumeName, volumeGB, volReplica)),new ACLPolicyModel(initiators,initiatorGroups))));

  payload = gson.toJson(app);

  System.out.println(payload);
  return payload;
 }
 private void doLogin()
 {
     HttpPut postRequest = new HttpPut("/v2/login");
     postRequest.setHeader("Content-Type","application/json");
     LoginModel loginInfo = new LoginModel();
     loginInfo.name = userName;
     loginInfo.password = password;

  try {
   StringEntity params = new StringEntity(gson.toJson(loginInfo));
        postRequest.setEntity(params);
  } catch (UnsupportedEncodingException e) {
   // TODO Auto-generated catch block
   e.printStackTrace();
  }

  String resp = execute(postRequest);
  s_logger.info("DateraRestClient.doLogin response ="+resp);
  if(null != resp)
  {
   respLogin = gson.fromJson(resp, LoginResponse.class);
  }

  if(null == respLogin || null == respLogin.getKey())
  {
     throw new CloudRuntimeException("Could not login to datera node");
  }
  //System.out.println("The session key :"+respLogin.getKey());

 }
 private String execute(HttpRequest request)
 {
  DefaultHttpClient httpclient = new DefaultHttpClient();
  String resp = "";
 try {

  httpclient = getHttpClient(managementPort);

        AuthScope authScope = new AuthScope(managementIp, managementPort, AuthScope.ANY_SCHEME);
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(userName, password);

        httpclient.getCredentialsProvider().setCredentials(authScope, credentials);

 // specify the host, protocol, and port
 HttpHost target = new HttpHost(managementIp, managementPort, "https");

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

      resp = EntityUtils.toString(entity);

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

 return resp;
 }

    private DefaultHttpClient getHttpClient(int iPort) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLSv1"); //SSLUtils.getSSLContext();
            X509TrustManager tm = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            sslContext.init(null, new TrustManager[] {tm}, new SecureRandom());

            SSLSocketFactory socketFactory = new SSLSocketFactory(sslContext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            SchemeRegistry registry = new SchemeRegistry();

            registry.register(new Scheme("https", iPort, socketFactory));

            BasicClientConnectionManager mgr = new BasicClientConnectionManager(registry);
            DefaultHttpClient client = new DefaultHttpClient();

            return new DefaultHttpClient(mgr, client.getParams());
        } catch (NoSuchAlgorithmException ex) {
            //throw new CloudRuntimeException(ex.getMessage());
        } catch (KeyManagementException ex) {
            //throw new CloudRuntimeException(ex.getMessage());
        }
        return null;
    }

    private List<AppInstanceInfo.VolumeInfo> getVolumeList(String volumesJson)
    {
        List<AppInstanceInfo.VolumeInfo> volumes = new ArrayList<AppInstanceInfo.VolumeInfo>();
        try
        {
            GsonBuilder gsonBuilder = new GsonBuilder();
            Type mapStringObjectType = new TypeToken<Map<String, Object>>() {}.getType();
            gsonBuilder.registerTypeAdapter(mapStringObjectType, new DateraMapKeysAdapter());
            Gson gson1 = gsonBuilder.create();

            Map<String, Object> map = gson1.fromJson(volumesJson, mapStringObjectType);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String volJson = entry.getValue().toString();
                AppInstanceInfo.VolumeInfo vol = gson1.fromJson(volJson, AppInstanceInfo.VolumeInfo.class);
                volumes.add(vol);
          }
        }
        catch(Exception ex)
        {
            throw new RuntimeException("Datera : unable to deserialize volume list. "+ex);
        }
       return volumes;
    }
    public String generateNextVolumeName(List<AppInstanceInfo.VolumeInfo> volumes, String filter)
    {
       List<String> names = new ArrayList<String>();
       for(AppInstanceInfo.VolumeInfo vol : volumes)
       {
           names.add(vol.name);
       }
       return generateName(names,filter);
    }
    public String generateName(List<String> nameList, String filter) {
        int maxInt = 0;
        if (nameList != null && !nameList.isEmpty()) {
            List<Integer> list=new ArrayList<>();
            /*maxInt = nameList.parallelStream().mapToInt(name -> getIntPart(name)).max().getAsInt() + 1;*/
            for (String name : nameList) {
                list.add(getIntPart(name));
            }
            Collections.sort(list);
            maxInt=list.get(list.size()-1)+1;
        }
        return filter + "-" + maxInt;
    }

    public int getIntPart(String name) {
        try {
            if (name.split("-").length > 1)
                return Integer.parseInt(name.split("-")[1]);
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
