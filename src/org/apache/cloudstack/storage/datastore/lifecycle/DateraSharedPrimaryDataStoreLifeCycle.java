package org.apache.cloudstack.storage.datastore.lifecycle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreParameters;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.utils.AppInstanceInfo;
import org.apache.cloudstack.storage.datastore.utils.DateraRestClient;
import org.apache.cloudstack.storage.datastore.utils.DateraRestClientMgr;
import org.apache.cloudstack.storage.datastore.utils.DateraUtil;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolAutomation;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.template.TemplateManager;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.exception.CloudRuntimeException;

public class DateraSharedPrimaryDataStoreLifeCycle implements PrimaryDataStoreLifeCycle {
    private static final Logger s_logger = Logger.getLogger(DateraSharedPrimaryDataStoreLifeCycle.class);

    @Inject private AccountDao _accountDao;
    @Inject private AccountDetailsDao _accountDetailsDao;
    @Inject private AgentManager _agentMgr;
    @Inject private ClusterDao _clusterDao;
    @Inject private ClusterDetailsDao _clusterDetailsDao;
    @Inject private DataCenterDao _zoneDao;
    @Inject private HostDao _hostDao;
    @Inject private PrimaryDataStoreDao _primaryDataStoreDao;
    @Inject private PrimaryDataStoreHelper _primaryDataStoreHelper;
    @Inject private ResourceManager _resourceMgr;
    @Inject private StorageManager _storageMgr;
    @Inject private StoragePoolAutomation _storagePoolAutomation;
    @Inject private StoragePoolDetailsDao _storagePoolDetailsDao;
    @Inject private StoragePoolHostDao _storagePoolHostDao;
    @Inject protected TemplateManager _tmpltMgr;
    private Long _timeout=10000L;

    // invoked to add primary storage that is based on the Datera plug-in
    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {
        final String CAPACITY_IOPS = "capacityIops";

        String url = (String)dsInfos.get("url");
        Long zoneId = (Long)dsInfos.get("zoneId");
        Long podId = (Long)dsInfos.get("podId");
        Long clusterId = (Long)dsInfos.get("clusterId");
        String storagePoolName = (String)dsInfos.get("name");
        String providerName = (String)dsInfos.get("providerName");
        Long capacityBytes = (Long)dsInfos.get("capacityBytes");
        Long capacityIops = (Long)dsInfos.get(CAPACITY_IOPS);
        String tags = (String)dsInfos.get("tags");

        int replica=0;

        capacityBytes = DateraRestClientMgr.getInstance().buildBytesPerGB(capacityBytes);

        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>)dsInfos.get("details");

        if (podId == null) {
            throw new CloudRuntimeException("Zone-wide scope is not supported by DateraShared provider");
        }

        if (clusterId == null) {
            throw new CloudRuntimeException("The Cluster ID must be specified.");
        }

        if (capacityBytes == null || capacityBytes <= 0) {
            throw new IllegalArgumentException("'capacityBytes' must be present and greater than 0.");
        }

        if (capacityIops == null || capacityIops > DateraUtil.MAX_TOTAL_IOPS_PER_VOLUME || capacityIops < DateraUtil.MIN_TOTAL_IOPS_PER_VOLUME) {
            throw new IllegalArgumentException("'Capacity IOPS' must be between "+ DateraUtil.MIN_TOTAL_IOPS_PER_VOLUME + " and "+ DateraUtil.MAX_TOTAL_IOPS_PER_VOLUME);
        }

        String val = DateraUtil.getValue(DateraUtil.VOLUME_REPLICA, url,false);
        if(null == val)
        {
            replica = DateraUtil.DEFAULT_VOLUME_REPLICA;
        }
        else
        {
            replica = Integer.parseInt(val);
        }
        if (replica > DateraUtil.MAX_VOLUME_REPLICA || replica < DateraUtil.MIN_VOLUME_REPLICA) {
            throw new IllegalArgumentException("'replica' must be between "+ DateraUtil.MIN_VOLUME_REPLICA + " and "+ DateraUtil.MAX_VOLUME_REPLICA);
        }

