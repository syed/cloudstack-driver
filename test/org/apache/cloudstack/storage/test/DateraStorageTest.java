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

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.lifecycle.DateraPrimaryDataStoreLifeCycle;
import org.apache.cloudstack.storage.datastore.utils.AppInstanceInfo;
import org.apache.cloudstack.storage.datastore.utils.DateraRestClient;
import org.apache.cloudstack.storage.datastore.utils.DateraUtil;
import org.apache.cloudstack.storage.datastore.utils.DateraRestClient.StorageResponse;
import org.bouncycastle.asn1.x509.qualified.TypeOfBiometricData;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.user.AccountVO;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class DateraStorageTest {

    private DateraRestClient client;
    private String MANAGEMENT_IP = "172.19.175.170";
    private String USERNAME = "admin";
    private String PASSWORD = "password";
    private int PORT = 7718;

    @Test
    public void testCreateVolume(){

        String appInstanceName = DateraUtil.generateAppInstanceName("test-appInst", UUID.randomUUID().toString());
        DateraRestClient client = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);

        assertTrue(client.createAppInstance(appInstanceName));
        assertTrue(client.createStorageInstance(appInstanceName, client.defaultStorageName, "default"));
        assertTrue(client.createVolume(appInstanceName, client.defaultStorageName, client.defaultVolumeName, 1, 3));
        assertTrue(client.setAdminState(appInstanceName, false));
        assertTrue(client.deleteAppInstance(appInstanceName));
    }

    @Test
    public void testDeleteVolume(){
    	
        String appInstanceName = DateraUtil.generateAppInstanceName("test-appInst", UUID.randomUUID().toString());

        DateraRestClient client = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);
        
        assertTrue(client.createAppInstance(appInstanceName));
        assertTrue(client.createStorageInstance(appInstanceName, client.defaultStorageName, "default"));
        assertTrue(client.createVolume(appInstanceName, client.defaultStorageName, client.defaultVolumeName, 2, 2));
        assertTrue(client.setAdminState(appInstanceName, false));
        assertTrue(client.deleteVolume(appInstanceName, client.defaultStorageName, client.defaultVolumeName));
        assertTrue(client.deleteAppInstance(appInstanceName));
    }

    @Test
    public void testValidateUrl(){
    	String url = "  mgmtIP   =       172.19.175.170;   mgmtPort=7718     ;mgmtUserName=admin;mgmtPassword=  password;replica = 3;     " + 
    			"networkPoolName=default  ; datacenter   =  dummy1; volumeGroupName=  vg2; appName= xen1 ;  storageName=storage-1;";

    	assertTrue(DateraUtil.getValue(DateraUtil.MANAGEMENT_IP, url).equals("172.19.175.170"));
    	assertTrue(DateraUtil.getValue(DateraUtil.MANAGEMENT_PORT, url).equals("7718"));
    	assertTrue(DateraUtil.getValue(DateraUtil.MANAGEMENT_USERNAME, url).equals("admin"));
    	assertTrue(DateraUtil.getValue(DateraUtil.MANAGEMENT_PASSWORD, url).equals("password"));
    	assertTrue(DateraUtil.getValue(DateraUtil.VOLUME_REPLICA, url).equals("3"));
    	assertTrue(DateraUtil.getValue(DateraUtil.NETWORK_POOL_NAME, url).equals("default"));
    	assertTrue(DateraUtil.getValue(DateraUtil.DATACENTER, url).equals("dummy1"));
    	assertTrue(DateraUtil.getValue(DateraUtil.CLVM_VOLUME_GROUP_NAME, url).equals("vg2"));
    	assertTrue(DateraUtil.getValue(DateraUtil.APP_NAME, url).equals("xen1"));
    	assertTrue(DateraUtil.getValue(DateraUtil.STORAGE_NAME, url).equals("storage-1"));
    }

    @Test
    public void testCreateApplicationInstance()
    {
    	String appInstanceName = DateraUtil.generateAppInstanceName("test-appInst-", UUID.randomUUID().toString());
    	DateraRestClient rest = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);

        assertTrue(rest.createAppInstance(appInstanceName)); 
        assertTrue(rest.createStorageInstance(appInstanceName, rest.defaultStorageName, "default"));
        assertTrue(rest.setAdminState(appInstanceName, false));
        assertTrue(rest.deleteAppInstance(appInstanceName));
    }

    @Test
    public void testRegisterInitiatorsOnDatera(){
    	
    	List<String> initiators = new ArrayList<String>();
        String iqn = "iqn.1994-05.com.xenserver:dc785c10706";
        initiators.add(iqn);
        
        String appInstanceName = DateraUtil.generateAppInstanceName("test-appInst-", UUID.randomUUID().toString());
        
        String groupName = "test-init-group_" + appInstanceName;
        DateraRestClient rest = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);

        rest.registerInitiators(initiators);
        rest.createInitiatorGroup(groupName, initiators);
        
        List<String> initiatorGroups = new ArrayList<String>();
        initiatorGroups.add(groupName);

        rest.updateStorageWithInitiator(appInstanceName, rest.defaultStorageName, null, initiatorGroups);

        initiators = rest.getInitiators();
        rest.unregisterInitiator(iqn);
    }

    @Test
    public void testIsAppInstanceExists(){
    	
    	DateraRestClient rest = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);
    	assertFalse(rest.isAppInstanceExists("some_name"));

    	String appInst = DateraUtil.generateAppInstanceName("test-appInst-", UUID.randomUUID().toString());
    	rest.createAppInstance(appInst);
    	assertTrue(rest.isAppInstanceExists(appInst));
    }
    
    @Test
    public void testGetStorageInfo(){
    	String appInstanceName = DateraUtil.generateAppInstanceName("test-appInst", UUID.randomUUID().toString());
        DateraRestClient client = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);

        assertTrue(client.createAppInstance(appInstanceName));
        assertTrue(client.createStorageInstance(appInstanceName, client.defaultStorageName, "default"));
        assertTrue(client.createVolume(appInstanceName, client.defaultStorageName, "volume-1", 1, 2));
        
        AppInstanceInfo.VolumeInfo info = client.getVolumeInfo(appInstanceName, client.defaultStorageName, "volume-1");
        
        assertTrue(info.name.equals("volume-1"));
        assertTrue(client.setAdminState(appInstanceName, false));
        assertTrue(client.deleteAppInstance(appInstanceName));
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
}

