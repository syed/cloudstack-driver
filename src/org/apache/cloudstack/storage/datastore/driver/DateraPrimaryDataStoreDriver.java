// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.storage.datastore.driver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ChapInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.utils.AppInstanceInfo;
import org.apache.cloudstack.storage.datastore.utils.DateraRestClient;
import org.apache.cloudstack.storage.datastore.utils.DateraUtil;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.capacity.CapacityManager;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.exception.CloudRuntimeException;

public class DateraPrimaryDataStoreDriver implements PrimaryDataStoreDriver {
    private static final Logger s_logger = Logger.getLogger(DateraPrimaryDataStoreDriver.class);

    @Inject private AccountDao _accountDao;
    @Inject private AccountDetailsDao _accountDetailsDao;
    @Inject private CapacityManager _capacityMgr;
    @Inject private ClusterDao _clusterDao;
    @Inject private ClusterDetailsDao _clusterDetailsDao;
    @Inject private HostDao _hostDao;
    @Inject private PrimaryDataStoreDao _storagePoolDao;
    @Inject private StoragePoolDetailsDao _storagePoolDetailsDao;
    @Inject private VolumeDao _volumeDao;
    @Inject private VolumeDetailsDao _volumeDetailsDao;

    @Override
    public Map<String, String> getCapabilities() {
        return null;
    }

