======
DATERA CloudStack Driver Repository
======
.. list-table::CloudStack Driver Version with Datera Product and Supported Hypervisor(s) 
   :header-rows: 1
   :class: version-table

* - CloudStack Release
  - Driver Version
  - Capabilities Introduced
  - Supported Datera Product Versions
  - Driver URL
* - 4.5.2
  - v1.0
  - Shared Primary Storage
  - 1.1
  - 

=======
Configuration Options
=======

.. list-table:: Description of Datera CloudStack driver configuration options
  :header-rows: 1
  :class: config-ref-table

  * - Configuration option = Default value
    - Description
  * - **[DEFAULT]**
    -
   * - ``datera_api_port`` = ``7717``
     - (String) Datera API port.
   * - ``datera_api_version`` = ``2``
     - (String) Datera API version.
   * - ``datera_num_replicas`` = ``3``
     - (String) Number of replicas to create of an inode.
   * - ``driver_client_cert`` = ``None``
     - (String) The path to the client certificate for verification, if the driver supports it.
   * - ``driver_client_cert_key`` = ``None``
     - (String) The path to the client certificate key for verification, if the driver supports it.
   * - ``datera_503_timeout`` = ``120``
     - (Int) Timeout for HTTP 503 retry messages
   * - ``datera_503_interval`` = ``5``
     - (Int) Interval between 503 retries
