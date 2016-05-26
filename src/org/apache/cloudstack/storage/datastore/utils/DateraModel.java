package org.apache.cloudstack.storage.datastore.utils;

import java.util.List;

public class DateraModel {

     public static class InitiatorGroup
     {
      public String name;
      public List<String> members = null;

      public InitiatorGroup(String paramName, List<String> paramMembers)
      {
       this.name = paramName;
       this.members = paramMembers;
      }
     }
}
