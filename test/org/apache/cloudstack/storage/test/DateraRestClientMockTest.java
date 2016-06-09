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
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.cloudstack.storage.datastore.utils.DateraModel;
import org.apache.cloudstack.storage.datastore.utils.DateraRestClient;
import org.apache.cloudstack.storage.datastore.utils.DateraUtil;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DateraRestClientMockTest {
	
    private DateraRestClient client = null;
    
    @Before
    public void init() {
    	client  = mock(DateraRestClient.class);
    }
    
    @Test
    public void testCreateAppInstance() {
    	
    	final String appInstanceName = DateraUtil.generateAppInstanceName("test-appInst", UUID.randomUUID().toString());
    	
    	when(client.createAppInstance(appInstanceName)).thenReturn(true);
    	assertTrue(client.createAppInstance(appInstanceName));
    	
    	// Creating App instance with existing App instance name
    	when(client.createAppInstance(appInstanceName)).thenReturn(false);
    	assertFalse(client.createAppInstance(appInstanceName));
    }
    
    @Test
    public void testDeleteAppInstance() {

    	final String appInstanceName = DateraUtil.generateAppInstanceName("test-appInst", UUID.randomUUID().toString());
    	
    	when(client.createAppInstance(appInstanceName)).thenReturn(true);
    	assertTrue(client.createAppInstance(appInstanceName));
    	
    	when(client.deleteAppInstance(appInstanceName)).thenReturn(false);
    	// Deleting Application instance while it is online
    	assertFalse(client.deleteAppInstance(appInstanceName));
    	
    	when(client.setAdminState(appInstanceName, false)).thenReturn(true);
    	when(client.deleteAppInstance(appInstanceName)).thenReturn(true);

    	assertTrue(client.setAdminState(appInstanceName, false));
    	assertTrue(client.deleteAppInstance(appInstanceName));
    }

    @Test
    public void testCreateVolume(){

    	final String appInstanceName = DateraUtil.generateAppInstanceName("test-appInst", UUID.randomUUID().toString());
    	final String storageInst = "storage-1";
    	final String volumeName1 = "volume-1";
    	
    	when(client.createAppInstance(appInstanceName)).thenReturn(true);
    	when(client.createStorageInstance(appInstanceName, storageInst, "default")).thenReturn(true);
    	when(client.createVolume(appInstanceName, DateraModel.defaultStorageName, volumeName1, 1, 3)).thenReturn(true);
    	
    	assertTrue(client.createAppInstance(appInstanceName));
    	assertTrue(client.createStorageInstance(appInstanceName, storageInst, "default"));
    	assertTrue(client.createVolume(appInstanceName, DateraModel.defaultStorageName, volumeName1, 1, 3));
    	
    	// Volume creation with same name not allowed
    	when(client.createVolume(appInstanceName, DateraModel.defaultStorageName, volumeName1, 2, 2)).thenReturn(false);
    	assertFalse(client.createVolume(appInstanceName, DateraModel.defaultStorageName, volumeName1, 2, 2));
    }

    @Test
    public void testDeleteVolume(){
    	
    	final String appInstanceName = DateraUtil.generateAppInstanceName("test-appInst", UUID.randomUUID().toString());
    	final String storageInst = "storage-1";
    	final String volumeName = "volume-1";
    	
    	when(client.createAppInstance(appInstanceName)).thenReturn(true);
    	when(client.createStorageInstance(appInstanceName, storageInst, "default")).thenReturn(true);
    	when(client.createVolume(appInstanceName, storageInst, volumeName, 2, 3)).thenReturn(true);
    	
    	assertTrue(client.createAppInstance(appInstanceName));
    	assertTrue(client.createStorageInstance(appInstanceName, storageInst, "default"));
    	assertTrue(client.createVolume(appInstanceName, DateraModel.defaultStorageName, volumeName, 2, 3));

    	// Volume deletion is not allowed while Application instance is online
    	when(client.deleteVolume(appInstanceName, storageInst, volumeName)).thenReturn(false);
    	assertFalse(client.deleteVolume(appInstanceName, storageInst, volumeName));
    	
    	when(client.setAdminState(appInstanceName, false)).thenReturn(true);
    	when(client.deleteVolume(appInstanceName, storageInst, volumeName)).thenReturn(true);

    	//Making App instance offline
    	assertTrue(client.setAdminState(appInstanceName, false));
    	assertTrue(client.deleteVolume(appInstanceName, storageInst, volumeName));
    }
    
    @Test
    public void testCreateStorageInstance() {
    	final String appInstanceName = DateraUtil.generateAppInstanceName("test-appInst", UUID.randomUUID().toString());
    	final String storageInst = "storage-1";
    	
    	when(client.createStorageInstance(appInstanceName, storageInst, "default")).thenReturn(true);
    	assertTrue(client.createStorageInstance(appInstanceName, storageInst, "default"));
    	
    	// App instance creation with same name not allowed
    	when(client.createStorageInstance(appInstanceName, storageInst, "default")).thenReturn(false);
    	assertFalse(client.createStorageInstance(appInstanceName, storageInst, "default"));
    }
    
    @Test
    public void testCreateInitiatorGroup() {
    	
    	List<String> initiators  = new ArrayList<String> ();
    	initiators.add(DateraCommon.INITIATOR_1);
    	initiators.add(DateraCommon.INITIATOR_2);
    	when(client.createInitiatorGroup("init_grp_1", initiators)).thenReturn(true);
    	assertTrue(client.createInitiatorGroup("init_grp_1", initiators));
    }
    
    @Test
    public void testResizeVolume() {
    	final String appInstanceName = DateraUtil.generateAppInstanceName("test-appInst", UUID.randomUUID().toString());
    	final String storageInst = "storage-1";
    	final String volumeName = "volume-1";
    	
    	when(client.createAppInstance(appInstanceName)).thenReturn(true);
    	when(client.createStorageInstance(appInstanceName, storageInst, "default")).thenReturn(true);
    	when(client.createVolume(appInstanceName, storageInst, volumeName, 2, 2)).thenReturn(true);
    	
    	assertTrue(client.createAppInstance(appInstanceName));
    	assertTrue(client.createStorageInstance(appInstanceName, storageInst, "default"));
    	assertTrue(client.createVolume(appInstanceName, storageInst, volumeName, 2, 2));

    	when(client.resizeVolume(appInstanceName, storageInst, volumeName, 4)).thenReturn(false);
    	// App instance is online so volume resize is not allowed
    	assertFalse(client.resizeVolume(appInstanceName, storageInst, volumeName, 4));
    	
    	when(client.setAdminState(appInstanceName, false)).thenReturn(true);
    	when(client.resizeVolume(appInstanceName, storageInst, volumeName, 4)).thenReturn(true);
    	assertTrue(client.resizeVolume(appInstanceName, storageInst, volumeName, 4));
    }
    
    @Test
    public void testIsAppInstanceExists() {
    	
    	final String appInstanceName = DateraUtil.generateAppInstanceName("test-appInst", UUID.randomUUID().toString());
    	when(client.createAppInstance(appInstanceName)).thenReturn(true);
    	assertTrue(client.createAppInstance(appInstanceName));
    	
    	List<String> appList = new ArrayList<String> ();
    	appList.add(appInstanceName);
    	when(client.enumerateAppInstances()).thenReturn(appList);
    	assertTrue(client.enumerateAppInstances().contains(appInstanceName));
    }
    
    @Test
    public void testSetQos() {
    	
    	final String appInstanceName = DateraUtil.generateAppInstanceName("test-appInst", UUID.randomUUID().toString());
    	final String storageInst = "storage-1";
    	final String volumeName = "volume-1";

    	when(client.createAppInstance(appInstanceName)).thenReturn(true);
    	when(client.createStorageInstance(appInstanceName, storageInst, "default")).thenReturn(true);
    	when(client.createVolume(appInstanceName, storageInst, volumeName, 1, 2)).thenReturn(true);
    	when(client.setQos(appInstanceName, storageInst, volumeName, 1100000L)).thenReturn(true);
	
    	assertTrue(client.createAppInstance(appInstanceName));
    	assertTrue(client.createStorageInstance(appInstanceName, storageInst, "default"));
    	assertTrue(client.createVolume(appInstanceName, storageInst, volumeName, 1, 2));
    	assertTrue(client.setQos(appInstanceName, storageInst, volumeName, 1100000L));
    	
    	when(client.setQos(appInstanceName, storageInst, volumeName, 110000000000L)).thenReturn(false);
    	// Invalid total IOPS
    	assertFalse(client.setQos(appInstanceName, storageInst, volumeName, 110000000000L));
    }
}