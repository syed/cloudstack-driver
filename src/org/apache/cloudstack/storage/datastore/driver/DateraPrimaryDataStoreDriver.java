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

import java.text.NumberFormat;
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
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.utils.DateraUtil;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.capacity.CapacityManager;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.ResizeVolumePayload;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.user.AccountDetailVO;
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

    private DateraUtil.DateraAccount createDateraAccount(DateraUtil.DateraConnection dtConnection, String dtAccountName) {
        long accountNumber = DateraUtil.createDateraAccount(dtConnection, dtAccountName);

        return DateraUtil.getDateraAccountById(dtConnection, accountNumber);
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

        long dtVolumeId = Long.parseLong(volumeInfo.getFolder());
        long clusterId = host.getClusterId();
        long storagePoolId = dataStore.getId();

        ClusterDetailsVO clusterDetail = _clusterDetailsDao.findDetail(clusterId, DateraUtil.getVagKey(storagePoolId));

        String vagId = clusterDetail != null ? clusterDetail.getValue() : null;

        List<HostVO> hosts = _hostDao.findByClusterId(clusterId);

        if (!DateraUtil.hostsSupport_iScsi(hosts)) {
            return false;
        }

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
    }

    private long getDefaultMinIops(long storagePoolId) {
        StoragePoolDetailVO storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePoolId, DateraUtil.CLUSTER_DEFAULT_MIN_IOPS);

        String clusterDefaultMinIops = storagePoolDetail.getValue();

        return Long.parseLong(clusterDefaultMinIops);
    }

    private long getDefaultMaxIops(long storagePoolId) {
        StoragePoolDetailVO storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePoolId, DateraUtil.CLUSTER_DEFAULT_MAX_IOPS);

        String clusterDefaultMaxIops = storagePoolDetail.getValue();

        return Long.parseLong(clusterDefaultMaxIops);
    }

    private long getDefaultBurstIops(long storagePoolId, long maxIops) {
        StoragePoolDetailVO storagePoolDetail = _storagePoolDetailsDao.findDetail(storagePoolId, DateraUtil.CLUSTER_DEFAULT_BURST_IOPS_PERCENT_OF_MAX_IOPS);

        String clusterDefaultBurstIopsPercentOfMaxIops = storagePoolDetail.getValue();

        float fClusterDefaultBurstIopsPercentOfMaxIops = Float.parseFloat(clusterDefaultBurstIopsPercentOfMaxIops);

        return (long)(maxIops * fClusterDefaultBurstIopsPercentOfMaxIops);
    }

    private DateraUtil.DateraVolume createDateraVolume(DateraUtil.DateraConnection dtConnection, VolumeInfo volumeInfo, long dtAccountId) {
        long storagePoolId = volumeInfo.getDataStore().getId();

        final Iops iops;

        Long minIops = volumeInfo.getMinIops();
        Long maxIops = volumeInfo.getMaxIops();

        if (minIops == null || minIops <= 0 || maxIops == null || maxIops <= 0) {
            long defaultMaxIops = getDefaultMaxIops(storagePoolId);

            iops = new Iops(getDefaultMinIops(storagePoolId), defaultMaxIops, getDefaultBurstIops(storagePoolId, defaultMaxIops));
        } else {
            iops = new Iops(volumeInfo.getMinIops(), volumeInfo.getMaxIops(), getDefaultBurstIops(storagePoolId, volumeInfo.getMaxIops()));
        }

        long volumeSize = getVolumeSizeIncludingHypervisorSnapshotReserve(volumeInfo, _storagePoolDao.findById(storagePoolId));

        long dtVolumeId = DateraUtil.createDateraVolume(dtConnection, DateraUtil.getDateraVolumeName(volumeInfo.getName()), dtAccountId, volumeSize, true,
                NumberFormat.getInstance().format(volumeInfo.getSize()), iops.getMinIops(), iops.getMaxIops(), iops.getBurstIops());

        return DateraUtil.getDateraVolume(dtConnection, dtVolumeId);
    }

    @Override
    public long getUsedBytes(StoragePool storagePool) {
        long usedSpace = 0;

        List<VolumeVO> lstVolumes = _volumeDao.findByPoolId(storagePool.getId(), null);

        if (lstVolumes != null) {
            for (VolumeVO volume : lstVolumes) {
                VolumeDetailVO volumeDetail = _volumeDetailsDao.findDetail(volume.getId(), DateraUtil.VOLUME_SIZE);

                if (volumeDetail != null && volumeDetail.getValue() != null) {
                    long volumeSize = Long.parseLong(volumeDetail.getValue());

                    usedSpace += volumeSize;
                }
            }
        }

        return usedSpace;
    }

    @Override
    public long getVolumeSizeIncludingHypervisorSnapshotReserve(Volume volume, StoragePool pool) {
        long volumeSize = volume.getSize();
        Integer hypervisorSnapshotReserve = volume.getHypervisorSnapshotReserve();

        if (hypervisorSnapshotReserve != null) {
            if (hypervisorSnapshotReserve < 50) {
                hypervisorSnapshotReserve = 50;
            }

            volumeSize += volumeSize * (hypervisorSnapshotReserve / 100f);
        }

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

    private void deleteDateraVolume(DateraUtil.DateraConnection dtConnection, VolumeInfo volumeInfo)
    {
        Long storagePoolId = volumeInfo.getPoolId();

        if (storagePoolId == null) {
            return; // this volume was never assigned to a storage pool, so no SAN volume should exist for it
        }

        long dtVolumeId = Long.parseLong(volumeInfo.getFolder());

        DateraUtil.deleteDateraVolume(dtConnection, dtVolumeId);
    }

    @Override
    public void createAsync(DataStore dataStore, DataObject dataObject, AsyncCompletionCallback<CreateCmdResult> callback) {
        String iqn = null;
        String errMsg = null;

        if (dataObject.getType() == DataObjectType.VOLUME) {
            VolumeInfo volumeInfo = (VolumeInfo)dataObject;
            AccountVO account = _accountDao.findById(volumeInfo.getAccountId());
            String dtAccountName = DateraUtil.getDateraAccountName(account.getUuid(), account.getAccountId());

            long storagePoolId = dataStore.getId();

            DateraUtil.DateraConnection dtConnection = DateraUtil.getDateraConnection(storagePoolId, _storagePoolDetailsDao);

            AccountDetailVO accountDetail = DateraUtil.getAccountDetail(volumeInfo.getAccountId(), storagePoolId, _accountDetailsDao);

            if (accountDetail == null || accountDetail.getValue() == null) {
                DateraUtil.DateraAccount dtAccount = DateraUtil.getDateraAccount(dtConnection, dtAccountName);

                if (dtAccount == null) {
                    dtAccount = createDateraAccount(dtConnection, dtAccountName);
                }

                //DateraUtil.updateCsDbWithDateraIopsInfo(storagePoolId, primaryDataStoreDao, storagePoolDetailsDao, minIops, maxIops, burstIops)DbWithDateraAccountInfo(account.getId(), sfAccount, storagePoolId, _accountDetailsDao);

                accountDetail = DateraUtil.getAccountDetail(volumeInfo.getAccountId(), storagePoolId, _accountDetailsDao);
            }

            long dtAccountId = Long.parseLong(accountDetail.getValue());

            DateraUtil.DateraVolume dtVolume = createDateraVolume(dtConnection, volumeInfo, dtAccountId);

            iqn = dtVolume.getIqn();

            VolumeVO volume = _volumeDao.findById(volumeInfo.getId());

            volume.set_iScsiName(iqn);
            volume.setFolder(String.valueOf(dtVolume.getId()));
            volume.setPoolType(StoragePoolType.IscsiLUN);
            volume.setPoolId(storagePoolId);

            _volumeDao.update(volume.getId(), volume);

            updateVolumeDetails(volume.getId(), dtVolume.getTotalSize());

            StoragePoolVO storagePool = _storagePoolDao.findById(dataStore.getId());

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

    private void updateVolumeDetails(long volumeId, long dtVolumeSize) {
        VolumeDetailVO volumeDetailVo = _volumeDetailsDao.findDetail(volumeId, DateraUtil.VOLUME_SIZE);

        if (volumeDetailVo == null || volumeDetailVo.getValue() == null) {
            volumeDetailVo = new VolumeDetailVO(volumeId, DateraUtil.VOLUME_SIZE, String.valueOf(dtVolumeSize), false);

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

                DateraUtil.DateraConnection dtConnection = DateraUtil.getDateraConnection(storagePoolId, _storagePoolDetailsDao);

                deleteDateraVolume(dtConnection, volumeInfo);

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
