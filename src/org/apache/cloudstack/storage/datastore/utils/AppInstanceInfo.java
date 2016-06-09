package org.apache.cloudstack.storage.datastore.utils;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class AppInstanceInfo {

 @SerializedName("storage_instances")
 public StorageInstances storageInstances;

 public String path;
 public String name;
 public String descr;
 @SerializedName("create_mode")
 public String createMode;
 public String uuid;
 public String health;
 @SerializedName("admin_state")
 public String adminState;

 public class StorageInstances
 {
  @SerializedName("storage-1")
  public StorageInstance storage1;
 }

 public class StorageInstance
 {
  public String path;
  public VolumeInstances volumes;
  @SerializedName("active_initiators")
  public List<String> activeInitiators;
  @SerializedName("active_storage_nodes")
  public List<String> activeStorageNodes;
  public AccessInfo access;
  public String name;
  public String uuid;
  @SerializedName("acl_policy")
  public ACLPolicy aclPolicy;
  @SerializedName("admin_state")
  public String adminState;
  @SerializedName("access_control_mode")
  public String accessControlMode;
  @SerializedName("ip_pool")
  public String ipPool;
  public StorageAuthentication auth;
  @SerializedName("op_state")
  public String opState;
  @SerializedName("creation_type")
  public String creationType;
 }

 public class StorageAuthentication
 {
     public String path;
     public String type;
     @SerializedName("target_user_name")
     public String targetUserName;
     @SerializedName("target_pswd")
     public String targetPswd;
     @SerializedName("initiator_user_name")
     public String initiatorUserName;
     @SerializedName("initiator_pswd")
     public String initiatorPswd;
 }

 public class AccessInfo
 {
  public List<String> ips;
  public String iqn;
 }

 public class VolumeInstances
 {
  @SerializedName("volume-1")
  public VolumeInfo volume1;
 }

 public class VolumeInfo
 {
  public String path;
  public String name;
  public String uuid;
  @SerializedName("op_state")
  public String opState;
  @SerializedName("replica_count")
  public int replicaCount;
  public int size;
  @SerializedName("capacity_in_use")
  public long capacityInUse;
 }
}
