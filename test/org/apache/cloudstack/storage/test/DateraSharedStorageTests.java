package org.apache.cloudstack.storage.test;

import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.cloudstack.storage.datastore.utils.AppInstanceInfo;
import org.apache.cloudstack.storage.datastore.utils.DateraRestClient;
import org.apache.cloudstack.storage.datastore.utils.DateraUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DateraSharedStorageTests {

    @Test
    public void utValidateNetworkPoolName()
    {
        
        DateraRestClient rest = new DateraRestClient(DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD);
        List<String> poolNames = rest.enumerateNetworkPool();
        assertTrue(poolNames.size()>0);
        assertTrue(poolNames.contains("default"));//for the default storage pool name
        
    }

    @Test
    public void utRegisterInitiators()
    {
    	
        Map<String,String> initiatorsMap = new HashMap<String,String>();
        DateraRestClient rest = new DateraRestClient(DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD);
        //name -- Must be a combination of letters, numbers, spaces, underscores, and dashes between 3 and 128 characters; must start with either a letter or underscore"
        initiatorsMap.put("cs_"+UUID.randomUUID().toString(),DateraCommon.INITIATOR_1);
        initiatorsMap.put("cs_"+UUID.randomUUID().toString(),DateraCommon.INITIATOR_2);
        initiatorsMap.put("cs_"+UUID.randomUUID().toString(),DateraCommon.INITIATOR_3);
        initiatorsMap.put("cs_"+UUID.randomUUID().toString(),DateraCommon.INITIATOR_4);
        initiatorsMap.put("cs_"+UUID.randomUUID().toString(),DateraCommon.INITIATOR_5);
        rest.registerInitiators(initiatorsMap);
        List<String> initiators = rest.getInitiators();
        assertTrue(initiators.contains(DateraCommon.INITIATOR_1));
        assertTrue(initiators.contains(DateraCommon.INITIATOR_2));
        assertTrue(initiators.contains(DateraCommon.INITIATOR_3));
        assertTrue(initiators.contains(DateraCommon.INITIATOR_4));
        assertTrue(initiators.contains(DateraCommon.INITIATOR_5));
        
        rest.unregisterInitiator(DateraCommon.INITIATOR_1);
        rest.unregisterInitiator(DateraCommon.INITIATOR_2);
        rest.unregisterInitiator(DateraCommon.INITIATOR_3);
        rest.unregisterInitiator(DateraCommon.INITIATOR_4);
        rest.unregisterInitiator(DateraCommon.INITIATOR_5);
    }
    
    @Test
    public void utRegisterPrimaryStorage() 
    {
        String appInstanceName = generateAppName();
        DateraRestClient rest = new DateraRestClient(DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD);
        assertTrue(rest.isAppInstanceExists(appInstanceName));

        String storageInstanceName = rest.defaultStorageName;
        String volumeInstanceName = rest.defaultVolumeName;
        String networkPoolName = "default";
        long capacityBytes = 5368709120L;
        int replica = 3;
        long totalIOPS = 1000;
        
        rest.createAppInstance(appInstanceName);
        rest.createStorageInstance(appInstanceName, storageInstanceName,networkPoolName);
        int dtVolSize = DateraUtil.getVolumeSizeInGB(capacityBytes);
        rest.createVolume(appInstanceName, storageInstanceName, volumeInstanceName, dtVolSize,replica);
        rest.setQos(appInstanceName, storageInstanceName, volumeInstanceName, totalIOPS);

        AppInstanceInfo.VolumeInfo volInfo = rest.getVolumeInfo(appInstanceName, storageInstanceName, volumeInstanceName);
        boolean volumeCreationSuccess = true;
        String err = "";
        assertTrue(volInfo.name.equals(volumeInstanceName));
        assertTrue(0 == volInfo.opState.compareTo(DateraRestClient.OP_STATE_AVAILABLE));

        AppInstanceInfo.StorageInstance storageInfo = rest.getStorageInfo(appInstanceName, storageInstanceName);
        deleteAppInstance(appInstanceName, rest);
        
    }

    private void deleteAppInstance(String appInstanceName, DateraRestClient rest) {
        rest.setAdminState(appInstanceName, false);
        rest.deleteAppInstance(appInstanceName);
    }

    private String generateAppName() {
        DateFormat df = new SimpleDateFormat("dd-MM-yy-HH-mm-ss");
        Date date = new Date();
        String appInstanceName = "App-"+df.format(date);
        return appInstanceName;
    }
}
