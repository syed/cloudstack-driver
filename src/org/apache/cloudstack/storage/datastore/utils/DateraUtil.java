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
package org.apache.cloudstack.storage.datastore.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.utils.exception.CloudRuntimeException;

public class DateraUtil {
    public static final String PROVIDER_NAME = "Datera";

    public static final String LOG_PREFIX = "Datera: ";

    public static final String MANAGEMENT_IP = "mgmtIP";

    public static final String MANAGEMENT_PORT = "mgmtPort";

    public static final String MANAGEMENT_USERNAME = "mgmtUserName";
    public static final String MANAGEMENT_PASSWORD = "mgmtPassword";

    public static final String NETWORK_POOL_NAME = "networkPoolName";
    public static final String MAX_TOTAL_IOPS = "maxTotalIOPs";
    public static final String MAX_READ_IOPS = "maxReadIOPs";
    public static final String MAX_WRITE_IOPS = "maxWriteIOPs";
    public static final String MAX_TOTAL_BANDWIDTH = "maxTotalBandwidth";
    public static final String MAX_READ_BANDWIDTH = "maxReadBandwidth";
    public static final String MAX_WRITE_BANDWIDTH = "maxWriteBandwidth";

/*    // these three variables should only be used for the Datera plug-in with the name DateraUtil.PROVIDER_NAME
    public static final String CLUSTER_DEFAULT_MIN_IOPS = "clusterDefaultMinIops";
    public static final String CLUSTER_DEFAULT_MAX_IOPS = "clusterDefaultMaxIops";
    public static final String CLUSTER_DEFAULT_BURST_IOPS_PERCENT_OF_MAX_IOPS = "clusterDefaultBurstIopsPercentOfMaxIops";

    // these three variables should only be used for the Datera plug-in with the name DateraUtil.SHARED_PROVIDER_NAME
    public static final String MIN_IOPS = "minIops";
    public static final String MAX_IOPS = "maxIops";
    public static final String BURST_IOPS = "burstIops";

    public static final String ACCOUNT_ID = "accountId";
    public static final String VOLUME_ID = "volumeId";
    public static final String SNAPSHOT_ID = "snapshotId";
    public static final String CLONE_ID = "cloneId";

    public static final String VOLUME_SIZE = "sfVolumeSize";
    public static final String SNAPSHOT_SIZE = "sfSnapshotSize";

    public static final String SNAPSHOT_STORAGE_POOL_ID = "sfSnapshotStoragePoolId";

    public static final String CHAP_INITIATOR_USERNAME = "chapInitiatorUsername";
    public static final String CHAP_INITIATOR_SECRET = "chapInitiatorSecret";

    public static final String CHAP_TARGET_USERNAME = "chapTargetUsername";
    public static final String CHAP_TARGET_SECRET = "chapTargetSecret";

    public static final String DATACENTER = "datacenter";

    public static final String DATASTORE_NAME = "datastoreName";
    public static final String IQN = "iqn";
*/
    public static final long MAX_IOPS_PER_VOLUME = 100000;

    private static final int DEFAULT_MANAGEMENT_PORT = 7718;
    private static final int DEFAULT_STORAGE_PORT = 3260;

    public static boolean hostsSupport_iScsi(List<HostVO> hosts) {
        if (hosts == null || hosts.size() == 0) {
            return false;
        }

        for (Host host : hosts) {
            if (host == null || host.getStorageUrl() == null || host.getStorageUrl().trim().length() == 0 || !host.getStorageUrl().startsWith("iqn")) {
                return false;
            }
        }

        return true;
    }

    public static String[] getNewHostIqns(String[] currentIqns, String[] newIqns) {
        List<String> lstIqns = new ArrayList<String>();

        if (currentIqns != null) {
            for (String currentIqn : currentIqns) {
                lstIqns.add(currentIqn);
            }
        }

        if (newIqns != null) {
            for (String newIqn : newIqns) {
                if (!lstIqns.contains(newIqn)) {
                    lstIqns.add(newIqn);
                }
            }
        }

        return lstIqns.toArray(new String[0]);
    }

    public static long[] getNewVolumeIds(long[] volumeIds, long volumeIdToAddOrRemove, boolean add) {
        if (add) {
            return getNewVolumeIdsAdd(volumeIds, volumeIdToAddOrRemove);
        }

        return getNewVolumeIdsRemove(volumeIds, volumeIdToAddOrRemove);
    }

