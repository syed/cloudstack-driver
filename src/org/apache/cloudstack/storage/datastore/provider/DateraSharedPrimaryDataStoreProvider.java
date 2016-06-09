package org.apache.cloudstack.storage.datastore.provider;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreProvider;
import org.apache.cloudstack.storage.datastore.driver.DateraSharedPrimaryDataStoreDriver;
import org.apache.cloudstack.storage.datastore.lifecycle.DateraSharedPrimaryDataStoreLifeCycle;
import org.apache.cloudstack.storage.datastore.utils.DateraUtil;

import com.cloud.utils.component.ComponentContext;

@Component
public class DateraSharedPrimaryDataStoreProvider implements PrimaryDataStoreProvider {
    private DataStoreLifeCycle lifecycle;
    private PrimaryDataStoreDriver driver;
    private HypervisorHostListener listener;

    DateraSharedPrimaryDataStoreProvider() {
    }

    @Override
    public String getName() {
        return DateraUtil.SHARED_PROVIDER_NAME;
    }

    @Override
    public DataStoreLifeCycle getDataStoreLifeCycle() {
        return lifecycle;
    }

    @Override
    public PrimaryDataStoreDriver getDataStoreDriver() {
        return driver;
    }

    @Override
    public HypervisorHostListener getHostListener() {
        return listener;
    }

    @Override
    public boolean configure(Map<String, Object> params) {
        lifecycle = ComponentContext.inject(DateraSharedPrimaryDataStoreLifeCycle.class);
        driver = ComponentContext.inject(DateraSharedPrimaryDataStoreDriver.class);
        listener = ComponentContext.inject(DateraSharedHostListener.class);

        return true;
    }

    @Override
    public Set<DataStoreProviderType> getTypes() {
        Set<DataStoreProviderType> types = new HashSet<DataStoreProviderType>();

        types.add(DataStoreProviderType.PRIMARY);

        return types;
    }
}