        val = DateraUtil.getValue("timeout", url,false);
        if(null == val)
        {
            _timeout = 10000L;
        }
        else
        {
            _timeout = Long.parseLong(val);
        }

        HypervisorType hypervisorType = getHypervisorTypeForCluster(clusterId);

        if (!isSupportedHypervisorType(hypervisorType)) {
            throw new CloudRuntimeException(hypervisorType + " is not a supported hypervisor type.");
        }

        String datacenter = "dummy1";
/*
        String datacenter = DateraUtil.getValue(DateraUtil.DATACENTER, url, false);

        if (HypervisorType.VMware.equals(hypervisorType) && datacenter == null) {
            throw new CloudRuntimeException("'Datacenter' must be set for hypervisor type of " + HypervisorType.VMware);
        }
*/
        PrimaryDataStoreParameters parameters = new PrimaryDataStoreParameters();

        parameters.setType(getStorageType(hypervisorType));
        parameters.setZoneId(zoneId);
        parameters.setPodId(podId);
        parameters.setClusterId(clusterId);
        parameters.setName(storagePoolName);
        parameters.setProviderName(providerName);
        parameters.setManaged(false);
        parameters.setCapacityBytes(capacityBytes);
        parameters.setUsedBytes(0);
        parameters.setCapacityIops(capacityIops);
        parameters.setHypervisorType(hypervisorType);
        parameters.setTags(tags);
        parameters.setDetails(details);

        String managementVip = "";
        int managementPort = 0;
        String managementUsername = "";
        String managementPassword = "";
        String networkPoolName = "";
        String appInstanceName = "";
        String storageInstanceName = "";
        String clvmVolumeGroupName = "";

        if(isDateraSupported(hypervisorType))
        {
            managementVip = DateraUtil.getManagementIP(url);
            managementPort = DateraUtil.getManagementPort(url);
            managementUsername = DateraUtil.getValue(DateraUtil.MANAGEMENT_USERNAME, url);
            managementPassword = DateraUtil.getValue(DateraUtil.MANAGEMENT_PASSWORD, url);
            networkPoolName = DateraUtil.getValue(DateraUtil.NETWORK_POOL_NAME, url);
            //_timeout = Long.parseLong(DateraUtil.getValue("timeout", url));
        }

        if(HypervisorType.KVM.equals(hypervisorType))
        {
            clvmVolumeGroupName = DateraUtil.getValue(DateraUtil.CLVM_VOLUME_GROUP_NAME, url);
            appInstanceName = DateraUtil.getValue(DateraUtil.APP_NAME, url);
            storageInstanceName = DateraUtil.getValue(DateraUtil.STORAGE_NAME, url);
        }


        String iqn = "";
        String storageVip = "";
        int storagePort = DateraUtil.DEFAULT_STORAGE_PORT;
        String storagePath = "";
        DateraRestClient rest= new DateraRestClient(managementVip, managementPort, managementUsername, managementPassword);
        DateraUtil.DateraMetaData dtMetaData = new DateraUtil.DateraMetaData(managementVip, managementPort, managementUsername, managementPassword, storagePoolName, replica, networkPoolName, appInstanceName, storageInstanceName,"" ,clvmVolumeGroupName);

