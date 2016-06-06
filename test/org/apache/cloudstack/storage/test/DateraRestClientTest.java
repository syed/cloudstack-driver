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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.lifecycle.DateraPrimaryDataStoreLifeCycle;
import org.apache.cloudstack.storage.datastore.utils.AppInstanceInfo;
import org.apache.cloudstack.storage.datastore.utils.DateraModel;
import org.apache.cloudstack.storage.datastore.utils.DateraRestClient;
import org.apache.cloudstack.storage.datastore.utils.DateraUtil;
import org.apache.cloudstack.storage.datastore.utils.DateraRestClient.StorageResponse;
import org.apache.http.client.methods.HttpPost;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class DateraRestClientTest {

    private String MANAGEMENT_IP = "172.19.175.170";
    private String USERNAME = "admin";
    private String PASSWORD = "password";
    private int PORT = 7718;
    private String APPINSTNAME = null;
    
    @Before
    public void init() {
    	APPINSTNAME = DateraUtil.generateAppInstanceName("test-appInst", UUID.randomUUID().toString());
    	DateraRestClient client = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);
    	assertTrue(client.createAppInstance(APPINSTNAME));
    	assertTrue(client.createStorageInstance(APPINSTNAME, client.defaultStorageName, "default"));
    }
    
    @After
    public void cleanUp() {
    	DateraRestClient client = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);
    	assertTrue(client.setAdminState(APPINSTNAME, false));
    	assertTrue(client.deleteAppInstance(APPINSTNAME));
    }

    @Test
    public void testCreateVolume(){
    	
        DateraRestClient client = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);
        assertTrue(client.createVolume(APPINSTNAME, client.defaultStorageName, "vol-1", 1, 3));
    }

    @Test
    public void testDeleteVolume(){

    	DateraRestClient client = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);
        assertTrue(client.createVolume(APPINSTNAME, client.defaultStorageName, "vol-2", 2, 2));
        assertTrue(client.setAdminState(APPINSTNAME, false));
        assertTrue(client.deleteVolume(APPINSTNAME, client.defaultStorageName, "vol-2"));
        assertTrue(client.setAdminState(APPINSTNAME, true));
    }

    @Test
    public void testCreateAppInstance()
    {
    	String appInstanceName = DateraUtil.generateAppInstanceName("test-appInst-", UUID.randomUUID().toString());
    	DateraRestClient rest = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);
        assertTrue(rest.createAppInstance(appInstanceName)); 
        assertTrue(rest.setAdminState(appInstanceName, false));
        assertTrue(rest.deleteAppInstance(appInstanceName));
    }

    @Test
    public void testRegisterInitiatorsOnDatera(){
    	
    	List<String> initiators = new ArrayList<String>();
        String iqn = "iqn.1994-05.com.xenserver:dc785c10806";
        initiators.add(iqn);

        String groupName = "test-init-group_" + APPINSTNAME;
        DateraRestClient rest = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);

        rest.registerInitiators(initiators);
        rest.createInitiatorGroup(groupName, initiators);
        
        List<String> initiatorGroups = new ArrayList<String>();
        initiatorGroups.add(groupName);

        rest.updateStorageWithInitiator(APPINSTNAME, rest.defaultStorageName, null, initiatorGroups);

        initiators = rest.getInitiators();
        rest.unregisterInitiator(iqn);
    }

    @Test
    public void testIsAppInstanceExists(){
    	
    	DateraRestClient rest = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);
    	String appInst1 = DateraUtil.generateAppInstanceName("test-appInst", UUID.randomUUID().toString());
    	assertFalse(rest.isAppInstanceExists(appInst1));

    	String appInst2 = DateraUtil.generateAppInstanceName("test-appInst-", UUID.randomUUID().toString());
    	rest.createAppInstance(appInst2);
    	assertTrue(rest.isAppInstanceExists(appInst2));
    }
 
    @Test
    public void testGetStorageInfo(){
    	
        DateraRestClient client = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);
        assertTrue(client.createStorageInstance(APPINSTNAME, "storage-2", "default"));
        assertTrue(client.createVolume(APPINSTNAME, client.defaultStorageName, "volume-1", 1, 2));
        AppInstanceInfo.StorageInstance info = client.getStorageInfo(APPINSTNAME, "storage-2");
        assertTrue(info.name.equals("storage-2"));
    }

    @Test
    public void testCreateInitiatorGroup() {
    	
    	DateraRestClient rest = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);
    	
		List<String> initiators = new ArrayList<String>();
		
		initiators.add("iqn.2005-03.org.open-iscsi:01cbe94a11");
		initiators.add("iqn.1994-05.com.xenserver:dc785c10806");

        List<String> inits = rest.registerInitiators(initiators);
        assertTrue(inits.contains("iqn.2005-03.org.open-iscsi:01cbe94a11") && inits.contains("iqn.1994-05.com.xenserver:dc785c10806"));
        String initGroupName = "test_initgroup_" + UUID.randomUUID().toString();
        assertTrue(rest.createInitiatorGroup(initGroupName, initiators));
        
        for(String iqn : initiators) {
        	assertTrue(rest.unregisterInitiator(iqn));
        }
        assertTrue(rest.deleteInitiatorGroup(initGroupName));
    }
    
    @Test
    public void testDeleteAppInstanceOnline() {
    	String appInstanceName = DateraUtil.generateAppInstanceName("test-appInst-", UUID.randomUUID().toString());
    	DateraRestClient client = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);
        assertTrue(client.createAppInstance(appInstanceName));
        assertFalse(client.deleteAppInstance(appInstanceName));
        assertTrue(client.setAdminState(appInstanceName, false));
        assertTrue(client.deleteAppInstance(appInstanceName));
    }
    
    @Test
    public void testEnumerateAppInstance() {
    	DateraRestClient client = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);
    	List <String> resp = client.enumerateAppInstances();
    }
    
    @Test
    public void testCreateStorageInstance() {
    	DateraRestClient client = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);
    	assertTrue(client.createStorageInstance(APPINSTNAME, "storage-3", "default"));
    }
    
    @Test
    public void testDeleteVolumeWithAppInstOnline() {
    	DateraRestClient client = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);
        assertTrue(client.createVolume(APPINSTNAME, client.defaultStorageName, "vol-2", 2, 2));
        assertFalse(client.deleteVolume(APPINSTNAME, client.defaultStorageName, "vol-2"));
    }
    
    @Test
    public void testGetVolumes() {
    	DateraRestClient client = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);
    	assertTrue(client.createVolume(APPINSTNAME, client.defaultStorageName, "vol-4", 1, 2));
    	AppInstanceInfo.VolumeInfo resp = client.getVolumeInfo(APPINSTNAME, client.defaultStorageName, "vol-4");
    	assertTrue(resp.name.equals("vol-4"));
    }
    
    @Test
    public void testGenerateVolumePayload() {
    	DateraRestClient client = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);
        List<String> initiators = new ArrayList<String>();
        
        List<String> initiatorGroup = new ArrayList<String>();
		
		initiators.add("iqn.2005-03.org.open-iscsi:01cbe94a11");
		initiators.add("iqn.1994-05.com.xenserver:dc785c10806");
		
    	String resp = client.generateVolumePayload(APPINSTNAME, initiators, initiatorGroup, 1, 2, "allow_all", "default");
    	assertTrue(resp.contains(APPINSTNAME) && resp.contains("iqn.1994-05.com.xenserver:dc785c10806"));
    }
    
    @Test
    public void testResizeVolume() {
    	DateraRestClient client = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);
    	assertTrue(client.createVolume(APPINSTNAME, client.defaultStorageName, "vol-6", 1, 2));
    	assertTrue(client.setAdminState(APPINSTNAME, false));
    	assertTrue(client.resizeVolume(APPINSTNAME, client.defaultStorageName, "vol-6", 2));
    }
    
    @Test
    public void testDeleteInitiatorGroup() {
    	
    	DateraRestClient rest = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);
    	
		List<String> initiators = new ArrayList<String>();
		
		String initGroupName1 = "test_initgroup_" + UUID.randomUUID().toString();
        assertTrue(rest.createInitiatorGroup(initGroupName1, initiators));
        assertTrue(rest.deleteInitiatorGroup(initGroupName1));
        
        // Negative test for deleting a group which does not exists
        assertFalse(rest.deleteInitiatorGroup("test_initgroup_" + UUID.randomUUID().toString()));
    }
    
    @Test
    public void testEnumerateInitiatorNames() {
    	DateraRestClient client = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);
    	String iqn = "iqn.2005-03.org.open-iscsi:01cbe94a19";
    	assertTrue(client.registerInitiator("test_create_initiators_1", iqn));
    	List<String> initiators = client.enumerateInitiatorNames();
    	assertTrue(initiators.contains(iqn));
    }
    
    @Test
    public void testUpdateStorageWithInitiator () {
    	
    	List<String> initiators = new ArrayList<String>();
        String iqn = "iqn.1994-05.com.xenserver:dc785c10306";
        initiators.add(iqn);
        
        String appInstanceName = DateraUtil.generateAppInstanceName("test-appInst-", UUID.randomUUID().toString());
        
        String groupName = "test-init-group_" + appInstanceName;
        DateraRestClient rest = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);

        rest.registerInitiators(initiators);
        rest.createInitiatorGroup(groupName, initiators);
        
        List<String> initiatorGroups = new ArrayList<String>();
        initiatorGroups.add(groupName);

        boolean bool = rest.updateStorageWithInitiator(appInstanceName, rest.defaultStorageName, null, initiatorGroups);

        initiators = rest.getInitiators();
        rest.unregisterInitiator(iqn);
    }
    
    @Test
    public void testSetQos() {
    	
    	DateraRestClient client = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);
    	assertTrue(client.createStorageInstance(APPINSTNAME, "storage-3", "default"));
    	assertTrue(client.createVolume(APPINSTNAME, "storage-3", "volume-8", 1, 2));
    	assertTrue(client.setQos(APPINSTNAME, "storage-3", "volume-8", 1100000L));
    	
    	// Invalid total IOPS
    	assertFalse(client.setQos(APPINSTNAME, "storage-3", "volume-8", 10000000000L));
    }
}

