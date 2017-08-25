package com.timing.job.admin.dao;

import com.timing.job.admin.core.model.TimingJobRegistry;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Created by xuxueli on 16/9/30.
 */
public interface TimingJobRegistryDao {

    public int removeDead(@Param("timeout") int timeout);

    public List<TimingJobRegistry> findAll(@Param("timeout") int timeout);

    public int registryUpdate(@Param("registryGroup") String registryGroup,
                              @Param("registryKey") String registryKey,
                              @Param("registryValue") String registryValue);

    public int registrySave(@Param("registryGroup") String registryGroup,
                            @Param("registryKey") String registryKey,
                            @Param("registryValue") String registryValue);

}
