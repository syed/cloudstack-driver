/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.datastore.lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreParameters;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.utils.DateraRestClient;
import org.apache.cloudstack.storage.datastore.utils.DateraRestClient.StorageResponse;
import org.apache.cloudstack.storage.datastore.utils.DateraUtil;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;

import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.capacity.CapacityManager;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.user.AccountDetailsDao;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolAutomation;
import com.cloud.utils.exception.CloudRuntimeException;

public class DateraPrimaryDataStoreLifeCycle implements PrimaryDataStoreLifeCycle {
    private static final Logger s_logger = Logger.getLogger(DateraPrimaryDataStoreLifeCycle.class);

    @Inject private CapacityManager _capacityMgr;
    @Inject private DataCenterDao zoneDao;
    @Inject private PrimaryDataStoreDao storagePoolDao;
    @Inject private PrimaryDataStoreHelper dataStoreHelper;
    @Inject private ResourceManager _resourceMgr;
    @Inject private StorageManager _storageMgr;
    @Inject private StoragePoolAutomation storagePoolAutomation;
    @Inject private HostDao _hostDao;
    @Inject private ClusterDao _clusterDao;
    @Inject private ClusterDetailsDao clusterDetailsDao;
    @Inject private AccountDetailsDao accountDetailsDao;
    @Inject private PrimaryDataStoreDao _primaryDataStoreDao;
    private int count = 1;

    // invoked to add primary storage that is based on the Datera plug-in
    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {
        String url = (String)dsInfos.get("url");
        Long zoneId = (Long)dsInfos.get("zoneId");
        Long clusterId = (Long)dsInfos.get("clusterId");
        String storagePoolName = (String)dsInfos.get("name");
        String providerName = (String)dsInfos.get("providerName");
        Long capacityBytes = (Long)dsInfos.get("capacityBytes");
        Long capacityIops = (Long)dsInfos.get("capacityIops");
        String tags = (String)dsInfos.get("tags");

        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>)dsInfos.get("details");


        DataCenterVO zone = zoneDao.findById(zoneId);


        if (capacityBytes == null || capacityBytes <= 0) {
            throw new IllegalArgumentException("'capacityBytes' must be present and greater than 0.");
        }

        if (capacityIops == null || capacityIops <= 0) {
            throw new IllegalArgumentException("'capacityIops' must be present and greater than 0.");
        }

        String managementIP = DateraUtil.getManagementIP(url);
        int managementPort = DateraUtil.getManagementPort(url);

        String appInstanceName = DateraUtil.getValue(DateraUtil.APP_NAME, url);
        String managementUsername = DateraUtil.getValue(DateraUtil.MANAGEMENT_USERNAME, url);
        String managementPassword = DateraUtil.getValue(DateraUtil.MANAGEMENT_PASSWORD, url);

        String networkPoolName = DateraUtil.getValue(DateraUtil.NETWORK_POOL_NAME, url);
        int volReplica = DateraUtil.getReplica(url);
        Long maxTotalIOPs = DateraUtil.getMaxTotalIOPs(url);
        Long maxReadIOPs = DateraUtil.getMaxReadIOPs(url);
        Long maxWriteIOPs = DateraUtil.getMaxWriteIOPs(url);
        Long maxTotalBandWidth = DateraUtil.getMaxTotalBandwidth(url);
        Long maxReadBandWidth = DateraUtil.getMaxReadBandwidth(url);
        Long maxWriteBandWidth = DateraUtil.getMaxWriteBandwidth(url);

        PrimaryDataStoreParameters parameters = new PrimaryDataStoreParameters();

        //parameters.setHost(storageVip);
        //parameters.setPort(storagePort);
        //parameters.setPath(DateraUtil.getModifiedUrl(url));
        //parameters.setPath("/export/storage");
        parameters.setType(StoragePoolType.Iscsi);
        parameters.setZoneId(zoneId);
        parameters.setName(storagePoolName);
        parameters.setProviderName(providerName);
        parameters.setManaged(true);
        parameters.setCapacityBytes(capacityBytes);
        parameters.setUsedBytes(0);
        parameters.setCapacityIops(capacityIops);
        parameters.setHypervisorType(HypervisorType.Any);
        parameters.setTags(tags);
        parameters.setDetails(details);