        if(isDateraSupported(hypervisorType))
        {
            appInstanceName = storagePoolName;
            appInstanceName.replace(" ", "");
            appInstanceName = DateraRestClientMgr.getInstance().suggestAppInstanceName(rest, dtMetaData, appInstanceName);
            validateHostsAvailability(clusterId);
            AppInstanceInfo.StorageInstance dtStorageInfo = DateraRestClientMgr.getInstance().createVolume(rest, managementVip, managementPort, managementUsername, managementPassword, appInstanceName, networkPoolName, capacityBytes, replica, capacityIops);

            if(null == dtStorageInfo || null == dtStorageInfo.access || dtStorageInfo.access.iqn == null || dtStorageInfo.access.iqn.isEmpty())
            {
                throw new CloudRuntimeException("IQN not generated on the primary storage.");
            }

            if(dtStorageInfo.access.ips == null || 0 == dtStorageInfo.access.ips.size())
            {
                throw new CloudRuntimeException("Storage IP not generated for the primary storage.");
            }
            storageInstanceName = dtStorageInfo.name;
            iqn = dtStorageInfo.access.iqn;
            storageVip = dtStorageInfo.access.ips.get(0);
        }
        else
        {
            iqn="iqn";
            storageVip = clvmVolumeGroupName;
            storagePath = clvmVolumeGroupName;
        }
        String initiatorGroupName = DateraUtil.generateInitiatorGroupName(appInstanceName);
        Map<String, String> initiators = extractInitiators(clusterId);
        DateraRestClientMgr.getInstance().registerInitiators(rest, managementVip, managementPort,
                managementUsername, managementPassword, appInstanceName,
                storageInstanceName, initiatorGroupName, initiators, _timeout);

        parameters.setUuid(iqn);

        details.put(DateraUtil.MANAGEMENT_IP, managementVip);
        details.put(DateraUtil.MANAGEMENT_PORT, String.valueOf(managementPort));
        details.put(DateraUtil.MANAGEMENT_USERNAME, managementUsername);
        details.put(DateraUtil.MANAGEMENT_PASSWORD, managementPassword);
        details.put(DateraUtil.APP_NAME, appInstanceName);
        details.put(DateraUtil.STORAGE_NAME, storageInstanceName);
        details.put(DateraUtil.NETWORK_POOL_NAME,networkPoolName);
        details.put(DateraUtil.CLVM_VOLUME_GROUP_NAME,clvmVolumeGroupName);
        details.put(DateraUtil.INITIATOR_GROUP_NAME, initiatorGroupName);

        if (HypervisorType.VMware.equals(hypervisorType)) {
            String datastore = iqn.replace("/", "_");
            String path = "/" + datacenter + "/" + datastore;

            parameters.setHost("VMFS datastore: " + path);
            parameters.setPort(0);
            parameters.setPath(path);

            details.put(DateraUtil.DATASTORE_NAME, datastore);
            details.put(DateraUtil.IQN, iqn);
            details.put(DateraUtil.STORAGE_VIP, storageVip);
            details.put(DateraUtil.STORAGE_PORT, String.valueOf(storagePort));
        }
        else if (HypervisorType.KVM.equals(hypervisorType))
        {
            parameters.setPath(storagePath);
            parameters.setHost(storageVip);
            parameters.setPort(storagePort);
        }
        else {
            parameters.setHost(storageVip);
            parameters.setPort(storagePort);
            parameters.setPath("/"+iqn+"/0");
        }

        // this adds a row in the cloud.storage_pool table for this Datera volume
        DataStore dataStore = _primaryDataStoreHelper.createPrimaryDataStore(parameters);



