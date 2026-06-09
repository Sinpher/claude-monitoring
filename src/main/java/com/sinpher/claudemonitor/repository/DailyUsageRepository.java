package com.sinpher.claudemonitor.repository;

import com.sinpher.claudemonitor.model.DailyUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 日用量数据访问层，提供日用量汇总的 CRUD 和查询。
 *
 * @author sinpher
 */
@Repository
public interface DailyUsageRepository extends JpaRepository<DailyUsage, LocalDate> {

    /**
     * 查询指定日期范围内的日用量，按日期升序排列。
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return 日用量列表
     */
    List<DailyUsage> findByDateBetweenOrderByDateAsc(LocalDate start, LocalDate end);
}
