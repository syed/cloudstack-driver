import logging
import random
import XenAPI
# All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase

# Import Integration Libraries

from marvin.lib.base import (Account, ServiceOffering, DiskOffering,
                             User, Volume, StoragePool, VirtualMachine)

# common - commonly used methods for all tests are listed here
from marvin.lib.common import (get_domain, get_zone, list_storage_pools,
                               list_hosts, list_clusters,
                               list_volumes, list_templates)

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
    diskOffering = "diskoffering"
    diskSize = "disksize"
    diskName = "diskName"
    domainId = "domainId"
    hypervisor = "hypervisor"
    login = "login"
    mvip = "mvip"
    name = "name"
    newHost = "newHost"
    newHostDisplayName = "newHostDisplayName"
    osName = "ostype"
    password = "password"
    podId = "podid"
    port = "port"
    primaryStorage = "primarystorage"
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
        self.datear_url = (
            "mgmtIP=172.19.2.214;mgmtPort=7718;" +
            "mgmtUserName=admin;mgmtPassword=password;" +
            "replica=3;networkPoolName=default"),
        self.testdata = {
            TestData.Datera: {
                TestData.mvip: "172.19.2.214",
                TestData.login: "admin",
                TestData.password: "password",
            },
            TestData.xenServer: {
                TestData.username: "root",
                TestData.password: "maple"
            },
            TestData.account: {
                "email": "test@example.com",
                "firstname": "John",
                "lastname": "Doe",
                TestData.username: "test",
                TestData.password: "test"
            },
            TestData.user: {
                "email": "user@example.com",
                "firstname": "Jane",
                "lastname": "Doe",
                TestData.username: "testuser",
                TestData.password: "password"
            },
            TestData.primaryStorage: {
                TestData.name: "datera-%d" % random.randint(0, 100),
                TestData.scope: "CLUSTER",
                TestData.url: self.datear_url[0],
                TestData.provider: "DateraShared",
                TestData.tags: TestData.storageTag,
                TestData.capacityIops: 10000,
                TestData.capacityBytes: 10737418240,
                TestData.hypervisor: "XenServer",
                TestData.podId: 1
            },
            TestData.diskOffering: {
                "name": "Datera_DO",
                "displaytext": "Datera_DO",
                "disksize": 512,
                "miniops": 300,
                "maxiops": 500,
                TestData.tags: TestData.storageTag,
                "storagetype": "shared"
            },
            TestData.volume_1: {
                TestData.diskName: "testvolume",
            },
            TestData.virtualMachine: {
                TestData.name: "TestVM",
                "displayname": "Test VM"
            },
            TestData.computeOffering: {
                TestData.name: "Datera",
                TestData.displayText: "datera",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 512,
                "storagetype": "shared",
                "miniops": "10000",
                "maxiops": "15000",
                TestData.tags: TestData.storageTag
            },
            TestData.osName: "Debian basic webserver (Xenserver)",
            TestData.zoneId: 1,
            TestData.clusterId: 1,
            TestData.domainId: 1,
            TestData.url: "172.19.175.174",
            TestData.clusterName: "Cluster-Xen",
            TestData.hostName: "tlx167.tlx.daterainc.com"
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

        list_template_response = list_templates(cls.apiClient,
                                                zoneid=cls.zone.id,
                                                templatefilter='all')
        for templates in list_template_response:
            if templates.name == cls.testdata[TestData.osName]:
                cls.template = templates
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

        cls.disk_offering = DiskOffering.create(
            cls.apiClient,
            cls.testdata[TestData.diskOffering]
        )

        cls._cleanup = [
            cls.compute_offering,
            cls.disk_offering,
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
            if self.virtual_machine is not None:
                self.virtual_machine.delete(self.apiClient, True)

            cleanup_resources(self.apiClient, self.cleanup)
        except Exception as e:
            logging.debug("Exception in tearDown(self): %s" % e)

    def test09_add_vm_with_datera_storage(self):
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
        self.virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=True
        )

        self._validate_storage(primary_storage, self.virtual_machine)

    def test10_add_vm_with_datera_storage_and_volume(self):
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
        self.virtual_machine = VirtualMachine.create(
            self.apiClient,
            self.testdata[TestData.virtualMachine],
            accountid=self.account.name,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            templateid=self.template.id,
            domainid=self.domain.id,
            startvm=True
        )

        self._validate_storage(primary_storage, self.virtual_machine)

        Volume.create(
            self.apiClient,
            self.testdata[TestData.volume_1],
            account=self.account.name,
            domainid=self.domain.id,
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering.id
        )

        storage_pools_response = list_storage_pools(
            self.apiClient, id=primary_storage.id)

        for key, value in self.xen_session.xenapi.SR.get_all_records().items():
            if value['name_description'] == primary_storage.id:
                xen_server_response = value

        self.assertNotEqual(
            int(storage_pools_response[0].disksizeused),
            int(xen_server_response['physical_utilisation']))

    def _validate_storage(self, storage, vm):
        list_volumes_response = list_volumes(
            self.apiClient, virtualmachineid=vm.id, listall=True)

        self.assertNotEqual(
            list_volumes_response, None,
            "'list_volumes_response' should not be equal to 'None'.")

        for volume in list_volumes_response:
            if volume.type.upper() == "ROOT":
                volumeData = volume
                self.assertEqual(volume.storage, storage.name,
                                 "Volume not created for VM " + str(vm.id))
        #Verify in cloudstack
        storage_pools_response = list_storage_pools(
            self.apiClient, id=storage.id)

        self.assertEqual(
            int(volumeData.size),
            int(storage_pools_response[0].disksizeused),
            "Allocated disk sizes are not same in volumes and primary stoarge")

        #Verify in datera
        datera_primarystorage_name = "cloudstack-" + storage.id
        for instance in self.datera_api.app_instances.list():
            if instance['name'] == datera_primarystorage_name:
                app_instance_response = instance

        app_instance_response_disk = (
            app_instance_response['storage_instances']
            ['storage-1']['volumes']['volume-1']
            ['capacity_in_use'] * 1073741824)
        self.assertEqual(
            int(volumeData.size),
            int(app_instance_response_disk),
            "App instance usage size is incorrect")

        #Verify in xen server
        for key, value in self.xen_session.xenapi.SR.get_all_records().items():
            if value['name_description'] == storage.id:
                xen_server_response = value
        self.assertEqual(
            int(xen_server_response['physical_utilisation']),
            int(volumeData.size),
            "Allocated disk sizes is incorrect in xenserver")
