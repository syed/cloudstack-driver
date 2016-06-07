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
package org.apache.cloudstack.storage.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.cloudstack.storage.datastore.utils.AppInstanceInfo;
import org.apache.cloudstack.storage.datastore.utils.AppInstanceInfo.StorageInstance;
import org.apache.cloudstack.storage.datastore.utils.DateraRestClient;
import org.apache.cloudstack.storage.datastore.utils.DateraUtil;
import org.apache.cloudstack.storage.datastore.utils.DateraRestClientMgr;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class DateraRestMgrMockTest {
	
    private DateraRestClient rest = null;
    private DateraRestClientMgr restMgr = null;
    
    @Before
    public void init() {
    	rest  = mock(DateraRestClient.class);
    	restMgr = DateraRestClientMgr.getInstance();
    }
    
    @Test
    public void testCreateVolume() {
    	final String appName = "test_app_inst_1";
    	when(rest.isAppInstanceExists(appName)).thenReturn(false);
    	List<String> netPool = new ArrayList();
    	netPool.add("default");
    	when(rest.enumerateNetworkPool()).thenReturn(netPool);

    	int dtVolSize = DateraUtil.getVolumeSizeInGB(DateraCommon.DEFAULT_CAPACITY_BYTES);
        when(rest.createVolume(appName, null, null, dtVolSize, DateraCommon.DEFAULT_REPLICA, 
        		DateraRestClient.ACCESS_CONTROL_MODE_ALLOW_ALL, DateraCommon.DEFAULT_NETWORK_POOL_NAME)).thenReturn(new AppInstanceInfo());

        when(rest.setQos(appName, rest.defaultStorageName, rest.defaultVolumeName, DateraCommon.DEFAULT_CAPACITY_IOPS)).thenReturn(true);
        
        AppInstanceInfo.VolumeInfo volInfo = new AppInstanceInfo().new VolumeInfo();
        volInfo.name = rest.defaultVolumeName;
        volInfo.opState = "available";

        when(rest.getVolumeInfo(appName, rest.defaultStorageName, rest.defaultVolumeName)).thenReturn(volInfo);
    	
        when(rest.setAdminState(appName, false)).thenReturn(true);
        when(rest.deleteAppInstance(appName)).thenReturn(true);
        when(rest.getStorageInfo(appName, rest.defaultStorageName)).thenReturn(null);
    	
    	restMgr.createVolume(rest, DateraCommon.MANAGEMENT_IP, DateraCommon.PORT, DateraCommon.USERNAME, 
    			DateraCommon.PASSWORD, appName, DateraCommon.DEFAULT_NETWORK_POOL_NAME, DateraCommon.DEFAULT_CAPACITY_BYTES, 
    			DateraCommon.DEFAULT_REPLICA, DateraCommon.DEFAULT_CAPACITY_IOPS);
    	
    }
}