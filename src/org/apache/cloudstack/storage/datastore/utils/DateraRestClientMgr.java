package org.apache.cloudstack.storage.datastore.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;

public class DateraRestClientMgr {
    private static final Logger s_logger = Logger.getLogger(DateraRestClientMgr.class);

    private static DateraRestClientMgr instance = null;
    public static DateraRestClientMgr getInstance()
    {
        if(null == instance)
        {
            instance = new DateraRestClientMgr();
        }
        return instance;
    }
    private DateraRestClientMgr(){}
    public AppInstanceInfo.StorageInstance createVolume(DateraRestClient rest, String managementIP, int managementPort, String managementUsername, String managementPassword, String appInstanceName, String networkPoolName, long capacityBytes, int replica, long totalIOPS)
    {
        boolean ret = false;
        if(null == rest)
        {
          rest = new DateraRestClient(managementIP, managementPort, managementUsername, managementPassword);
        }
        if(rest.isAppInstanceExists(appInstanceName))
             throw new CloudRuntimeException("App name already exists : "+appInstanceName);

        if(false == rest.enumerateNetworkPool().contains(networkPoolName))
            throw new CloudRuntimeException("Network pool does not exists : "+networkPoolName);

        String storageInstanceName = rest.defaultStorageName;
        String volumeInstanceName = rest.defaultVolumeName;
        String accessControlMode = DateraRestClient.ACCESS_CONTROL_MODE_ALLOW_ALL;
        int dtVolSize = DateraUtil.getVolumeSizeInGB(capacityBytes);
        rest.createVolume(appInstanceName, null, null, dtVolSize, replica, accessControlMode, networkPoolName);
        rest.setQos(appInstanceName, storageInstanceName, volumeInstanceName, totalIOPS);

        AppInstanceInfo.VolumeInfo volInfo = rest.getVolumeInfo(appInstanceName, storageInstanceName, volumeInstanceName);
        boolean volumeCreationSuccess = true;
        String err = "";
        if(false == volInfo.name.equals(volumeInstanceName))
        {
           err = String.format("Could not create volume /%s/%s/%s ",appInstanceName,storageInstanceName,volumeInstanceName);
           volumeCreationSuccess = false;
        }
        else if(0 != volInfo.opState.compareTo(DateraRestClient.OP_STATE_AVAILABLE))
        {
            err = String.format("Volume's  opstate = %s /%s/%s/%s ",volInfo.opState,appInstanceName,storageInstanceName,volumeInstanceName);
            volumeCreationSuccess = false;
        }
        if(false == volumeCreationSuccess)
        {
            rest.setAdminState(appInstanceName, false);
            rest.deleteAppInstance(appInstanceName);
            throw new CloudRuntimeException(err);
        }

        return rest.getStorageInfo(appInstanceName, storageInstanceName);
    }