    private static long[] getNewVolumeIdsAdd(long[] volumeIds, long volumeIdToAdd) {
        List<Long> lstVolumeIds = new ArrayList<Long>();

        if (volumeIds != null) {
            for (long volumeId : volumeIds) {
                lstVolumeIds.add(volumeId);
            }
        }

        if (lstVolumeIds.contains(volumeIdToAdd)) {
            return volumeIds;
        }

        lstVolumeIds.add(volumeIdToAdd);

        return convertArray(lstVolumeIds);
    }

    private static long[] getNewVolumeIdsRemove(long[] volumeIds, long volumeIdToRemove) {
        List<Long> lstVolumeIds = new ArrayList<Long>();

        if (volumeIds != null) {
            for (long volumeId : volumeIds) {
                lstVolumeIds.add(volumeId);
            }
        }

        lstVolumeIds.remove(volumeIdToRemove);

        return convertArray(lstVolumeIds);
    }

    private static long[] convertArray(List<Long> items) {
        if (items == null) {
            return new long[0];
        }

        long[] outArray = new long[items.size()];

        for (int i = 0; i < items.size(); i++) {
            Long value = items.get(i);

            outArray[i] = value;
        }

        return outArray;
    }

    public static String[] getIqnsFromHosts(List<? extends Host> hosts) {
        if (hosts == null || hosts.size() == 0) {
            throw new CloudRuntimeException("There do not appear to be any hosts in this cluster.");
        }

        List<String> lstIqns = new ArrayList<String>();

        for (Host host : hosts) {
            lstIqns.add(host.getStorageUrl());
        }

        return lstIqns.toArray(new String[0]);
    }

    private static List<String> getStringArrayAsLowerCaseStringList(String[] aString) {
        List<String> lstLowerCaseString = new ArrayList<String>();

        if (aString != null) {
            for (String str : aString) {
                if (str != null) {
                    lstLowerCaseString.add(str.toLowerCase());
                }
            }
        }

        return lstLowerCaseString;
    }

    private static boolean isSuccess(int iCode) {
        return iCode >= 200 && iCode < 300;
    }

    public static String getModifiedUrl(String originalUrl) {
        StringBuilder sb = new StringBuilder();

        String delimiter = ";";

        StringTokenizer st = new StringTokenizer(originalUrl, delimiter);

        while (st.hasMoreElements()) {
            String token = st.nextElement().toString().toUpperCase();

            if (token.startsWith(DateraUtil.MANAGEMENT_IP.toUpperCase()) ) {
                sb.append(token).append(delimiter);
            }
        }

        String modifiedUrl = sb.toString();
        int lastIndexOf = modifiedUrl.lastIndexOf(delimiter);

        if (lastIndexOf == (modifiedUrl.length() - delimiter.length())) {
            return modifiedUrl.substring(0, lastIndexOf);
        }

        return modifiedUrl;
    }

    public static String getManagementVip(String url) {
        return getIP(DateraUtil.MANAGEMENT_IP, url);
    }


    public static int getManagementPort(String url) {
        return getPort(DateraUtil.MANAGEMENT_IP, url, DEFAULT_MANAGEMENT_PORT);
    }

    private static String getIP(String keyToMatch, String url) {
        String delimiter = ":";

        String ip = getValue(keyToMatch, url);

        int index = ip.indexOf(delimiter);

        if (index != -1) {
            return ip.substring(0, index);
        }

        return ip;
    }

    private static int getPort(String keyToMatch, String url, int defaultPortNumber) {
        String delimiter = ":";

        String ip = getValue(keyToMatch, url);

        int index = ip.indexOf(delimiter);

        int portNumber = defaultPortNumber;

        if (index != -1) {
            String port = ip.substring(index + delimiter.length());

            try {
                portNumber = Integer.parseInt(port);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid URL format (port is not an integer)");
            }
        }

        return portNumber;
    }

    public static String getValue(String keyToMatch, String url) {
        return getValue(keyToMatch, url, true);
    }

    public static String getValue(String keyToMatch, String url, boolean throwExceptionIfNotFound) {
        String delimiter1 = ";";
        String delimiter2 = "=";

        StringTokenizer st = new StringTokenizer(url, delimiter1);

        while (st.hasMoreElements()) {
            String token = st.nextElement().toString();

            int index = token.indexOf(delimiter2);

            if (index == -1) {
                throw new RuntimeException("Invalid URL format");
            }

            String key = token.substring(0, index);

            if (key.equalsIgnoreCase(keyToMatch)) {
                String valueToReturn = token.substring(index + delimiter2.length());

                return valueToReturn;
            }
        }

        if (throwExceptionIfNotFound) {
            throw new RuntimeException("Key not found in URL");
        }

        return null;
    }
}
