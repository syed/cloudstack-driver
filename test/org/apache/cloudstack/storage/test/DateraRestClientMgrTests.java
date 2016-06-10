package org.apache.cloudstack.storage.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.cloudstack.storage.datastore.utils.AppInstanceInfo;
import org.apache.cloudstack.storage.datastore.utils.DateraModel;
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
    private DateraUtil.DateraMetaData dtMetaData = null;
    @After
    public void cleanUp() {
        if(null == rest)
        {
            rest = new DateraRestClient(DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD);
        }
        
        assertTrue(DateraRestClientMgr.getInstance().deleteAppInstance(rest, dtMetaData));

        assertEquals(false,DateraRestClientMgr.getInstance().isAppInstanceExists(rest, dtMetaData));
        
        assertTrue(DateraRestClientMgr.getInstance().deleteInitiatorGroup(rest, dtMetaData));

        assertTrue(DateraRestClientMgr.getInstance().unRegisterInitiators(rest,dtMetaData,iqns));
    }

    @Test
    public void utCreateVolumeWithInitiators()
    {
        rest =  new DateraRestClient(DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD);
        appInstanceName = DateraRestClientMgr.getInstance().suggestAppInstanceName(rest, dtMetaData, null);
        String initiatorGroupName = DateraRestClientMgr.getInstance().generateInitiatorGroupName(rest, dtMetaData,iqns,appInstanceName);
        dtMetaData = new DateraUtil.DateraMetaData(DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, "dummy", 3, DateraCommon.DEFAULT_NETWORK_POOL_NAME, appInstanceName, DateraModel.defaultStorageName, initiatorGroupName,"vgDummy");
        DateraRestClientMgr.getInstance().createVolume(rest, DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, appInstanceName, DateraCommon.DEFAULT_NETWORK_POOL_NAME, DateraCommon.DEFAULT_CAPACITY_BYTES,DateraCommon.DEFAULT_REPLICA, DateraCommon.DEFAULT_CAPACITY_IOPS);
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
        DateraRestClientMgr.getInstance().registerInitiators(rest, DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, appInstanceName, DateraModel.defaultStorageName, initiatorGroupName, initiators, 1);

        storageInfo = DateraRestClientMgr.getInstance().getStorageInfo(rest,dtMetaData);
        assertEquals(1,storageInfo.aclPolicy.initiatorGroups.size());
        assertTrue(storageInfo.aclPolicy.initiatorGroups.get(0).contains(initiatorGroupName));

    }
    
    @Test
    public void utCreateVolumeWithInitiatorsNonInitRest()
    {
        dtMetaData = new DateraUtil.DateraMetaData();
        dtMetaData.mangementIP = DateraCommon.MANAGEMENT_IP;
        dtMetaData.managementPort = DateraCommon.PORT;
        dtMetaData.managementUserName = DateraCommon.USERNAME;
        dtMetaData.managementPassword = DateraCommon.PASSWORD;
        dtMetaData.storagePoolName = "dummySP";
        dtMetaData.replica = 3;
        dtMetaData.networkPoolName = "default";
        dtMetaData.appInstanceName = "";
        dtMetaData.storageInstanceName = "storage-1";
        dtMetaData.initiatorGroupName = "";
        dtMetaData.clvmVolumeGroupName = "dummyCLVM";

        appInstanceName = DateraRestClientMgr.getInstance().suggestAppInstanceName(rest, dtMetaData, null);
        String initiatorGroupName = DateraRestClientMgr.getInstance().generateInitiatorGroupName(rest, dtMetaData,iqns,appInstanceName);
        dtMetaData.appInstanceName = appInstanceName;
        dtMetaData.initiatorGroupName = initiatorGroupName;
        DateraRestClientMgr.getInstance().createVolume(rest, DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, appInstanceName, DateraCommon.DEFAULT_NETWORK_POOL_NAME, DateraCommon.DEFAULT_CAPACITY_BYTES,DateraCommon.DEFAULT_REPLICA, DateraCommon.DEFAULT_CAPACITY_IOPS);
        AppInstanceInfo.StorageInstance storageInfo = DateraRestClientMgr.getInstance().getStorageInfo(rest,dtMetaData);
        assertTrue(null != storageInfo);
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
        DateraRestClientMgr.getInstance().registerInitiators(rest, DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, appInstanceName, "storage-1", initiatorGroupName, initiators, 1);

        storageInfo = DateraRestClientMgr.getInstance().getStorageInfo(rest,dtMetaData);
        assertEquals(1,storageInfo.aclPolicy.initiatorGroups.size());
        assertTrue(storageInfo.aclPolicy.initiatorGroups.get(0).contains(initiatorGroupName));

    }

    @Test
    public void utResizeVolume()
    {
        rest =  new DateraRestClient(DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD);
        appInstanceName = DateraRestClientMgr.getInstance().suggestAppInstanceName(rest, dtMetaData, null);
        String initiatorGroupName = DateraRestClientMgr.getInstance().generateInitiatorGroupName(rest, dtMetaData,iqns,appInstanceName);
        dtMetaData = new DateraUtil.DateraMetaData(DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, "dummy", 3, DateraCommon.DEFAULT_NETWORK_POOL_NAME, appInstanceName, DateraModel.defaultStorageName, initiatorGroupName,"vgDummy");
        DateraRestClientMgr.getInstance().createVolume(rest, DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, appInstanceName, DateraCommon.DEFAULT_NETWORK_POOL_NAME, DateraCommon.DEFAULT_CAPACITY_BYTES,DateraCommon.DEFAULT_REPLICA, DateraCommon.DEFAULT_CAPACITY_IOPS);
        DateraUtil.DateraMetaData dtMetaData = new DateraUtil.DateraMetaData(DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, "dummy", 3, DateraCommon.DEFAULT_NETWORK_POOL_NAME, appInstanceName, DateraModel.defaultStorageName, "dummyInitiatorGroupName","vgDummy");
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
        DateraRestClientMgr.getInstance().registerInitiators(rest, DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, appInstanceName, DateraModel.defaultStorageName, initiatorGroupName, initiators, 1);

        storageInfo = DateraRestClientMgr.getInstance().getStorageInfo(rest, dtMetaData);
        assertEquals(1,storageInfo.aclPolicy.initiatorGroups.size());
        assertTrue(storageInfo.aclPolicy.initiatorGroups.get(0).contains(initiatorGroupName));

        long capacityBytes = 2147483648L;
        DateraRestClientMgr.getInstance().updatePrimaryStorageCapacityBytes(rest, dtMetaData, capacityBytes);
        AppInstanceInfo.StorageInstance updatedInfo = DateraRestClientMgr.getInstance().getStorageInfo(rest, dtMetaData);
        dtVolumeSize = DateraRestClientMgr.getInstance().getDateraCompatibleVolumeInGB(capacityBytes);
        assertNotEquals(dtVolumeSize, storageInfo.volumes.volume1.size);
        assertEquals(dtVolumeSize, updatedInfo.volumes.volume1.size);
        DateraModel.AppModel appInfo = DateraRestClientMgr.getInstance().getAppInstanceInfo(rest, dtMetaData);
        assertTrue(null != appInfo);
        assertEquals(DateraUtil.ADMIN_STATE_ONLINE, appInfo.adminState);
    }
    @Test
    public void utResizeVolumeNonInitRest()
    {
        dtMetaData = new DateraUtil.DateraMetaData();
        dtMetaData.mangementIP = DateraCommon.MANAGEMENT_IP;
        dtMetaData.managementPort = DateraCommon.PORT;
        dtMetaData.managementUserName = DateraCommon.USERNAME;
        dtMetaData.managementPassword = DateraCommon.PASSWORD;
        dtMetaData.storagePoolName = "dummySP";
        dtMetaData.replica = 3;
        dtMetaData.networkPoolName = "default";
        dtMetaData.appInstanceName = "";
        dtMetaData.storageInstanceName = DateraModel.defaultStorageName;
        dtMetaData.initiatorGroupName = "";
        dtMetaData.clvmVolumeGroupName = "dummyCLVM";

        appInstanceName = DateraRestClientMgr.getInstance().suggestAppInstanceName(rest, dtMetaData, null);
        String initiatorGroupName = DateraRestClientMgr.getInstance().generateInitiatorGroupName(rest, dtMetaData,iqns,appInstanceName);
        dtMetaData.appInstanceName = appInstanceName;
        dtMetaData.initiatorGroupName = initiatorGroupName;
        DateraRestClientMgr.getInstance().createVolume(rest, DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, appInstanceName, DateraCommon.DEFAULT_NETWORK_POOL_NAME, DateraCommon.DEFAULT_CAPACITY_BYTES,DateraCommon.DEFAULT_REPLICA, DateraCommon.DEFAULT_CAPACITY_IOPS);
        DateraUtil.DateraMetaData dtMetaData = new DateraUtil.DateraMetaData(DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, "dummy", 3, DateraCommon.DEFAULT_NETWORK_POOL_NAME, appInstanceName, DateraModel.defaultStorageName, "dummyInitiatorGroupName","vgDummy");
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
        DateraRestClientMgr.getInstance().registerInitiators(rest, DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, appInstanceName, DateraModel.defaultStorageName, initiatorGroupName, initiators, 1);

        storageInfo = DateraRestClientMgr.getInstance().getStorageInfo(rest, dtMetaData);
        assertEquals(1,storageInfo.aclPolicy.initiatorGroups.size());
        assertTrue(storageInfo.aclPolicy.initiatorGroups.get(0).contains(initiatorGroupName));

        long capacityBytes = 2147483648L;
        DateraRestClientMgr.getInstance().updatePrimaryStorageCapacityBytes(rest, dtMetaData, capacityBytes);
        AppInstanceInfo.StorageInstance updatedInfo = DateraRestClientMgr.getInstance().getStorageInfo(rest, dtMetaData);
        dtVolumeSize = DateraRestClientMgr.getInstance().getDateraCompatibleVolumeInGB(capacityBytes);
        assertNotEquals(dtVolumeSize, storageInfo.volumes.volume1.size);
        assertEquals(dtVolumeSize, updatedInfo.volumes.volume1.size);
        DateraModel.AppModel appInfo = DateraRestClientMgr.getInstance().getAppInstanceInfo(rest, dtMetaData);
        assertTrue(null != appInfo);
        assertEquals(DateraUtil.ADMIN_STATE_ONLINE, appInfo.adminState);
    }

    @Test
    public void utUpdateIOPS()
    {
        rest =  new DateraRestClient(DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD);
        appInstanceName = DateraRestClientMgr.getInstance().suggestAppInstanceName(rest, dtMetaData, null);
        String initiatorGroupName = DateraRestClientMgr.getInstance().generateInitiatorGroupName(rest, dtMetaData,iqns,appInstanceName);
        dtMetaData = new DateraUtil.DateraMetaData(DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, "dummy", 3, DateraCommon.DEFAULT_NETWORK_POOL_NAME, appInstanceName, DateraModel.defaultStorageName, initiatorGroupName,"vgDummy");
        DateraRestClientMgr.getInstance().createVolume(rest, DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, appInstanceName, DateraCommon.DEFAULT_NETWORK_POOL_NAME, DateraCommon.DEFAULT_CAPACITY_BYTES,DateraCommon.DEFAULT_REPLICA, DateraCommon.DEFAULT_CAPACITY_IOPS);
        DateraUtil.DateraMetaData dtMetaData = new DateraUtil.DateraMetaData(DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, "dummy", 3, DateraCommon.DEFAULT_NETWORK_POOL_NAME, appInstanceName, DateraModel.defaultStorageName, "dummyInitiatorGroup","vgDummy");
        AppInstanceInfo.StorageInstance storageInfo = DateraRestClientMgr.getInstance().getStorageInfo(rest,dtMetaData);

        Map<String,String> initiators = new HashMap<String,String>();
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_1);
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_2);
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_3);
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_4);
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_5);
        iqns = new ArrayList<String>(initiators.values());
        DateraRestClientMgr.getInstance().registerInitiators(rest, DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, appInstanceName, DateraModel.defaultStorageName, initiatorGroupName, initiators, 1);

        storageInfo = DateraRestClientMgr.getInstance().getStorageInfo(rest, dtMetaData);
        assertEquals(1,storageInfo.aclPolicy.initiatorGroups.size());
        assertTrue(storageInfo.aclPolicy.initiatorGroups.get(0).contains(initiatorGroupName));

        long capacityIOPS = 1001L;
        DateraRestClientMgr.getInstance().updatePrimaryStorageIOPS(rest, dtMetaData, capacityIOPS);

        DateraModel.PerformancePolicy perf = DateraRestClientMgr.getInstance().getQos(rest, dtMetaData);
        assertTrue(null != perf);
        assertEquals(capacityIOPS, perf.totalIopsMax);
        DateraModel.AppModel appInfo = DateraRestClientMgr.getInstance().getAppInstanceInfo(rest, dtMetaData);
        assertTrue(null != appInfo);
        assertEquals(DateraUtil.ADMIN_STATE_ONLINE, appInfo.adminState);
    }

    @Test
    public void utUpdateIOPSNonInitRest()
    {
        dtMetaData = new DateraUtil.DateraMetaData();
        dtMetaData.mangementIP = DateraCommon.MANAGEMENT_IP;
        dtMetaData.managementPort = DateraCommon.PORT;
        dtMetaData.managementUserName = DateraCommon.USERNAME;
        dtMetaData.managementPassword = DateraCommon.PASSWORD;
        dtMetaData.storagePoolName = "dummySP";
        dtMetaData.replica = 3;
        dtMetaData.networkPoolName = "default";
        dtMetaData.appInstanceName = "";
        dtMetaData.storageInstanceName = DateraModel.defaultStorageName;
        dtMetaData.initiatorGroupName = "";
        dtMetaData.clvmVolumeGroupName = "dummyCLVM";

        appInstanceName = DateraRestClientMgr.getInstance().suggestAppInstanceName(rest, dtMetaData, null);
        String initiatorGroupName = DateraRestClientMgr.getInstance().generateInitiatorGroupName(rest, dtMetaData,iqns,appInstanceName);
        dtMetaData.appInstanceName = appInstanceName;
        dtMetaData.initiatorGroupName = initiatorGroupName;
        DateraRestClientMgr.getInstance().createVolume(rest, DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, appInstanceName, DateraCommon.DEFAULT_NETWORK_POOL_NAME, DateraCommon.DEFAULT_CAPACITY_BYTES,DateraCommon.DEFAULT_REPLICA, DateraCommon.DEFAULT_CAPACITY_IOPS);
        DateraUtil.DateraMetaData dtMetaData = new DateraUtil.DateraMetaData(DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, "dummy", 3, DateraCommon.DEFAULT_NETWORK_POOL_NAME, appInstanceName, DateraModel.defaultStorageName, "dummyInitiatorGroup","vgDummy");
        AppInstanceInfo.StorageInstance storageInfo = DateraRestClientMgr.getInstance().getStorageInfo(rest,dtMetaData);

        Map<String,String> initiators = new HashMap<String,String>();
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_1);
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_2);
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_3);
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_4);
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_5);
        iqns = new ArrayList<String>(initiators.values());
        DateraRestClientMgr.getInstance().registerInitiators(rest, DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, appInstanceName, DateraModel.defaultStorageName, initiatorGroupName, initiators, 1);

        storageInfo = DateraRestClientMgr.getInstance().getStorageInfo(rest, dtMetaData);
        assertEquals(1,storageInfo.aclPolicy.initiatorGroups.size());
        assertTrue(storageInfo.aclPolicy.initiatorGroups.get(0).contains(initiatorGroupName));

        long capacityIOPS = 1001L;
        DateraRestClientMgr.getInstance().updatePrimaryStorageIOPS(rest, dtMetaData, capacityIOPS);

        DateraModel.PerformancePolicy perf = DateraRestClientMgr.getInstance().getQos(rest, dtMetaData);
        assertTrue(null != perf);
        assertEquals(capacityIOPS, perf.totalIopsMax);
        DateraModel.AppModel appInfo = DateraRestClientMgr.getInstance().getAppInstanceInfo(rest, dtMetaData);
        assertTrue(null != appInfo);
        assertEquals(DateraUtil.ADMIN_STATE_ONLINE, appInfo.adminState);
    }

    @Test
    public void utCaptureAllMethods()
    {
        DateraRestClientMgr.getInstance().setAllowThrowException(false);

        assertEquals(false, DateraRestClientMgr.getInstance().deleteAppInstance(null, null));
        assertEquals(false, DateraRestClientMgr.getInstance().deleteInitiatorGroup(null, null));
        assertEquals(false, DateraRestClientMgr.getInstance().deleteAppInstanceAndInitiatorGroup(null));
        assertEquals(false, DateraRestClientMgr.getInstance().updatePrimaryStorageCapacityBytes(null,null,10000));
        assertEquals(false, DateraRestClientMgr.getInstance().updatePrimaryStorageIOPS(null,null,10000));
        assertEquals(null, DateraRestClientMgr.getInstance().getStorageInfo(null,null));
        assertEquals(null, DateraRestClientMgr.getInstance().getAppInstanceInfo(null,null));
        assertEquals(null, DateraRestClientMgr.getInstance().getQos(null,null));
        assertEquals(null, DateraRestClientMgr.getInstance().suggestAppInstanceName(null,null,null));
        assertEquals(null, DateraRestClientMgr.getInstance().generateInitiatorGroupName(null,null,null,null));
        assertEquals(false, DateraRestClientMgr.getInstance().unRegisterInitiators(null,null,null));
        assertEquals(false, DateraRestClientMgr.getInstance().isAppInstanceExists(null,null));

        //5368709120 = 5GB
        long newCapacity = 5368709120L;
        assertEquals(newCapacity, DateraRestClientMgr.getInstance().getCloudstackCompatibleVolumeSize(5));
        
        dtMetaData = new DateraUtil.DateraMetaData();
        dtMetaData.mangementIP = DateraCommon.MANAGEMENT_IP;
        dtMetaData.managementPort = DateraCommon.PORT;
        dtMetaData.managementUserName = DateraCommon.USERNAME;
        dtMetaData.managementPassword = DateraCommon.PASSWORD;
        dtMetaData.storagePoolName = "dummySP";
        dtMetaData.replica = 3;
        dtMetaData.networkPoolName = "default";
        dtMetaData.appInstanceName = "";
        dtMetaData.storageInstanceName = "storage-1";
        dtMetaData.initiatorGroupName = "";
        dtMetaData.clvmVolumeGroupName = "dummyCLVM";

        appInstanceName = DateraRestClientMgr.getInstance().suggestAppInstanceName(rest, dtMetaData, null);
        String initiatorGroupName = DateraRestClientMgr.getInstance().generateInitiatorGroupName(rest, dtMetaData,iqns,appInstanceName);
        dtMetaData.appInstanceName = appInstanceName;
        dtMetaData.initiatorGroupName = initiatorGroupName;
        DateraRestClientMgr.getInstance().createVolume(rest, DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, appInstanceName, DateraCommon.DEFAULT_NETWORK_POOL_NAME, DateraCommon.DEFAULT_CAPACITY_BYTES,DateraCommon.DEFAULT_REPLICA, DateraCommon.DEFAULT_CAPACITY_IOPS);
        DateraModel.AppModel appModel = DateraRestClientMgr.getInstance().getAppInstanceInfo(rest, dtMetaData);
        assertTrue(null != appModel);
        assertEquals(dtMetaData.appInstanceName,appModel.name);
        assertEquals(DateraUtil.ADMIN_STATE_ONLINE, appModel.adminState);
        
        Map<String,String> initiators = new HashMap<String,String>();
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_1);
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_2);
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_3);
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_4);
        initiators.put(DateraUtil.constructInitiatorLabel(UUID.randomUUID().toString()), DateraCommon.INITIATOR_5);
        iqns = new ArrayList<String>(initiators.values());
        DateraRestClientMgr.getInstance().registerInitiators(rest, DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, DateraCommon.PASSWORD, appInstanceName, "storage-1", initiatorGroupName, initiators, 1);

        DateraModel.PerformancePolicy policy = DateraRestClientMgr.getInstance().getQos(rest, dtMetaData);
        assertTrue(null != policy);
        assertEquals(DateraCommon.DEFAULT_CAPACITY_IOPS,policy.totalIopsMax);

    }
}
