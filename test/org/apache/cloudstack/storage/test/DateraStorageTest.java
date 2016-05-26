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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;


import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.utils.AppInstanceInfo;
import org.apache.cloudstack.storage.datastore.utils.DateraRestClient;
import org.apache.cloudstack.storage.datastore.utils.DateraUtil;
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
		
        String appInstanceName = DateraUtil.generateAppInstanceName("test-pool-create", "cloudstack-vol-1");
        String storageInstanceName = "Storage-1";
        
        DateraRestClient client = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);
        
        assertTrue(client.createAppInstance(appInstanceName));
        assertTrue(client.createStorageInstance(appInstanceName, storageInstanceName));
        assertTrue(client.createVolume(appInstanceName, storageInstanceName, "volume-1", 2));
        assertTrue(client.setAdminState(appInstanceName, false));
        assertTrue(client.deleteAppInstance(appInstanceName));
	}
	@Test
	public void testDeleteVolume(){

        String appInstanceName = DateraUtil.generateAppInstanceName("test-pool-delete", "cloudstack-vol-2");
        String storageInstanceName = "Storage-1";
        String volumeName = "volume-1";
        
        DateraRestClient client = new DateraRestClient(MANAGEMENT_IP, PORT, USERNAME, PASSWORD);
        
        assertTrue(client.createAppInstance(appInstanceName));
        assertTrue(client.createStorageInstance(appInstanceName, storageInstanceName));
        assertTrue(client.createVolume(appInstanceName, storageInstanceName, volumeName, 2));
        assertTrue(client.setAdminState(appInstanceName, false));
        assertTrue(client.deleteVolume(appInstanceName, "Storage-1", volumeName));
        assertTrue(client.deleteAppInstance(appInstanceName));
	}
}
