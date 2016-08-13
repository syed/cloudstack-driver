import logging
import random
import time
import XenAPI

# All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase

# Import Integration Libraries

# base - contains all resources as entities and defines create, delete, list operations on them
from marvin.lib.base import Account, ServiceOffering, User, Host, StoragePool, VirtualMachine

# common - commonly used methods for all tests are listed here
from marvin.lib.common import get_domain, get_template, get_zone, list_hosts, list_clusters, list_volumes

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
    primaryStorage3 = "primarystorage3"
    primaryStorage4 = "primarystorage4"
    primaryStorage5 = "primaryStorage5"
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
        self.datear_url_without_replica = (
            "mgmtIP=172.19.1.214;mgmtPort=7718;" +
            "mgmtUserName=admin;mgmtPassword=password;" +
            "networkPoolName=default"),
        self.datear_url_without_netpool = (
            "mgmtIP=172.19.2.214;mgmtPort=7718;" +
            "mgmtUserName=admin;mgmtPassword=password;" +
            "replica=3"),
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
            TestData.urlOfNewHost: "https://172.19.175.167",
            TestData.primaryStorage: {
                TestData.name: "datera-%d" % random.randint(0, 100),
                TestData.scope: "CLUSTER",
                TestData.url: self.datear_url[0],
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
                TestData.url: self.datear_url[0],
                TestData.provider: "DateraShared",
                TestData.tags: TestData.storageTag,
                TestData.capacityIops: 5000,
                TestData.capacityBytes: 125000000000000,
                TestData.hypervisor: "XenServer",
                TestData.podId: 1
            },
            TestData.primaryStorage3: {
                TestData.name: "Datera-%d" % random.randint(0, 100),
                TestData.scope: "CLUSTER",
                TestData.url: self.datear_url_without_replica[0],
                TestData.provider: "DateraShared",
                TestData.tags: TestData.storageTag,
                TestData.capacityIops: 5000,
                TestData.capacityBytes: 1073741824,
                TestData.hypervisor: "XenServer",
                TestData.podId: 1
            },
            TestData.primaryStorage4: {
                TestData.name: "Datera-%d" % random.randint(0, 100),
                TestData.scope: "CLUSTER",
                TestData.url: self.datear_url_without_netpool[0],
                TestData.provider: "DateraShared",
                TestData.tags: TestData.storageTag,
                TestData.capacityIops: 5000,
                TestData.capacityBytes: 1073741824,
                TestData.hypervisor: "XenServer",
                TestData.podId: 1
            },
            TestData.newHost: {
                TestData.username: "root",
                TestData.password: "maple",
                TestData.url: "http://172.19.175.167",
                TestData.podId : "1",
                TestData.zoneId: "1"
            },
            TestData.primaryStorage5: {
                TestData.name: "datera-%d" % random.randint(0, 100),
                TestData.scope: "CLUSTER",
                TestData.url: self.datear_url[0],
                TestData.provider: "DateraShared",
                TestData.tags: TestData.storageTag,
                TestData.capacityIops: 0,
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
            TestData.osType: "debian webserver",
            TestData.zoneId: 1,
            TestData.clusterId: 1,
            TestData.domainId: 1,
            TestData.url: "172.18.1.204",
            TestData.clusterName: "Cluster-Xen",
            TestData.hostName: "tlx175.tlx.daterainc.com"
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

        cls.compute_offering = ServiceOffering.create(
            cls.apiClient,
            cls.testdata[TestData.computeOffering]
        )
        cls.template = get_template(cls.apiClient, cls.zone.id, cls.testdata[TestData.osType])
        cls.domain = get_domain(cls.apiClient, cls.testdata[TestData.domainId])

        cls._cleanup = [cls.compute_offering]

        cls.xen_session.xenapi.login_with_password(xenserver[TestData.username], xenserver[TestData.password])


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
        except Exception as e:
            logging.debug("Exception in tearDown(self): %s" % e)

    def test17_add_new_host_to_cluster(self):
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
        iqns_before_host_addition = self._get_host_iscsi_iqns()
        host, iqns_after_host_addition = self._perform_add_host(primary_storage.id)

        for iqn in iqns_before_host_addition:
            if iqn in iqns_after_host_addition:
                iqns_after_host_addition.remove(iqn)
        new_iqn = iqns_after_host_addition[0]

        found = False
        datera_primarystorage_name = "cloudstack-" + primary_storage.id
        for instance in self.datera_api.app_instances.list():
            if instance['name'] == datera_primarystorage_name:
                active_inits = instance['storage_instances']['storage-1']['active_initiators']
                if  new_iqn in active_inits:
                    found = True
                else:
                    found = False

        self.assertEqual(found, True, "initiator not updated")
        host.delete(self.apiClient)

    def _perform_add_host(self, primary_storage_id):
        host_iscsi_iqns = self._get_host_iscsi_iqns()

        xen_session = XenAPI.Session(self.testdata[TestData.urlOfNewHost])
        xenserver = self.testdata[TestData.xenServer]
        xen_session.xenapi.login_with_password(xenserver[TestData.username], xenserver[TestData.password])

        xen_session.xenapi.pool.join(self.xs_pool_master_ip, xenserver[TestData.username], xenserver[TestData.password])

        time.sleep(60)

        host = Host.create(
            self.apiClient,
            self.cluster,
            self.testdata[TestData.newHost],
            hypervisor="XenServer"
        )

        self.assertTrue(
            isinstance(host, Host),
            "'host' is not a 'Host'."
        )

        host_iscsi_iqns = self._get_host_iscsi_iqns()


        time.sleep(120)
        return host, host_iscsi_iqns


    def _get_root_volume(self, vm):
        list_volumes_response = list_volumes(
            self.apiClient,
            virtualmachineid=vm.id,
            listall=True
        )

        self.assertNotEqual(
            list_volumes_response,
            None,
            "'list_volumes_response' should not be equal to 'None'."
        )

        self.assertEqual(
            len(list_volumes_response) > 0,
            True,
            "'len(list_volumes_response)' should be greater than 0."
        )

        for volume in list_volumes_response:
            if volume.type.upper() == "ROOT":
                return volume

        self.assert_(False, "Unable to locate the ROOT volume of the VM with the following ID: " + str(vm.id))

    def _get_iqn(self, volume):
        # Get volume IQN
        sf_iscsi_name_request = {'volumeid': volume.id}
        # put this commented line back once PR 1403 is in
        # sf_iscsi_name_result = self.cs_api.getVolumeiScsiName(sf_iscsi_name_request)
        sf_iscsi_name_result = self.cs_api.getSolidFireVolumeIscsiName(sf_iscsi_name_request)
        # sf_iscsi_name = sf_iscsi_name_result['apivolumeiscsiname']['volumeiScsiName']
        sf_iscsi_name = sf_iscsi_name_result['apisolidfirevolumeiscsiname']['solidFireVolumeIscsiName']

        self._check_iscsi_name(sf_iscsi_name)

        return sf_iscsi_name

    def _get_iqn_2(self, primary_storage):
        sql_query = "Select path From storage_pool Where uuid = '" + str(primary_storage.id) + "'"

        # make sure you can connect to MySQL: https://teamtreehouse.com/community/cant-connect-remotely-to-mysql-server-with-mysql-workbench
        sql_result = self.dbConnection.execute(sql_query)

        return sql_result[0][0]

    def _check_iscsi_name(self, sf_iscsi_name):
        self.assertEqual(
            sf_iscsi_name[0],
            "/",
            "The iSCSI name needs to start with a forward slash."
        )

    def _get_host_iscsi_iqns(self):
        hosts = self.xen_session.xenapi.host.get_all()

        self.assertEqual(
            isinstance(hosts, list),
            True,
            "'hosts' is not a list."
        )

        host_iscsi_iqns = []

        for host in hosts:
            host_iscsi_iqns.append(self._get_host_iscsi_iqn(host))

        return host_iscsi_iqns

    def _get_host_iscsi_iqn(self, host):
        other_config = self.xen_session.xenapi.host.get_other_config(host)

        return other_config["iscsi_iqn"]


    def _check_list(self, in_list, expected_size_of_list, err_msg):
        self.assertEqual(
            isinstance(in_list, list),
            True,
            "'in_list' is not a list."
        )

        self.assertEqual(
            len(in_list),
            expected_size_of_list,
            err_msg
        )

