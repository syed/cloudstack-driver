import logging
import random
import XenAPI
# All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase

# Import Integration Libraries

from marvin.lib.base import Account, ServiceOffering, User, StoragePool

# common - commonly used methods for all tests are listed here
from marvin.lib.common import (get_domain, get_zone,
                               list_hosts, list_clusters,
                               list_storage_pools)

# utils - utility classes for common cleanup, external library wrappers, etc.
from marvin.lib.utils import cleanup_resources

from dfs_sdk import DateraApi


class TestData:
    account = "account"
    capacityBytes = "capacitybytes"
    capacityIops = "capacityiops"
    clusterId = "clusterId"
    computeOffering = "computeoffering"
    displayText = "displaytext"
    diskSize = "disksize"
    domainId = "domainId"
    hypervisor = "hypervisor"
    login = "login"
    mvip = "mvip"
    name = "name"
    newHost = "newHost"
    newHostDisplayName = "newHostDisplayName"
    osType = "ostype"
    password = "password"
    podId = "podid"
    port = "port"
    primaryStorage = "primarystorage"
    primaryStorage2 = "primarystorage2"
    provider = "provider"
    scope = "scope"
    Datera = "Datera"
    storageTag = "Datera_1"
    storageTag2 = "datera_2"
    tags = "tags"
    url = "url"
    path = "path"
    urlOfNewHost = "urlOfNewHost"
    user = "user"
    username = "username"
    virtualMachine = "virtualmachine"
    volume_1 = "volume_1"
    xenServer = "xenserver"
    zoneId = "zoneid"
    clusterName = "clusterName"
    hostName = "hostname"

    def __init__(self):
        self.testdata = {
            TestData.Datera: {
                TestData.mvip: "172.19.175.170",
                TestData.login: "admin",
                TestData.password: "password",
            },
            TestData.xenServer: {
                TestData.username: "root",
                TestData.password: "maple"
            },
            TestData.account: {
                "email": "test@test.com",
                "firstname": "John",
                "lastname": "Doe",
                TestData.username: "test",
                TestData.password: "test"
            },
            TestData.user: {
                "email": "user@test.com",
                "firstname": "Jane",
                "lastname": "Doe",
                TestData.username: "testuser",
                TestData.password: "password"
            },
            TestData.primaryStorage: {
                TestData.name: "datera-%d" % random.randint(0, 100),
                TestData.scope: "CLUSTER",
                TestData.url: (
                    "mgmtIP=172.19.175.170;mgmtPort=7718;" +
                    "mgmtUserName=admin;mgmtPassword=password;" +
                    "replica=3;networkPoolName=default"),
                TestData.provider: "DateraShared",
                TestData.tags: TestData.storageTag,
                TestData.capacityIops: 5000,
                TestData.capacityBytes: 1073741824,
                TestData.hypervisor: "XenServer",
                TestData.podId: 1
            },
            TestData.primaryStorage2: {
                TestData.name: "Datera-%d" % random.randint(0, 100),
                TestData.scope: "CLUSTER",
                TestData.url: (
                    "mgmtIP=172.19.175.170;mgmtPort=7718;" +
                    "mgmtUserName=admin;mgmtPassword=password;" +
                    "replica=3;networkPoolName=default"),

                TestData.provider: "DateraShared",
                TestData.tags: TestData.storageTag,
                TestData.capacityIops: 5000,
                TestData.capacityBytes: 42949672960,
                TestData.hypervisor: "XenServer",
                TestData.podId: 1
            },

            TestData.computeOffering: {
                TestData.name: "DATERA",
                TestData.displayText: (
                    "DATERA (Min IOPS = 10,000; Max IOPS = 15,000)"),
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 128,
                "storagetype": "shared",
                "customizediops": False,
                "miniops": "10000",
                "maxiops": "15000",
                "hypervisorsnapshotreserve": 200,
                TestData.tags: TestData.storageTag
            },
            TestData.osType: "CentOS 5.6(64-bit) no GUI (XenServer)",
            TestData.zoneId: 1,
            TestData.clusterId: 1,
            TestData.domainId: 1,
            TestData.url: "172.19.175.174",
            TestData.clusterName: "Cluster-Xen",
            TestData.hostName: "tlx175"
        }