    public void registerInitiators(DateraRestClient rest, String managementIP,
            int managementPort, String managementUsername,
            String managementPassword, String appInstanceName,
            String storageInstanceName, String initiatorGroupName,
            Map<String, String> initiators, long timeout) {
        if(null == rest)
        {
            rest = new DateraRestClient(managementIP, managementPort, managementUsername, managementPassword);
        }
        rest.registerInitiators(initiators);
        List<String> listIqns = new ArrayList<String>(initiators.values());
        rest.createInitiatorGroup(initiatorGroupName, listIqns);
        List<String> initiatorGroups = new ArrayList<String>();
        initiatorGroups.add(initiatorGroupName);
        if(false == rest.updateStorageWithInitiator(appInstanceName, storageInstanceName, null, initiatorGroups))
        {
            throw new CloudRuntimeException("Could not update storage with initiator ,"+appInstanceName+", "+storageInstanceName);
        }

        try {
            s_logger.info("Waiting for the datera to setup everything , "+timeout);
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public boolean deleteAppInstance(DateraRestClient rest,
        DateraUtil.DateraMetaData dtMetaData) {
        if(null == rest)
        {
            rest = new DateraRestClient(dtMetaData.mangementIP, dtMetaData.managementPort, dtMetaData.managementUserName, dtMetaData.managementPassword);
        }
        rest.setAdminState(dtMetaData.appInstanceName, false);
        return rest.deleteAppInstance(dtMetaData.appInstanceName);
    }

    public boolean deleteInitiatorGroup(DateraRestClient rest,DateraUtil.DateraMetaData dtMetaData)
    {
        if(null == rest)
        {
            rest = new DateraRestClient(dtMetaData.mangementIP, dtMetaData.managementPort, dtMetaData.managementUserName, dtMetaData.managementPassword);
        }
        return rest.deleteInitiatorGroup(dtMetaData.volumeGroupName);
    }
    public boolean deleteAppInstanceAndInitiatorGroup(DateraUtil.DateraMetaData dtMetaData)
    {
        DateraRestClient rest = null;
        deleteAppInstance(rest,dtMetaData);
        return deleteInitiatorGroup(rest,dtMetaData);
    }

    public boolean updatePrimaryStorageCapacityBytes(DateraRestClient rest,DateraUtil.DateraMetaData dtMetaData, long capacityBytes)
    {
        boolean ret = false;
        if(null == rest)
        {
            rest = new DateraRestClient(dtMetaData.mangementIP, dtMetaData.managementPort, dtMetaData.managementUserName, dtMetaData.managementPassword);
        }
        int dtVolumeSize = getDateraCompatibleVolumeInGB(capacityBytes);
        rest.setAdminState(dtMetaData.appInstanceName, false);
        ret = rest.resizeVolume(dtMetaData.appInstanceName, dtMetaData.storageInstanceName, rest.defaultVolumeName, dtVolumeSize);
        rest.setAdminState(dtMetaData.appInstanceName, true);
        return ret;
    }
    public boolean updatePrimaryStorageIOPS(DateraRestClient rest,DateraUtil.DateraMetaData dtMetaData, long capacityIops)
    {
        boolean ret = false;
        if(null == rest)
        {
            rest = new DateraRestClient(dtMetaData.mangementIP, dtMetaData.managementPort, dtMetaData.managementUserName, dtMetaData.managementPassword);
        }
        ret = rest.updateQos(dtMetaData.appInstanceName, dtMetaData.storageInstanceName, rest.defaultVolumeName, capacityIops);
        return ret;
    }

    public AppInstanceInfo.StorageInstance getStorageInfo(DateraRestClient rest, DateraUtil.DateraMetaData dtMetaData)
    {
        if(null == rest)
        {
            rest = new DateraRestClient(dtMetaData.mangementIP, dtMetaData.managementPort, dtMetaData.managementUserName, dtMetaData.managementPassword);
        }
        return rest.getStorageInfo(dtMetaData.appInstanceName, dtMetaData.storageInstanceName);
    }
    public long getCloudstackCompatibleVolumeSize(int volSize)
    {
         return DateraUtil.getVolumeSizeInBytes(volSize);
    }
    public int getDateraCompatibleVolumeInGB(long capacityBytes)
    {
        capacityBytes = DateraUtil.getVolumeSizeInBytes((long)DateraUtil.getVolumeSizeInGB(capacityBytes));
        return DateraUtil.getVolumeSizeInGB(capacityBytes);
    }
    public DateraModel.AppModel getAppInstanceInfo(DateraRestClient rest,DateraUtil.DateraMetaData dtMetaData)
    {
        if(null == rest)
        {
            rest = new DateraRestClient(dtMetaData.mangementIP, dtMetaData.managementPort, dtMetaData.managementUserName, dtMetaData.managementPassword);
        }
        return rest.getAppInstanceInfo(dtMetaData.appInstanceName);
    }
    public DateraModel.PerformancePolicy getQos(DateraRestClient rest,DateraUtil.DateraMetaData dtMetaData)
    {
        if(null == rest)
        {
            rest = new DateraRestClient(dtMetaData.mangementIP, dtMetaData.managementPort, dtMetaData.managementUserName, dtMetaData.managementPassword);
        }
        return rest.getQos(dtMetaData.appInstanceName, dtMetaData.storageInstanceName, rest.defaultVolumeName);
    }
    public String suggestAppInstanceName(DateraRestClient rest, DateraUtil.DateraMetaData dtMetaData, String suggestedAppName)
    {
       String appName = "";
       if(null != suggestedAppName)
       {
           appName = suggestedAppName;
       }
       else
       {
           appName = "csApp";
       }
       if(null == rest)
       {
           rest = new DateraRestClient(dtMetaData.mangementIP, dtMetaData.managementPort, dtMetaData.managementUserName, dtMetaData.managementPassword);
       }
       List<String> appNames = rest.enumerateAppInstances();
       int counter = 1;
       for(;;)
       {
           if(false == appNames.contains(appName))
           {
               break;
           }
           else
           {
               appName+=counter++;
           }
       }
       return appName;
    }
}
