package org.apache.cloudstack.storage.test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateraCommon {
    public static final String MANAGEMENT_IP = "172.19.175.170";
    public static final String USERNAME = "admin";
    public static final String PASSWORD = "password";
    public static final int PORT = 7718;
    public static final String DEFAULT_NETWORK_POOL_NAME = "default";
    public static final long DEFAULT_CAPACITY_BYTES = 1073741824L;
    public static final long DEFAULT_CAPACITY_IOPS = 1000L;
    public static final int DEFAULT_REPLICA = 2;
    
    public static final String INITIATOR_1 = "iqn.1994-05.com.xenserver:dc785c10706CLOUDSTACK1";
    public static final String INITIATOR_2 = "iqn.1994-05.com.xenserver:a51d4704CLOUDSTACK2";
    public static final String INITIATOR_3 = "iqn.1994-05.com.xenserver:dc785c10706CLOUDSTACK3";
    public static final String INITIATOR_4 = "iqn.1994-05.com.xenserver:a51d4704CLOUDSTACK4";
    public static final String INITIATOR_5 = "iqn.1994-05.com.xenserver:dc785c10706CLOUDSTACK5";

    public static String generateAppName() {
        DateFormat df = new SimpleDateFormat("dd-MM-yy-HH-mm-ss");
        Date date = new Date();
        String appInstanceName = "App-"+df.format(date);
        return appInstanceName;
    }
}