    @Override
    public DataTO getTO(DataObject data) {
        return null;
    }

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        return null;
    }

    @Override
    public ChapInfo getChapInfo(VolumeInfo volumeInfo) {
        return null;
    }

    // get the VAG associated with volumeInfo's cluster, if any (ListVolumeAccessGroups)
    // if the VAG exists
    //     update the VAG to contain all IQNs of the hosts (ModifyVolumeAccessGroup)
    //     if the ID of volumeInfo in not in the VAG, add it (ModifyVolumeAccessGroup)
    // if the VAG doesn't exist, create it with the IQNs of the hosts and the ID of volumeInfo (CreateVolumeAccessGroup)
    @Override
    public synchronized boolean connectVolumeToHost(VolumeInfo volumeInfo, Host host, DataStore dataStore)
    {
        if (volumeInfo == null || host == null || dataStore == null) {
            return false;
        }
        s_logger.info("Begin connectVolumeToHost host iqn = "+host.getStorageUrl());
        long dtVolumeId = Long.parseLong(volumeInfo.getFolder());
        long clusterId = host.getClusterId();
        long storagePoolId = dataStore.getId();
/*
        ClusterDetailsVO clusterDetail = _clusterDetailsDao.findDetail(clusterId, DateraUtil.getVagKey(storagePoolId));

        String vagId = clusterDetail != null ? clusterDetail.getValue() : null;
*/
        List<HostVO> hosts = _hostDao.findByClusterId(clusterId);

        if (!DateraUtil.hostsSupport_iScsi(hosts)) {
           return false;
        }
/*
        DateraUtil.DateraConnection dtConnection = DateraUtil.getDateraConnection(storagePoolId, _storagePoolDetailsDao);

        if (vagId != null) {
            DateraUtil.DateraVag dtVag = DateraUtil.getDateraVag(dtConnection, Long.parseLong(vagId));

            String[] hostIqns = DateraUtil.getNewHostIqns(dtVag.getInitiators(), DateraUtil.getIqnsFromHosts(hosts));
            long[] volumeIds = DateraUtil.getNewVolumeIds(dtVag.getVolumeIds(), dtVolumeId, true);

            DateraUtil.modifyDateraVag(dtConnection, dtVag.getId(), hostIqns, volumeIds);
        }
        else {
            ClusterVO cluster = _clusterDao.findById(clusterId);

            DateraUtil.placeVolumeInVolumeAccessGroup(dtConnection, dtVolumeId, storagePoolId, cluster.getUuid(), hosts, _clusterDetailsDao);
        }
*/

        if(null == host.getStorageUrl())
        {
            throw new CloudRuntimeException("Host iqn not available, cannot register the host");
        }
        List<String> initiators = new ArrayList<String>();
        initiators.add(DateraUtil.constructInitiatorName(host.getStorageUrl()));
        DateraUtil.DateraMetaData dtMetaData = DateraUtil.getDateraCred(storagePoolId, _storagePoolDetailsDao);
        DateraRestClient rest = new DateraRestClient(dtMetaData.mangementIP, dtMetaData.managementPort, dtMetaData.managementUserName, dtMetaData.managementPassword);
        rest.registerInitiator(DateraUtil.generateInitiatorLabel(host.getUuid()), host.getStorageUrl());

        rest.updateStorageWithInitiator(dtMetaData.appInstanceName, rest.defaultStorageName, initiators);
        s_logger.info("End connectVolumeToHost ");
        return true;
    }

    // get the VAG associated with volumeInfo's cluster, if any (ListVolumeAccessGroups) // might not exist if using CHAP
    // if the VAG exists
    //     remove the ID of volumeInfo from the VAG (ModifyVolumeAccessGroup)
    @Override
    public synchronized void disconnectVolumeFromHost(VolumeInfo volumeInfo, Host host, DataStore dataStore)
    {
        if (volumeInfo == null || host == null || dataStore == null) {
           return;
        }
        s_logger.info("Begin disconnectVolumeFromHost host iqn ="+host.getStorageUrl());
/*
        long dtVolumeId = Long.parseLong(volumeInfo.getFolder());
        long clusterId = host.getClusterId();
        long storagePoolId = dataStore.getId();

        ClusterDetailsVO clusterDetail = _clusterDetailsDao.findDetail(clusterId, DateraUtil.getVagKey(storagePoolId));

        String vagId = clusterDetail != null ? clusterDetail.getValue() : null;

        if (vagId != null) {
            List<HostVO> hosts = _hostDao.findByClusterId(clusterId);

            DateraUtil.DateraConnection dtConnection = DateraUtil.getDateraConnection(storagePoolId, _storagePoolDetailsDao);

            DateraUtil.DateraVag dtVag = DateraUtil.getDateraVag(dtConnection, Long.parseLong(vagId));

            String[] hostIqns = DateraUtil.getNewHostIqns(dtVag.getInitiators(), DateraUtil.getIqnsFromHosts(hosts));
            long[] volumeIds = DateraUtil.getNewVolumeIds(dtVag.getVolumeIds(), dtVolumeId, false);

            DateraUtil.modifyDateraVag(dtConnection, dtVag.getId(), hostIqns, volumeIds);
        }
*/
        if(null == host.getStorageUrl())
        {
            throw new CloudRuntimeException("Host iqn not available, cannot unregister the host");
        }

/*        long storagePoolId = dataStore.getId();
        DateraUtil.DateraMetaData dtMetaData = DateraUtil.getDateraCred(storagePoolId, _storagePoolDetailsDao);
        DateraRestClient rest = new DateraRestClient(dtMetaData.mangementIP, dtMetaData.managementPort, dtMetaData.managementUserName, dtMetaData.managementPassword);
        rest.unregisterInitiator(host.getStorageUrl());
*/
    }

    @Override
    public long getUsedBytes(StoragePool storagePool) {
        s_logger.info("_Datera Begin getUsedBytes");
        long usedSpace = 0;
        List<VolumeVO> lstVolumes = _volumeDao.findByPoolId(storagePool.getId(), null);

        if (lstVolumes != null) {
            for (VolumeVO volume : lstVolumes) {
                VolumeDetailVO volumeDetail = _volumeDetailsDao.findDetail(volume.getId(), DateraUtil.VOLUME_SIZE_NAME);

                if (volumeDetail != null && volumeDetail.getValue() != null) {
                    long volumeSize = Long.parseLong(volumeDetail.getValue());

                    usedSpace += volumeSize;
                }
            }
        }
        s_logger.info("_Datera End getUsedBytes usedSpace ="+usedSpace);

        return usedSpace;
    }

    @Override
    public long getVolumeSizeIncludingHypervisorSnapshotReserve(Volume volume, StoragePool pool) {
        s_logger.info("_Datera Begin getVolumeSizeIncludingHypervisorSnapshotReserve");
        long volumeSize = volume.getSize();
        Integer hypervisorSnapshotReserve = volume.getHypervisorSnapshotReserve();

        if (hypervisorSnapshotReserve != null) {
            if (hypervisorSnapshotReserve < 50) {
                hypervisorSnapshotReserve = 50;
            }

            volumeSize += volumeSize * (hypervisorSnapshotReserve / 100f);
        }
        s_logger.info("_Datera End getVolumeSizeIncludingHypervisorSnapshotReserve volumeSize ="+volumeSize);

        return volumeSize;
    }

    private static class Iops {
        private final long _minIops;
        private final long _maxIops;
        private final long _burstIops;

        public Iops(long minIops, long maxIops, long burstIops) throws IllegalArgumentException {
            if (minIops <= 0 || maxIops <= 0) {
                throw new IllegalArgumentException("The 'Min IOPS' and 'Max IOPS' values must be greater than 0.");
            }

            if (minIops > maxIops) {
                throw new IllegalArgumentException("The 'Min IOPS' value cannot exceed the 'Max IOPS' value.");
            }

            if (maxIops > burstIops) {
                throw new IllegalArgumentException("The 'Max IOPS' value cannot exceed the 'Burst IOPS' value.");
            }

            _minIops = minIops;
            _maxIops = maxIops;
            _burstIops = burstIops;
        }

        public long getMinIops() {
            return _minIops;
        }

        public long getMaxIops() {
            return _maxIops;
        }

        public long getBurstIops() {
            return _burstIops;
        }
    }


    @Override
    public void createAsync(DataStore dataStore, DataObject dataObject, AsyncCompletionCallback<CreateCmdResult> callback) {
        String iqn = null;
        String errMsg = null;

        if (dataObject.getType() == DataObjectType.VOLUME) {
            VolumeInfo volumeInfo = (VolumeInfo)dataObject;
            AccountVO account = _accountDao.findById(volumeInfo.getAccountId());

            long storagePoolId = dataStore.getId();


            DateraUtil.DateraMetaData dtMetaData = DateraUtil.getDateraCred(storagePoolId, _storagePoolDetailsDao);

            DateraRestClient rest = new DateraRestClient(dtMetaData.mangementIP, dtMetaData.managementPort, dtMetaData.managementUserName, dtMetaData.managementPassword);
            int dtVolSize =  DateraUtil.getVolumeSizeInGB(volumeInfo.getSize());
            String dtVolumeName = rest.createNextVolume(dtMetaData.appInstanceName, rest.defaultStorageName, dtVolSize);
            if(null == dtVolumeName || dtVolumeName.isEmpty())
            {
                throw new CloudRuntimeException("Datera : Could not create a volume");
            }
            int lunId = rest.getIntPart(dtVolumeName);
            AppInstanceInfo.VolumeInfo dtVolumeInfo =  rest.getVolumeInfo(dtMetaData.appInstanceName, rest.defaultStorageName, dtVolumeName);
/*
            String dtAppInstanceName = DateraUtil.generateAppInstanceName(dtMetaData.storagePoolName,volumeInfo.getUuid());


            //int volSize =  (int) volumeInfo.getSize().intValue();
            int volSize =  DateraUtil.getVolumeSizeInGB(volumeInfo.getSize());

            rest.createVolume(dtAppInstanceName, null,null,volSize,dtMetaData.replica,"allow_all",dtMetaData.networkPoolName);

            AppInstanceInfo.StorageInstance storageInfo = rest.getStorageInfo(dtAppInstanceName, rest.defaultStorageName);

            if(null == storageInfo || null == storageInfo.access || storageInfo.access.iqn == null || storageInfo.access.iqn.isEmpty())
            {
                throw new CloudRuntimeException("IQN not generated on the storage.");
            }

            if(storageInfo.access.ips == null || 0 == storageInfo.access.ips.size())
            {
                throw new CloudRuntimeException("Storage IP not generated for the storage.");
            }
*/
            StoragePoolVO storagePool = _storagePoolDao.findById(dataStore.getId());

            iqn = storagePool.getPath();
            s_logger.info(dtMetaData.appInstanceName+ " Storage IP and iqn  createAsync " +iqn + ", "+storagePool.getHostAddress());

            VolumeVO csVolume = _volumeDao.findById(volumeInfo.getId());

            csVolume.set_iScsiName("/"+iqn+"/"+lunId);
            //csVolume.setFolder(storageInfo.volumes.volume1.uuid);
            csVolume.setFolder(String.valueOf(lunId));
            csVolume.setPoolType(StoragePoolType.IscsiLUN);
            csVolume.setPoolId(storagePoolId);

            _volumeDao.update(csVolume.getId(), csVolume);


            updateVolumeDetails(csVolume.getId(), DateraUtil.getVolumeSizeInBytes(dtVolumeInfo.size));


            long capacityBytes = storagePool.getCapacityBytes();
            // getUsedBytes(StoragePool) will include the bytes of the newly created volume because
            // updateVolumeDetails(long, long) has already been called for this volume
            long usedBytes = getUsedBytes(storagePool);

            storagePool.setUsedBytes(usedBytes > capacityBytes ? capacityBytes : usedBytes);

            _storagePoolDao.update(storagePoolId, storagePool);

        } else {
            errMsg = "Invalid DataObjectType (" + dataObject.getType() + ") passed to createAsync";
        }

        // path = iqn
        // size is pulled from DataObject instance, if errMsg is null
        CreateCmdResult result = new CreateCmdResult(iqn, new Answer(null, errMsg == null, errMsg));

        result.setResult(errMsg);

        callback.complete(result);


    }


    private void updateVolumeDetails(long volumeId, long sfVolumeSize) {
        VolumeDetailVO volumeDetailVo = _volumeDetailsDao.findDetail(volumeId, DateraUtil.VOLUME_SIZE_NAME);

        if (volumeDetailVo == null || volumeDetailVo.getValue() == null) {
            volumeDetailVo = new VolumeDetailVO(volumeId, DateraUtil.VOLUME_SIZE_NAME, String.valueOf(sfVolumeSize), false);

            _volumeDetailsDao.persist(volumeDetailVo);
        }
    }

    @Override
    public void deleteAsync(DataStore dataStore, DataObject dataObject, AsyncCompletionCallback<CommandResult> callback) {
        String errMsg = null;
        if (dataObject.getType() == DataObjectType.VOLUME) {
            try {
                VolumeInfo volumeInfo = (VolumeInfo)dataObject;
                long volumeId = volumeInfo.getId();

                long storagePoolId = dataStore.getId();

                DateraUtil.DateraMetaData dtMetaData = DateraUtil.getDateraCred(storagePoolId, _storagePoolDetailsDao);

                //String dtAppInstanceName = DateraUtil.generateAppInstanceName(dtMetaData.storagePoolName,volumeInfo.getUuid());

                DateraRestClient rest = new DateraRestClient(dtMetaData.mangementIP, dtMetaData.managementPort, dtMetaData.managementUserName, dtMetaData.managementPassword);
                rest.setAdminState(dtMetaData.appInstanceName, false);
                String volumeName = DateraUtil.constructVolumeName(volumeInfo.getFolder());

                rest.deleteVolume(dtMetaData.appInstanceName, rest.defaultStorageName, volumeName);
                rest.setAdminState(dtMetaData.appInstanceName, true);


                _volumeDetailsDao.removeDetails(volumeId);

                StoragePoolVO storagePool = _storagePoolDao.findById(storagePoolId);

                // getUsedBytes(StoragePool) will not include the volume to delete because it has already been deleted by this point
                long usedBytes = getUsedBytes(storagePool);

                storagePool.setUsedBytes(usedBytes < 0 ? 0 : usedBytes);

                _storagePoolDao.update(storagePoolId, storagePool);
            }
            catch (Exception ex) {
                s_logger.debug(DateraUtil.LOG_PREFIX + "Failed to delete Datera volume", ex);

                errMsg = ex.getMessage();
            }
        } else {
            errMsg = "Invalid DataObjectType (" + dataObject.getType() + ") passed to deleteAsync";
        }

        CommandResult result = new CommandResult();

        result.setResult(errMsg);

        callback.complete(result);
    }

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        return false;
    }

    @Override
    public void takeSnapshot(SnapshotInfo snapshotInfo, AsyncCompletionCallback<CreateCmdResult> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void revertSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CommandResult> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resize(DataObject dataObject, AsyncCompletionCallback<CreateCmdResult> callback) {
        String iqn = null;
        String errMsg = null;
/*
        if (dataObject.getType() == DataObjectType.VOLUME) {
            VolumeInfo volumeInfo = (VolumeInfo)dataObject;
            iqn = volumeInfo.get_iScsiName();
            long storagePoolId = volumeInfo.getPoolId();
            long dtVolumeId = Long.parseLong(volumeInfo.getFolder());
            ResizeVolumePayload payload = (ResizeVolumePayload)volumeInfo.getpayload();

            DateraUtil.DateraConnection dtConnection = DateraUtil.getDateraConnection(storagePoolId, _storagePoolDetailsDao);
            DateraUtil.DateraVolume dtVolume = DateraUtil.getDateraVolume(dtConnection, dtVolumeId);

            verifySufficientIopsForStoragePool(storagePoolId, volumeInfo.getId(), payload.newMinIops);

            DateraUtil.modifyDateraVolume(dtConnection, dtVolumeId, dtVolume.getTotalSize(), NumberFormat.getInstance().format(payload.newSize),
                    payload.newMinIops, payload.newMaxIops, getDefaultBurstIops(storagePoolId, payload.newMaxIops));

            VolumeVO volume = _volumeDao.findById(volumeInfo.getId());

            volume.setMinIops(payload.newMinIops);
            volume.setMaxIops(payload.newMaxIops);

            _volumeDao.update(volume.getId(), volume);

            // DateraUtil.VOLUME_SIZE was introduced in 4.5.
            // To be backward compatible with releases prior to 4.5, call updateVolumeDetails here.
            // That way if DateraUtil.VOLUME_SIZE wasn't put in the volume_details table when the
            // volume was initially created, it can be placed in volume_details if the volume is resized.
            updateVolumeDetails(volume.getId(), dtVolume.getTotalSize());
        } else {
            errMsg = "Invalid DataObjectType (" + dataObject.getType() + ") passed to resize";
        }
*/
        CreateCmdResult result = new CreateCmdResult(iqn, new Answer(null, errMsg == null, errMsg));

        result.setResult(errMsg);

        callback.complete(result);
    }

    private void verifySufficientIopsForStoragePool(long storagePoolId, long volumeId, long newMinIops) {
        StoragePoolVO storagePool = _storagePoolDao.findById(storagePoolId);
        VolumeVO volume = _volumeDao.findById(volumeId);

        long currentMinIops = volume.getMinIops();
        long diffInMinIops = newMinIops - currentMinIops;

        // if the desire is for more IOPS
        if (diffInMinIops > 0) {
            long usedIops = _capacityMgr.getUsedIops(storagePool);
            long capacityIops = storagePool.getCapacityIops();

            if (usedIops + diffInMinIops > capacityIops) {
                throw new CloudRuntimeException("Insufficient number of IOPS available in this storage pool");
            }
        }
    }
}
