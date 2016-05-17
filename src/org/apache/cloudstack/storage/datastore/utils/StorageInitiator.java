package org.apache.cloudstack.storage.datastore.utils;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class StorageInitiator
{
   @SerializedName("acl_policy")
   public ACLPolicy aclPolicy;
   public StorageInitiator(List<String> initiators,List<String> initiatorsGroup)
   {
      aclPolicy = new ACLPolicy(initiators,initiatorsGroup);
   }
}
