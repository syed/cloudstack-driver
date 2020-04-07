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

package org.apache.cloudstack.storage.datastore.util;

import com.cloud.utils.StringUtils;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class DateraObject {

    public static final String DEFAULT_CREATE_MODE = "cloudstack";
    public static final String DEFAULT_STORAGE_NAME = "storage-1";
    public static final String DEFAULT_VOLUME_NAME = "volume-1";
    public static final String DEFAULT_ACL = "deny_all";
    public static final String DEFAULT_STORAGE_FORCE_BOOLEAN = "true";

    public enum AppState {
        ONLINE, OFFLINE;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    public enum DateraOperation {
        ADD, REMOVE;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    public enum DateraErrorTypes {
        PermissionDeniedError, InvalidRouteError, AuthFailedError, ValidationFailedError, InvalidRequestError,
        NotFoundError, NotConnectedError, InvalidSessionKeyError, DatabaseError, InternalError,ConflictError;

        public boolean equals(DateraError err) {
            return this.name().equals(err.getName());
        }
    }

    public static class DateraApiResponse {
        public String path;
        public String version;
        public String tenant;
        public String data;

        public String getResponseObjectString() {
            return data;
        }
    }

    public static class DateraConnection {

        private int managementPort;
        private String managementIp;
        private String username;
        private String password;

        public DateraConnection(String managementIp, int managementPort, String username, String password) {
            this.managementPort = managementPort;
            this.managementIp = managementIp;
            this.username = username;
            this.password = password;
        }

        public int getManagementPort() {
            return managementPort;
        }

        public String getManagementIp() {
            return managementIp;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }

    public static class DateraLogin {

        private final String name;
        private final String password;

        public DateraLogin(String username, String password) {
            this.name = username;
            this.password = password;
        }
    }

    public static class DateraLoginResponse {

        private String key;

        public String getKey() {
            return key;
        }
    }

    public class Access {
        private String iqn;
        private List<String> ips;

        public Access(String iqn, List<String> ips) {
            this.iqn = iqn;
            this.ips = ips;
        }

        public String getIqn() {
            return iqn;
        }
    }

    public static class PerformancePolicy {

        @SerializedName("total_iops_max")
        private Integer totalIops;

        public PerformancePolicy(int totalIops) {
            this.totalIops = totalIops;
        }

        public Integer getTotalIops() {
            return totalIops;
        }
    }

    public static class Volume {

        private String name;
        private String path;
        private Integer size;

        @SerializedName("replica_count")
        private Integer replicaCount;

        @SerializedName("performance_policy")
        private PerformancePolicy performancePolicy;

        @SerializedName("placement_mode")
        private String placementMode;

        @SerializedName("op_state")
        private String opState;

        public Volume(int size, int totalIops, int replicaCount) {
            this.name = DEFAULT_VOLUME_NAME;
            this.size = size;
            this.replicaCount = replicaCount;
            this.performancePolicy = new PerformancePolicy(totalIops);
        }

        public Volume(int size, int totalIops, int replicaCount, String placementMode) {
            this.name = DEFAULT_VOLUME_NAME;
            this.size = size;
            this.replicaCount = replicaCount;
            this.performancePolicy = new PerformancePolicy(totalIops);
            this.placementMode = placementMode;
        }

        public Volume(Integer newSize) {
            this.size = newSize;
        }

        public Volume(String newPlacementMode) {
            this.placementMode = newPlacementMode;
        }

        public Volume(String path, String placementMode) {
            this.path=path;
            this.placementMode = placementMode;
        }

        public PerformancePolicy getPerformancePolicy() {
            return performancePolicy;
        }

        public int getSize() {
            return size;
        }

        public String getPlacementMode() {
            return placementMode;
        }

        public String getPath() {
            return path;
        }

        public String getOpState() {
            return opState;
        }
    }

    public static class IpPool {
        private String name;
        private String path;

        public IpPool(String name) {
            name = name;
            path = "/access_network_ip_pools/" + name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class StorageInstance {

        private final String name = DEFAULT_STORAGE_NAME;
        private List<Volume> volumes;
        private Access access;
        private String force;

        @SerializedName("ip_pool")
        private IpPool ipPool;

        public StorageInstance(int size, int totalIops, int replicaCount) {
            Volume volume = new Volume(size, totalIops, replicaCount);
            volumes = new ArrayList<>();
            volumes.add(volume);
        }

        public StorageInstance(int size, int totalIops, int replicaCount, String placementMode, String ipPoolName) {
            Volume volume = new Volume(size, totalIops, replicaCount, placementMode);
            volumes = new ArrayList<>();
            volumes.add(volume);
            ipPool = new IpPool(ipPoolName);
        }

        public Access getAccess() {
            return access;
        }

        public Volume getVolume() {
            return volumes.get(0);
        }

        public int getSize() {
            return getVolume().getSize();
        }

        public String getForce() {
            return this.force;
        }

    }

    public static class AppInstance {

        private String name;

        @SerializedName("descr")
        private String description;

        @SerializedName("access_control_mode")
        private String accessControlMode;

        @SerializedName("create_mode")
        private String createMode;

        @SerializedName("storage_instances")
        private List<StorageInstance> storageInstances;

        @SerializedName("clone_volume_src")
        private Volume cloneVolumeSrc;

        @SerializedName("clone_snapshot_src")
        private VolumeSnapshot cloneSnapshotSrc;

        @SerializedName("admin_state")
        private String adminState;
        private Boolean force;

       public AppInstance(String name, String description, int size, int totalIops, int replicaCount) {
            this.name = name;
            this.description = description;
            StorageInstance storageInstance = new StorageInstance(size, totalIops, replicaCount);
            this.storageInstances = new ArrayList<>();
            this.storageInstances.add(storageInstance);
            this.accessControlMode = DEFAULT_ACL;
            this.createMode = DEFAULT_CREATE_MODE;
        }

        public AppInstance(String name, String description, int size, int totalIops, int replicaCount, String placementMode,
                String ipPool) {
            this.name = name;
            this.description = description;
            StorageInstance storageInstance = new StorageInstance(size, totalIops, replicaCount, placementMode, ipPool);

            this.storageInstances = new ArrayList<>();
            this.storageInstances.add(storageInstance);

            this.accessControlMode = DEFAULT_ACL;
            this.createMode = DEFAULT_CREATE_MODE;
        }

        public AppInstance(AppState state) {
            this.adminState = state.toString();
            this.force = true;
        }

        public AppInstance(String name, String description, Volume cloneSrc) {
            this.name = name;
            this.description = description;
            this.cloneVolumeSrc = cloneSrc;
        }

        public AppInstance(String name, String description, VolumeSnapshot cloneSrc) {
            this.name = name;
            this.description = description;
            this.cloneSnapshotSrc = cloneSrc;
        }


        public String getIqn() {
            StorageInstance storageInstance = storageInstances.get(0);
            return storageInstance.getAccess().getIqn();
        }

        public int getTotalIops() {
            StorageInstance storageInstance = storageInstances.get(0);
            return storageInstance.getVolume().getPerformancePolicy().getTotalIops();
        }

        public String getName() {
            return name;
        }

        public int getSize() {
            StorageInstance storageInstance = storageInstances.get(0);
            return storageInstance.getSize();
        }

        public String getVolumePath() {
            StorageInstance storageInstance = storageInstances.get(0);
            return storageInstance.getVolume().getPath();
        }

        public String getVolumeOpState() {
            StorageInstance storageInstance = storageInstances.get(0);
            return storageInstance.getVolume().getOpState();
        }
    }

    public static class AccessNetworkIpPool {
        @SerializedName("ip_pool")
        private IpPool ipPool;


        public AccessNetworkIpPool(String ipPool) {
            this.ipPool = new IpPool(ipPool);
        }
    }

    public static class Initiator {

        private String id; // IQN
        private String name;
        private String path;
        private String op;
        private boolean force;

        public Initiator(String name, String id) {
            this.id = id;
            this.name = name;
        }

        public Initiator(String path, DateraOperation op) {
            this.path = path;
            this.op = op.toString();
        }

        public String getPath() {
            return path;
        }
    }

    public static class InitiatorGroup {

        private String name;
        private List<Initiator> members;
        private String path;
        private String op;
        private boolean force;

        public InitiatorGroup(String name, List<Initiator> members) {
            this.name = name;
            this.members = members;
            this.force = true;
        }

        public InitiatorGroup(String path, DateraOperation op) {
            this.path = path;
            this.op = op.toString();
            this.force = true;
        }

        public String getPath() {
            return path;
        }

        public String getName() {
            return name;
        }

        public List<Initiator> getMembers() {
            return members;
        }
    }

    public static class VolumeSnapshot {

        private String uuid;
        private String timestamp;
        private String path;

        @SerializedName("op_state")
        private String opState;

        VolumeSnapshot() {
        }

        VolumeSnapshot(String path) {
            this.path = path;
        }


        public String getTimestamp() {
            return timestamp;
        }

        public String getOpState() {
            return opState;
        }

        public String getPath() {
            return path;
        }
    }

    public static class VolumeSnapshotRestore {

        @SerializedName("restore_point")
        private String restorePoint;

        VolumeSnapshotRestore(String restorePoint) {
            this.restorePoint = restorePoint;
        }
    }

    public static class DateraError extends Exception {

        private String name;
        private int code;
        private List<String> errors;
        private String message;

        public DateraError(String name, int code, List<String> errors, String message) {
            this.name = name;
            this.code = code;
            this.errors = errors;
            this.message = message;
        }

        public List<String> getErrors() {
            return errors;
        }

        public boolean isError() {
            return message != null && name.endsWith("Error");
        }

        public String getMessage() {

            String errMesg = name + "\n";
            if (message != null) {
                errMesg += message + "\n";
            }

            if (errors != null) {
                errMesg += StringUtils.join(errors, "\n");

            }

            return errMesg;
        }

        public String getName() {
            return name;
        }
    }
}
