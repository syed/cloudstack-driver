import logging
import random
import XenAPI
# All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase

# Import Integration Libraries

from marvin.lib.base import Account, ServiceOffering, User, StoragePool

# common - commonly used methods for all tests are listed here
from marvin.lib.common import (get_domain, get_zone,
                               list_hosts, list_clusters)

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
                TestData.name: "Datera-%d" % random.randint(0, 100),
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

        primarystorage = cls.testdata[TestData.primaryStorage]

        primary_storage = StoragePool.create(
            cls.apiClient,
            primarystorage,
            scope=primarystorage[TestData.scope],
            zoneid=cls.zone.id,
            clusterid=cls.cluster.id,
            provider=primarystorage[TestData.provider],
            tags=primarystorage[TestData.tags],
            capacityiops=primarystorage[TestData.capacityIops],
            capacitybytes=primarystorage[TestData.capacityBytes],
            hypervisor=primarystorage[TestData.hypervisor]
        )

        cls._primary_storage = [primary_storage]

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiClient, cls._cleanup)
        except Exception as e:
            logging.debug("Exception in tearDownClass(cls): %s" % e)

    def setUp(self):
        pass

    def tearDown(self):
        pass

    def test_delete_primary_storage_1(self):
        primarystorage = self.testdata[TestData.primaryStorage]
        cleanup_resources(self.apiClient, self._primary_storage)

        flag = 0
        for item in self.datera_api.app_instances.list():
            if item['name'] == primarystorage[TestData.name]:
                flag = 1
        self.assertEqual(flag, 0, "app instance not deleted.")
