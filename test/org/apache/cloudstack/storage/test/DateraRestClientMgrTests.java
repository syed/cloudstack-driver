package org.apache.cloudstack.storage.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.cloudstack.storage.datastore.utils.AppInstanceInfo;
import org.apache.cloudstack.storage.datastore.utils.DateraRestClient;
import org.apache.cloudstack.storage.datastore.utils.DateraRestClientMgr;
import org.apache.cloudstack.storage.datastore.utils.DateraUtil;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DateraRestClientMgrTests {

	private DateraRestClient rest = null;
	private String appInstanceName="";
	private List<String> iqns = null;
	private String initiatorGroupName = null;
    @After
    public void cleanUp() {
    	if(null == rest)
    	{
    		rest = new DateraRestClient(DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD);
    	}
    	rest.setAdminState(appInstanceName, false);
        rest.deleteAppInstance(appInstanceName);

        assertEquals(false,rest.isAppInstanceExists(appInstanceName));
        
        rest.deleteInitiatorGroup(initiatorGroupName);
        
        unRegisterInitiators(rest,iqns);
    }

    @Test
    public void utCreateVolumeWithInitiators()
    {
    	appInstanceName = DateraCommon.generateAppName();
        rest =  new DateraRestClient(DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD);
        DateraRestClientMgr.getInstance().createVolume(rest, DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, appInstanceName, DateraCommon.DEFAULT_NETWORK_POOL_NAME, DateraCommon.DEFAULT_CAPACITY_BYTES,DateraCommon.DEFAULT_REPLICA, DateraCommon.DEFAULT_CAPACITY_IOPS);
        DateraUtil.DateraMetaData dtMetaData = new DateraUtil.DateraMetaData(DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, "dummy", 3, DateraCommon.DEFAULT_NETWORK_POOL_NAME, appInstanceName, rest.defaultStorageName, "vgDummy");
        AppInstanceInfo.StorageInstance storageInfo = DateraRestClientMgr.getInstance().getStorageInfo(rest,dtMetaData);
        int dtVolumeSize = DateraRestClientMgr.getInstance().getDateraCompatibleVolumeInGB(DateraCommon.DEFAULT_CAPACITY_BYTES);
        assertEquals(dtVolumeSize, storageInfo.volumes.volume1.size);
        assertEquals(DateraCommon.DEFAULT_REPLICA, storageInfo.volumes.volume1.replicaCount);
        assertTrue(storageInfo.ipPool.contains(DateraCommon.DEFAULT_NETWORK_POOL_NAME));
        assertEquals(0,storageInfo.aclPolicy.initiatorGroups.size());
        
        Map<String,String> initiators = new HashMap<String,String>();
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_1);
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_2);
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_3);
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_4);
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_5);
        iqns = new ArrayList<String>(initiators.values());
        initiatorGroupName = generateInitiatorGroup(rest, iqns);
        DateraRestClientMgr.getInstance().registerInitiators(rest, DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, appInstanceName, rest.defaultStorageName, initiatorGroupName, initiators, 1);

        storageInfo = rest.getStorageInfo(appInstanceName,rest.defaultStorageName);
        assertEquals(1,storageInfo.aclPolicy.initiatorGroups.size());
        assertTrue(storageInfo.aclPolicy.initiatorGroups.get(0).contains(initiatorGroupName));

    }
    private String generateInitiatorGroup(DateraRestClient rest,
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
        return initiatorGroupName;
    }
    
    private void unRegisterInitiators(DateraRestClient rest, List<String> initiators)
    {
        for(String iter : initiators)
        {
            rest.unregisterInitiator(iter);
        }
    }
    @Test
    public void utResizeVolume()
    {
        appInstanceName = DateraCommon.generateAppName();
        rest =  new DateraRestClient(DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD);
        DateraRestClientMgr.getInstance().createVolume(rest, DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, appInstanceName, DateraCommon.DEFAULT_NETWORK_POOL_NAME, DateraCommon.DEFAULT_CAPACITY_BYTES,DateraCommon.DEFAULT_REPLICA, DateraCommon.DEFAULT_CAPACITY_IOPS);
        DateraUtil.DateraMetaData dtMetaData = new DateraUtil.DateraMetaData(DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, "dummy", 3, DateraCommon.DEFAULT_NETWORK_POOL_NAME, appInstanceName, rest.defaultStorageName, "vgDummy");
        AppInstanceInfo.StorageInstance storageInfo = DateraRestClientMgr.getInstance().getStorageInfo(rest,dtMetaData);
        int dtVolumeSize = DateraRestClientMgr.getInstance().getDateraCompatibleVolumeInGB(DateraCommon.DEFAULT_CAPACITY_BYTES);
        assertEquals(dtVolumeSize, storageInfo.volumes.volume1.size);
        assertEquals(DateraCommon.DEFAULT_REPLICA, storageInfo.volumes.volume1.replicaCount);
        assertTrue(storageInfo.ipPool.contains(DateraCommon.DEFAULT_NETWORK_POOL_NAME));
        assertEquals(0,storageInfo.aclPolicy.initiatorGroups.size());
        
        Map<String,String> initiators = new HashMap<String,String>();
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_1);
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_2);
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_3);
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_4);
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_5);
        iqns = new ArrayList<String>(initiators.values());
        initiatorGroupName = generateInitiatorGroup(rest, iqns);
        DateraRestClientMgr.getInstance().registerInitiators(rest, DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, appInstanceName, rest.defaultStorageName, initiatorGroupName, initiators, 1);

        storageInfo = rest.getStorageInfo(appInstanceName,rest.defaultStorageName);
        assertEquals(1,storageInfo.aclPolicy.initiatorGroups.size());
        assertTrue(storageInfo.aclPolicy.initiatorGroups.get(0).contains(initiatorGroupName));

        long capacityBytes = 2147483648L;
        DateraRestClientMgr.getInstance().updatePrimaryStorageCapacityBytes(rest, dtMetaData, capacityBytes);
        AppInstanceInfo.StorageInstance updatedInfo = DateraRestClientMgr.getInstance().getStorageInfo(rest, dtMetaData);
        dtVolumeSize = DateraRestClientMgr.getInstance().getDateraCompatibleVolumeInGB(capacityBytes);
        assertNotEquals(dtVolumeSize, storageInfo.volumes.volume1.size);
        assertEquals(dtVolumeSize, updatedInfo.volumes.volume1.size);
    }

}