        return dataStore;
    }
    private boolean validateHostsAvailability(long clusterId)
    {
       boolean ret = false;
       List<HostVO> hosts = _hostDao.findByClusterId(clusterId);
       if(0 == hosts.size())
       {
          throw new CloudRuntimeException("Cannot create volume, there are no hosts available in the cluster");
       }
       return ret;
    }


    private Map<String, String> extractInitiators(Long clusterId) {
        List<HostVO> hosts = _hostDao.findByClusterId(clusterId);
        Map<String,String> initiators = new HashMap<String,String>();

        for(HostVO host : hosts)
        {
            initiators.put(DateraUtil.constructInitiatorLabel(host.getUuid()) ,host.getStorageUrl());
        }
        if(0 == initiators.size())
        {
           throw new CloudRuntimeException("The hosts do not have initiators");
        }
        return initiators;
    }


    private HypervisorType getHypervisorTypeForCluster(long clusterId) {
        ClusterVO cluster = _clusterDao.findById(clusterId);

        if (cluster == null) {
            throw new CloudRuntimeException("Cluster ID '" + clusterId + "' was not found in the database.");
        }

        return cluster.getHypervisorType();
    }

    private StoragePoolType getStorageType(HypervisorType hypervisorType) {
        if (HypervisorType.XenServer.equals(hypervisorType)) {
            return StoragePoolType.IscsiLUN;
        }
        if(HypervisorType.KVM.equals(hypervisorType))
        {
            return StoragePoolType.CLVM;
        }
/*
        if (HypervisorType.VMware.equals(hypervisorType)) {
            return StoragePoolType.VMFS;
        }
*/
        throw new CloudRuntimeException("The 'hypervisor' parameter must be '" + HypervisorType.XenServer + "' or '" + HypervisorType.KVM + "'.");
    }

    @Override
    public boolean attachHost(DataStore store, HostScope scope, StoragePoolInfo existingInfo) {
        return true;
    }

    @Override
    public boolean attachCluster(DataStore store, ClusterScope scope) {
        PrimaryDataStoreInfo primaryDataStoreInfo = (PrimaryDataStoreInfo)store;

        // check if there is at least one host up in this cluster
        List<HostVO> allHosts = _resourceMgr.listAllUpAndEnabledHosts(Host.Type.Routing, primaryDataStoreInfo.getClusterId(),
                primaryDataStoreInfo.getPodId(), primaryDataStoreInfo.getDataCenterId());

        if (allHosts.isEmpty()) {
            _primaryDataStoreDao.expunge(primaryDataStoreInfo.getId());

            throw new CloudRuntimeException("No host up to associate a storage pool with in cluster " + primaryDataStoreInfo.getClusterId());
        }

        boolean success = false;

        for (HostVO host : allHosts) {
            success = createStoragePool(host, primaryDataStoreInfo);

            if (success) {
                break;
            }
        }

        if (!success) {
            throw new CloudRuntimeException("Unable to create storage in cluster " + primaryDataStoreInfo.getClusterId());
        }

        List<HostVO> poolHosts = new ArrayList<HostVO>();

        for (HostVO host : allHosts) {
            try {
                _storageMgr.connectHostToSharedPool(host.getId(), primaryDataStoreInfo.getId());

                poolHosts.add(host);
            } catch (Exception e) {
                s_logger.warn("Unable to establish a connection between " + host + " and " + primaryDataStoreInfo, e);
            }
        }

        if (poolHosts.isEmpty()) {
            s_logger.warn("No host can access storage pool '" + primaryDataStoreInfo + "' on cluster '" + primaryDataStoreInfo.getClusterId() + "'.");

            _primaryDataStoreDao.expunge(primaryDataStoreInfo.getId());

            throw new CloudRuntimeException("Failed to access storage pool");
        }

        _primaryDataStoreHelper.attachCluster(store);

        return true;
    }

    private boolean createStoragePool(HostVO host, StoragePool storagePool) {
        long hostId = host.getId();
        HypervisorType hypervisorType = host.getHypervisorType();
        CreateStoragePoolCommand cmd = new CreateStoragePoolCommand(true, storagePool);

        if (HypervisorType.VMware.equals(hypervisorType)) {
            cmd.setCreateDatastore(true);

            Map<String, String> details = new HashMap<String, String>();

            StoragePoolDetailVO storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePool.getId(), DateraUtil.DATASTORE_NAME);

            details.put(CreateStoragePoolCommand.DATASTORE_NAME, storagePoolDetail.getValue());

            storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePool.getId(), DateraUtil.IQN);

            details.put(CreateStoragePoolCommand.IQN, storagePoolDetail.getValue());

            storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePool.getId(), DateraUtil.STORAGE_VIP);

            details.put(CreateStoragePoolCommand.STORAGE_HOST, storagePoolDetail.getValue());

            storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePool.getId(), DateraUtil.STORAGE_PORT);

            details.put(CreateStoragePoolCommand.STORAGE_PORT, storagePoolDetail.getValue());

            cmd.setDetails(details);
        }

        Answer answer = _agentMgr.easySend(hostId, cmd);

        if (answer != null && answer.getResult()) {
            return true;
        } else {
            _primaryDataStoreDao.expunge(storagePool.getId());

            String msg = "";

            if (answer != null) {
                msg = "Cannot create storage pool through host '" + hostId + "' due to the following: " + answer.getDetails();
            } else {
                msg = "Cannot create storage pool through host '" + hostId + "' due to CreateStoragePoolCommand returns null";
            }

            s_logger.warn(msg);

            throw new CloudRuntimeException(msg);
        }
    }

    @Override
    public boolean attachZone(DataStore dataStore, ZoneScope scope, HypervisorType hypervisorType) {
        throw new CloudRuntimeException("Zone scope not supported");
        //return true;
    }

    @Override
    public boolean maintain(DataStore dataStore) {
        _storagePoolAutomation.maintain(dataStore);
        _primaryDataStoreHelper.maintain(dataStore);

        StoragePool storagePool = (StoragePool)dataStore;
        DateraUtil.DateraMetaData dtMetaData = DateraUtil.getDateraCred(storagePool.getId(), _storagePoolDetailsDao);
        DateraRestClientMgr.getInstance().setAdminState(null, dtMetaData, false);
        return true;
    }

    @Override
    public boolean cancelMaintain(DataStore store) {
        _primaryDataStoreHelper.cancelMaintain(store);
        _storagePoolAutomation.cancelMaintain(store);

        StoragePool storagePool = (StoragePool)store;
        DateraUtil.DateraMetaData dtMetaData = DateraUtil.getDateraCred(storagePool.getId(), _storagePoolDetailsDao);
        DateraRestClientMgr.getInstance().setAdminState(null, dtMetaData, true);

        return true;
    }

    // invoked to delete primary storage that is based on the Datera plug-in
    @Override
    public boolean deleteDataStore(DataStore dataStore) {
        List<StoragePoolHostVO> hostPoolRecords = _storagePoolHostDao.listByPoolId(dataStore.getId());

        HypervisorType hypervisorType = null;

        if (hostPoolRecords.size() > 0 ) {
            hypervisorType = getHypervisorType(hostPoolRecords.get(0).getHostId());
        }

        if (!isSupportedHypervisorType(hypervisorType)) {
            throw new CloudRuntimeException(hypervisorType + " is not a supported hypervisor type.");
        }

        StoragePool storagePool = (StoragePool)dataStore;
        StoragePoolVO storagePoolVO = _primaryDataStoreDao.findById(storagePool.getId());
        List<VMTemplateStoragePoolVO> unusedTemplatesInPool = _tmpltMgr.getUnusedTemplatesInPool(storagePoolVO);

        for (VMTemplateStoragePoolVO templatePoolVO : unusedTemplatesInPool) {
            _tmpltMgr.evictTemplateFromStoragePool(templatePoolVO);
        }

        Long clusterId = null;

        for (StoragePoolHostVO host : hostPoolRecords) {
            DeleteStoragePoolCommand deleteCmd = new DeleteStoragePoolCommand(storagePool);

            if (HypervisorType.VMware.equals(hypervisorType)) {
                deleteCmd.setRemoveDatastore(true);

                Map<String, String> details = new HashMap<String, String>();

                StoragePoolDetailVO storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePool.getId(), DateraUtil.DATASTORE_NAME);

                details.put(DeleteStoragePoolCommand.DATASTORE_NAME, storagePoolDetail.getValue());

                storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePool.getId(), DateraUtil.IQN);

                details.put(DeleteStoragePoolCommand.IQN, storagePoolDetail.getValue());

                storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePool.getId(), DateraUtil.STORAGE_VIP);

                details.put(DeleteStoragePoolCommand.STORAGE_HOST, storagePoolDetail.getValue());

                storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePool.getId(), DateraUtil.STORAGE_PORT);

                details.put(DeleteStoragePoolCommand.STORAGE_PORT, storagePoolDetail.getValue());

                deleteCmd.setDetails(details);
            }

            final Answer answer = _agentMgr.easySend(host.getHostId(), deleteCmd);

            if (answer != null && answer.getResult()) {
                s_logger.info("Successfully deleted storage pool using Host ID " + host.getHostId());

                HostVO hostVO = _hostDao.findById(host.getHostId());

                if (hostVO != null) {
                    clusterId = hostVO.getClusterId();
                }

                break;
            }
            else {
                s_logger.error("Failed to delete storage pool using Host ID " + host.getHostId() + ": " + answer.getResult());
            }
        }

        if (clusterId != null) {
            //remove initiator from the storage
            //unregister the initiators or remove the initiator group
        }

        if(isDateraSupported(hypervisorType))
            deleteDateraApplicationInstance(storagePool.getId());

        return _primaryDataStoreHelper.deletePrimaryDataStore(dataStore);
    }

    private boolean isDateraSupported(HypervisorType hypervisorType)
    {
       if(HypervisorType.VMware.equals(hypervisorType) || HypervisorType.XenServer.equals(hypervisorType))
          return true;
       return false;
    }
    private boolean deleteDateraApplicationInstance(long storagePoolId) {
        DateraUtil.DateraMetaData dtMetaData = DateraUtil.getDateraCred(storagePoolId, _storagePoolDetailsDao);
        return DateraRestClientMgr.getInstance().deleteAppInstanceAndInitiatorGroup(dtMetaData);
    }

    private long getIopsValue(long storagePoolId, String iopsKey) {
        StoragePoolDetailVO storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePoolId, iopsKey);

        String iops = storagePoolDetail.getValue();

        return Long.parseLong(iops);
    }

    private static boolean isSupportedHypervisorType(HypervisorType hypervisorType) {
        return HypervisorType.XenServer.equals(hypervisorType) || HypervisorType.VMware.equals(hypervisorType) || HypervisorType.KVM.equals(hypervisorType);
    }

    private HypervisorType getHypervisorType(long hostId) {
        HostVO host = _hostDao.findById(hostId);

        if (host != null) {
            return host.getHypervisorType();
        }

        return HypervisorType.None;
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
        String strCapacityBytes = details.get(PrimaryDataStoreLifeCycle.CAPACITY_BYTES);
        String strCapacityIops = details.get(PrimaryDataStoreLifeCycle.CAPACITY_IOPS);

        Long capacityBytes = strCapacityBytes != null ? Long.parseLong(strCapacityBytes) : null;
        Long capacityIops = strCapacityIops != null ? Long.parseLong(strCapacityIops) : null;


        long newSize = capacityBytes != null ? capacityBytes : storagePool.getCapacityBytes();

        if(newSize < storagePool.getCapacityBytes())
        {
             throw new CloudRuntimeException("Cannot shrink the capacity bytes, from "+ storagePool.getCapacityBytes()+", to requested capacity bytes"+ newSize );
        }

        if (capacityIops == null || capacityIops > DateraUtil.MAX_TOTAL_IOPS_PER_VOLUME || capacityIops < DateraUtil.MIN_TOTAL_IOPS_PER_VOLUME) {
            throw new IllegalArgumentException("'capacityIops' must be between "+ DateraUtil.MIN_TOTAL_IOPS_PER_VOLUME + " and "+ DateraUtil.MAX_TOTAL_IOPS_PER_VOLUME);
        }


        DateraUtil.DateraMetaData dtMetaData = DateraUtil.getDateraCred(storagePool.getId(), _storagePoolDetailsDao);
        DateraRestClient rest = new DateraRestClient(dtMetaData.mangementIP, dtMetaData.managementPort, dtMetaData.managementUserName, dtMetaData.managementPassword);
        //update the volume capacity, resize the volume

        if(newSize != storagePool.getCapacityIops())
        {
            DateraRestClientMgr.getInstance().updatePrimaryStorageCapacityBytes(rest, dtMetaData, newSize);
        }
        if(capacityIops != storagePool.getCapacityIops())
        {
            DateraRestClientMgr.getInstance().updatePrimaryStorageIOPS(rest, dtMetaData, capacityIops);
        }
    }

}