        details.put(DateraUtil.MANAGEMENT_IP, managementIP);
        details.put(DateraUtil.MANAGEMENT_PORT,String.valueOf(managementPort));
        details.put(DateraUtil.MANAGEMENT_USERNAME, managementUsername);
        details.put(DateraUtil.MANAGEMENT_PASSWORD, managementPassword);

        details.put(DateraUtil.NETWORK_POOL_NAME,networkPoolName);
        details.put(DateraUtil.VOLUME_REPLICA,String.valueOf(volReplica));
        details.put(DateraUtil.MAX_TOTAL_IOPS,String.valueOf(maxTotalIOPs));
        details.put(DateraUtil.MAX_READ_IOPS,String.valueOf(maxReadIOPs));
        details.put(DateraUtil.MAX_WRITE_IOPS,String.valueOf(maxWriteIOPs));
        details.put(DateraUtil.MAX_TOTAL_BANDWIDTH,String.valueOf(maxTotalBandWidth));
        details.put(DateraUtil.MAX_READ_BANDWIDTH,String.valueOf(maxReadBandWidth));
        details.put(DateraUtil.MAX_WRITE_BANDWIDTH,String.valueOf(maxWriteBandWidth));


        DateraRestClient.StorageResponse storageInfo = createApplicationInstance(managementIP,managementPort,managementUsername,managementPassword,appInstanceName,networkPoolName);

        if(null == storageInfo.access.ips || null == storageInfo.access.iqn || 0 == storageInfo.access.ips.size() || 0 == storageInfo.access.iqn.length())
            throw new CloudRuntimeException("Could not get Storage ip and iqn");

        String uuid = DateraUtil.PROVIDER_NAME + "_" + zone.getUuid() + "_" + storageInfo.access.ips.get(0);

        parameters.setHost(storageInfo.access.ips.get(0));
        parameters.setPath(storageInfo.access.iqn);
        parameters.setUuid(uuid);