class TestPrimaryStorage(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        # Set up API client
        testclient = super(TestPrimaryStorage, cls).getClsTestClient()
        cls.apiClient = testclient.getApiClient()
        cls.dbConnection = testclient.getDbConnection()
        cls.services = testclient.getParsedTestDataConfig()
        cls.testdata = TestData().testdata
        cls.zone = get_zone(
            cls.apiClient, zone_id=cls.testdata[TestData.zoneId])
        for cluster in list_clusters(cls.apiClient):
            if cluster.name == cls.testdata[TestData.clusterName]:
                cls.cluster = cluster
        cls.domain = get_domain(cls.apiClient, cls.testdata[TestData.domainId])
        cls.xs_pool_master_ip = list_hosts(
            cls.apiClient, clusterid=cls.cluster.id)
        for host in cls.xs_pool_master_ip:
            if host.name == cls.testdata[TestData.hostName]:
                cls.xs_pool_master_ip = host.ipaddress
        host_ip = "https://" + cls.xs_pool_master_ip
        cls.xen_session = XenAPI.Session(host_ip)
        xenserver = cls.testdata[TestData.xenServer]
        cls.xen_session.xenapi.login_with_password(
            xenserver[TestData.username], xenserver[TestData.password])
        datera = cls.testdata[TestData.Datera]
        cls.datera_api = DateraApi(
            username=datera[TestData.login],
            password=datera[TestData.password],
            hostname=datera[TestData.mvip])

        # Create test account
        cls.account = Account.create(
            cls.apiClient,
            cls.testdata[TestData.account],
            admin=1
        )

        # Set up connection to make customized API calls
        user = User.create(
            cls.apiClient,
            cls.testdata[TestData.user],
            account=cls.account.name,
            domainid=cls.domain.id
        )

        cls.compute_offering = ServiceOffering.create(
            cls.apiClient,
            cls.testdata[TestData.computeOffering]
        )

        cls._cleanup = [
            cls.compute_offering,
            user,
            cls.account
        ]

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiClient, cls._cleanup)
        except Exception as e:
            logging.debug("Exception in tearDownClass(cls): %s" % e)

    def setUp(self):
        self.virtual_machine = None
        self.cleanup = []

    def tearDown(self):
        try:
            primarystorage = self.testdata[TestData.primaryStorage]
            if self.virtual_machine is not None:
                self.virtual_machine.delete(self.apiClient, True)

            cleanup_resources(self.apiClient, self.cleanup)
            flag = 0
            for item in self.datera_api.app_instances.list():
                if item['name'] == primarystorage[TestData.name]:
                    flag = 1
            if flag > 0:
                raise Exception('app instance not deleted.')
        except Exception as e:
            logging.debug("Exception in tearDown(self): %s" % e)
            raise

    def test_primary_storage_1(self):
        primarystorage = self.testdata[TestData.primaryStorage]

        primary_storage = StoragePool.create(
            self.apiClient,
            primarystorage,
            scope=primarystorage[TestData.scope],
            zoneid=self.zone.id,
            clusterid=self.cluster.id,
            provider=primarystorage[TestData.provider],
            tags=primarystorage[TestData.tags],
            capacityiops=primarystorage[TestData.capacityIops],
            capacitybytes=primarystorage[TestData.capacityBytes],
            hypervisor=primarystorage[TestData.hypervisor]
        )

        self.cleanup.append(primary_storage)

        self.assertEqual(
            any(primarystorage[TestData.name] == app_instance['name']
                for app_instance in self.datera_api.app_instances.list()),
            True, "app instance not created")
        primary_storage_url = primarystorage[TestData.url]

        self._verify_attributes(
            primary_storage.id, primarystorage[TestData.name],
            primary_storage_url)

    def test_primary_storage_2(self):
        primarystorage2 = self.testdata[TestData.primaryStorage2]

        primary_storage2 = StoragePool.create(
            self.apiClient,
            primarystorage2,
            scope=primarystorage2[TestData.scope],
            zoneid=self.zone.id,
            clusterid=self.cluster.id,
            provider=primarystorage2[TestData.provider],
            tags=primarystorage2[TestData.tags],
            capacityiops=primarystorage2[TestData.capacityIops],
            capacitybytes=primarystorage2[TestData.capacityBytes],
            hypervisor=primarystorage2[TestData.hypervisor]
        )

        self.cleanup.append(primary_storage2)

        self.assertEqual(
            any(primarystorage2[TestData.name] == app_instance['name']
                for app_instance in self.datera_api.app_instances.list()),
            True, "app instance not created")

        primary_storage_url = primarystorage2[TestData.url]

        self._verify_attributes(
            primary_storage2.id, primarystorage2[TestData.name],
            primary_storage_url)

    def _verify_attributes(self, primary_storage_id, primarystorage_name,
                           primary_storage_url):
        #Cloudstack Primary storage pool
        storage_pools_response = list_storage_pools(
            self.apiClient, id=primary_storage_id)
        storage_pools_response = storage_pools_response[0]
        storage_pools_response_type = storage_pools_response.type
        storage_pools_response_iqn = storage_pools_response.id
        storage_pools_response_disk = storage_pools_response.disksizetotal
        storage_pools_response_ipaddress = storage_pools_response.ipaddress
        storage_pools_response_state = storage_pools_response.state
        storage_pools_response_iops = storage_pools_response.capacityiops

        # Datera app instances
        for instance in self.datera_api.app_instances.list():
            if instance['name'] == primarystorage_name:
                app_instance_response = instance
        app_instance_response_iqn = (
            app_instance_response['storage_instances']
            ['storage-1']['access']['iqn'])
        app_instance_response_ipaddress = (
            app_instance_response['storage_instances']
            ['storage-1']['access']['ips'])
        app_instance_response_op_state = (
            app_instance_response['storage_instances']
            ['storage-1']['volumes']['volume-1']['op_state'])
        app_instance_response_disk = (
            app_instance_response['storage_instances']
            ['storage-1']['volumes']['volume-1']['size'] * 1073741824)
        app_instance_response_iops = (
            app_instance_response['storage_instances']
            ['storage-1']['volumes']['volume-1']['performance_policy']
            ['total_iops_max'])
        app_instance_response_replica = (
            app_instance_response['storage_instances']
            ['storage-1']['volumes']['volume-1']['replica_count'])
        app_instance_response_ippool = (
            app_instance_response['storage_instances']
            ['storage-1']['ip_pool'])

        # xen server details
        for key, value in self.xen_session.xenapi.SR.get_all_records().items():
            if value['name_label'] == app_instance_response_iqn:
                xen_server_response = value

        xen_server_response_iqn = xen_server_response['name_label']
        xen_server_response_type = xen_server_response['type']

        self.assertEqual(
            storage_pools_response_type, "IscsiLUN",
            "Failed to create IscsiLUN")

        self.assertEqual(
            xen_server_response_type, "lvmoiscsi",
            "Failed to craete lvmoiscsi on xen server")

        if ((xen_server_response_iqn != app_instance_response_iqn) or
                (app_instance_response_iqn != storage_pools_response_iqn)):
            self.assert_(False, "iqn IDs are different")

        self.assertEqual(
            app_instance_response_disk,
            storage_pools_response_disk,
            "Disk sizes are not different")

        if (storage_pools_response_ipaddress not in
                app_instance_response_ipaddress):
            self.assert_(False, "Storage ip address are different")

        self.assertEqual(
            storage_pools_response_state,
            "Up",
            "Primary storage status is down")

        self.assertEqual(
            app_instance_response_op_state,
            "available",
            "datera app instance is down")

        self.assertEqual(
            storage_pools_response_iops,
            app_instance_response_iops,
            "IOPS values are incorrect")
        cs_replica = [data for data in primary_storage_url.split(";")
                      if data.startswith('replica')]
        cs_netPool = [data for data in primary_storage_url.split(";")
                      if data.startswith('networkPoolName')]

        replica_count = ''.join(cs_replica).split("=")[1]
        cs_netPool = ''.join(cs_netPool).split("=")[1]

        self.assertEqual(
            int(replica_count),
            int(app_instance_response_replica),
            "Incorrect replicas count")

        self.assertEqual(
            app_instance_response_ippool.split('/')[2],
            cs_netPool,
            "Incorrect networkPoolName")
