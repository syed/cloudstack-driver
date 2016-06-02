package org.apache.cloudstack.storage.test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

        DateraRestClient rest = new DateraRestClient(DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD);
        List<String> initiators = registerInitiators(rest);
        unRegisterInitiators(rest, initiators);
    }

    @Test
    public void utRegisterInitiatorGroup()
    {
        DateraRestClient rest = new DateraRestClient(DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD);
        List<String> initiators = registerInitiators(rest);

        List<String> initiatorGroups;
		String initiatorGroupName = createInitiatorGroup(rest, initiators);
        
        deleteInitiatorGroup(rest, initiatorGroupName);
        
        unRegisterInitiators(rest, initiators);
    }

	private void deleteInitiatorGroup(DateraRestClient rest,
			String initiatorGroupName) {
		List<String> initiatorGroups;
		rest.deleteInitiatorGroup(initiatorGroupName);
        initiatorGroups = rest.enumerateInitiatorGroups();
        assertEquals(false, initiatorGroups.contains(initiatorGroupName));
	}

	private String createInitiatorGroup(DateraRestClient rest,
			List<String> initiators) {
		String initiatorGroupName = "myGroup";
        List<String> initiatorGroups = rest.enumerateInitiatorGroups();
        for(;;)
        {
        	if(false == initiatorGroups.contains(initiatorGroupName))
        	{
        		break;
        	}
        	else
        	{
        		initiatorGroupName = initiatorGroupName+Math.random();
        	}
        }
        rest.createInitiatorGroup(initiatorGroupName, initiators);
          
        initiatorGroups = rest.enumerateInitiatorGroups();
        assertEquals(true, initiatorGroups.contains(initiatorGroupName));
		return initiatorGroupName;
	}
    @Test
    public void utRegisterPrimaryStorage() 
    {
        String appInstanceName = generateAppName();
        String networkPoolName = "default";
        long capacityBytes = 5368709120L;
        int replica = 3;
        long totalIOPS = 1000;
        String accessControlMode = "allow_all";
        int dtVolSize = DateraUtil.getVolumeSizeInGB(capacityBytes);

        DateraRestClient rest = new DateraRestClient(DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD);
        assertTrue(rest.isAppInstanceExists(appInstanceName));
        assertEquals(0,rest.enumerateNetworkPool().contains(networkPoolName));
        
        String storageInstanceName = rest.defaultStorageName;
        String volumeInstanceName = rest.defaultVolumeName;
        
        List<String> initiators = registerInitiators(rest);
        String initiatorGroupName = createInitiatorGroup(rest, initiators);
        List<String> initiatorGroups = new ArrayList<String>();
        initiatorGroups.add(initiatorGroupName);
        rest.createVolume(appInstanceName, null, initiatorGroups, dtVolSize, replica, accessControlMode, networkPoolName,totalIOPS);
        AppInstanceInfo.VolumeInfo volInfo = rest.getVolumeInfo(appInstanceName, storageInstanceName, volumeInstanceName);
        boolean volumeCreationSuccess = true;
        String err = "";
        assertTrue(volInfo.name.equals(volumeInstanceName));
        assertTrue(0 == volInfo.opState.compareTo(DateraRestClient.OP_STATE_AVAILABLE));

        AppInstanceInfo.StorageInstance storageInfo = rest.getStorageInfo(appInstanceName, storageInstanceName);
        deleteAppInstance(appInstanceName, rest);
        
        deleteInitiatorGroup(rest, initiatorGroupName);
        unRegisterInitiators(rest, initiators);
        
    }

    private List<String> registerInitiators(DateraRestClient rest) {
        Map<String,String> initiatorsMap = new HashMap<String,String>();
        //name -- Must be a combination of letters, numbers, spaces, underscores, and dashes between 3 and 128 characters; must start with either a letter or underscore"
        initiatorsMap.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()),DateraCommon.INITIATOR_1);
        initiatorsMap.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()),DateraCommon.INITIATOR_2);
        initiatorsMap.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()),DateraCommon.INITIATOR_3);
        initiatorsMap.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()),DateraCommon.INITIATOR_4);
        initiatorsMap.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()),DateraCommon.INITIATOR_5);
        rest.registerInitiators(initiatorsMap);
        List<String> initiators = rest.getInitiators();
        assertTrue(initiators.contains(DateraCommon.INITIATOR_1));
        assertTrue(initiators.contains(DateraCommon.INITIATOR_2));
        assertTrue(initiators.contains(DateraCommon.INITIATOR_3));
        assertTrue(initiators.contains(DateraCommon.INITIATOR_4));
        assertTrue(initiators.contains(DateraCommon.INITIATOR_5));
        
        return initiators;
	}
    
    private void unRegisterInitiators(DateraRestClient rest, List<String> initiators)
    {
    	for(String iter : initiators)
    	{
            rest.unregisterInitiator(iter);
    	}
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