       // this adds a row in the cloud.storage_pool table for this Datera cluster
        return dataStoreHelper.createPrimaryDataStore(parameters);
    }

    private StorageResponse createApplicationInstance(String managementIP, int managementPort, String managementUsername, String managementPassword, String appInstanceName, String networkPoolName) {

        DateraRestClient rest = new DateraRestClient(managementIP, managementPort, managementUsername, managementPassword);
        if(rest.isAppInstanceExists(appInstanceName))
             throw new CloudRuntimeException("App name already exists : "+appInstanceName);

        //rest.createAppInstance(appInstanceName);
        //rest.createStorageInstance(appInstanceName, rest.defaultStorageName);
        //rest.createVolume(appInstanceName, rest.defaultStorageName, rest.defaultVolumeName, 2);
        rest.createVolume(appInstanceName, null, null, 2, 3, "allow_all", "/access_network_ip_pools/"+networkPoolName);
        rest.setAdminState(appInstanceName, false);
        rest.deleteVolume(appInstanceName, rest.defaultStorageName, rest.defaultVolumeName);
        rest.setAdminState(appInstanceName, true);
        return rest.getStorageInfo(appInstanceName, rest.defaultStorageName);
     }

    private StorageResponse createDateraVolume(String storageVip, int storagePort, String clusterAdminUsername,
        String clusterAdminPassword, String appName) {
        StorageResponse resp = null;

        DateraRestClient rest = new DateraRestClient(storageVip, storagePort, clusterAdminUsername, clusterAdminPassword);
/*
        //create the initiators group
        Account csAccount = CallContext.current().getCallingAccount();
        List<HostVO> hosts = _hostDao.findByClusterId(clusterId);
        ClusterVO cluster = _clusterDao.findById(clusterId);
        List<String> iqns = new ArrayList<String>();
        for(HostVO host : hosts)
        {
         iqns.add(host.getStorageUrl());
        }
        String iqnGroupName = "Initiator"+csAccount.getUuid();
        rest.createInitiatorGroup(iqnGroupName, iqns);
*/

        List<String> initiatorGroups = new ArrayList<String>();
        initiatorGroups.add("/initiator_groups/cluster2_initiator_group");
        initiatorGroups.add("/initiator_groups/test_initiator_groups1");
        initiatorGroups.add("/initiator_groups/Computes");

        List<String> initiators = new ArrayList<String>();

        //create the volume on datera node
        //rest.createVolume(appName,initiators,2,"allow_all",initiatorGroups);

        return rest.getStorageInfo(appName, "storage-1");

   }

    // do not implement this method for Datera's plug-in
    @Override
    public boolean attachHost(DataStore store, HostScope scope, StoragePoolInfo existingInfo) {
        return true; // should be ignored for zone-wide-only plug-ins like Datera's
    }

    // do not implement this method for Datera's plug-in
    @Override
    public boolean attachCluster(DataStore store, ClusterScope scope) {
        return true; // should be ignored for zone-wide-only plug-ins like Datera's
    }

    @Override
    public boolean attachZone(DataStore dataStore, ZoneScope scope, HypervisorType hypervisorType) {
        dataStoreHelper.attachZone(dataStore);

        List<HostVO> xenServerHosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByHypervisor(HypervisorType.XenServer, scope.getScopeId());
        List<HostVO> vmWareServerHosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByHypervisor(HypervisorType.VMware, scope.getScopeId());
        List<HostVO> kvmHosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByHypervisor(HypervisorType.KVM, scope.getScopeId());
        List<HostVO> hosts = new ArrayList<HostVO>();

        hosts.addAll(xenServerHosts);
        hosts.addAll(vmWareServerHosts);
        hosts.addAll(kvmHosts);

        for (HostVO host : hosts) {
            try {
                _storageMgr.connectHostToSharedPool(host.getId(), dataStore.getId());
            } catch (Exception e) {
                s_logger.warn("Unable to establish a connection between " + host + " and " + dataStore, e);
            }
        }

        return true;
    }

    @Override
    public boolean maintain(DataStore dataStore) {
        storagePoolAutomation.maintain(dataStore);
        dataStoreHelper.maintain(dataStore);

        return true;
    }

    @Override
    public boolean cancelMaintain(DataStore store) {
        dataStoreHelper.cancelMaintain(store);
        storagePoolAutomation.cancelMaintain(store);

        return true;
    }

    // invoked to delete primary storage that is based on the Datera plug-in
    @Override
    public boolean deleteDataStore(DataStore store) {
        return dataStoreHelper.deletePrimaryDataStore(store);
    }

    /* (non-Javadoc)
     * @see org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle#migrateToObjectStore(org.apache.cloudstack.engine.subsystem.api.storage.DataStore)
     */
    @Override
    public boolean migrateToObjectStore(DataStore store) {
        return false;
    }

    @Override
    public void updateStoragePool(StoragePool storagePool, Map<String, String> details) {
        StoragePoolVO storagePoolVo = storagePoolDao.findById(storagePool.getId());

        String strCapacityBytes = details.get(PrimaryDataStoreLifeCycle.CAPACITY_BYTES);
        Long capacityBytes = strCapacityBytes != null ? Long.parseLong(strCapacityBytes) : null;

        if (capacityBytes != null) {
            long usedBytes = _capacityMgr.getUsedBytes(storagePoolVo);

            if (capacityBytes < usedBytes) {
                throw new CloudRuntimeException("Cannot reduce the number of bytes for this storage pool as it would lead to an insufficient number of bytes");
            }
        }

        String strCapacityIops = details.get(PrimaryDataStoreLifeCycle.CAPACITY_IOPS);
        Long capacityIops = strCapacityIops != null ? Long.parseLong(strCapacityIops) : null;

        if (capacityIops != null) {
            long usedIops = _capacityMgr.getUsedIops(storagePoolVo);

            if (capacityIops < usedIops) {
                throw new CloudRuntimeException("Cannot reduce the number of IOPS for this storage pool as it would lead to an insufficient number of IOPS");
            }
        }
    }
}
