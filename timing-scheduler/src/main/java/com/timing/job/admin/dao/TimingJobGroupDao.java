package com.timing.job.admin.dao;

import com.timing.job.admin.core.model.TimingJobGroup;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TimingJobGroupDao {

    public List<TimingJobGroup> findAll();

    public List<TimingJobGroup> findByAddressType(@Param("addressType") int addressType);

    public int save(TimingJobGroup timingJobGroup);

    public int update(TimingJobGroup timingJobGroup);

    public int remove(@Param("id") int id);

    public TimingJobGroup load(@Param("id") int id);
}
