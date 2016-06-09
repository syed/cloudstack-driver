package org.apache.cloudstack.storage.datastore.utils;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class ACLPolicy
{
   public String path;
   public List<String> initiators;
   @SerializedName("initiator_groups")
   public List<String> initiatorGroups;
   public ACLPolicy(List<String> paramInitiators,List<String> paramInitiatorsGroup)
   {
      initiators = paramInitiators;
      initiatorGroups = paramInitiatorsGroup;
   }
}