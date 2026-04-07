package com.a3solutions.fsm.workorder;

import com.a3solutions.fsm.dashboard.DashboardBucketProjection;
import com.a3solutions.fsm.dashboard.DashboardDateBucketProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.workorder
 * @project A3 Field Service Management Backend
 * @date 11/17/25
 */
public interface WorkOrderRepository extends JpaRepository<WorkOrderEntity, Long>, JpaSpecificationExecutor<WorkOrderEntity> {

    long countByStatus(WorkOrderStatus status);
    long countByAssignedTechIdIsNull();
    long countByScheduledDate(LocalDate date);
    long countByScheduledDateAndStatusNotIn(LocalDate date, Collection<WorkOrderStatus> statuses);
    long countByScheduledDateBeforeAndStatusNotIn(LocalDate date, Collection<WorkOrderStatus> statuses);
    long countByStatusAndCompletedAtBetween(WorkOrderStatus status, Instant startInclusive, Instant endExclusive);
    long countByStatusAndPriorityIn(WorkOrderStatus status, Collection<String> priorities);
    @Query(value = """
            select status as bucketKey, count(*) as total
            from work_orders
            group by status
            """, nativeQuery = true)
    List<DashboardBucketProjection> countWorkOrdersByStatus();

    @Query(value = """
            select coalesce(priority, 'UNSPECIFIED') as bucketKey, count(*) as total
            from work_orders
            group by coalesce(priority, 'UNSPECIFIED')
            """, nativeQuery = true)
    List<DashboardBucketProjection> countWorkOrdersByPriority();

    @Query(value = """
            select cast(completed_at as date) as bucketDate, count(*) as total
            from work_orders
            where status = 'COMPLETED'
              and completed_at >= :startInclusive
              and completed_at < :endExclusive
            group by cast(completed_at as date)
            order by bucketDate
            """, nativeQuery = true)
    List<DashboardDateBucketProjection> countCompletionsByCompletedDate(
            Instant startInclusive,
            Instant endExclusive
    );

    List<WorkOrderEntity> findByScheduledDateBeforeAndStatusInOrderByScheduledDateAscIdAsc(
            LocalDate scheduledDate,
            List<WorkOrderStatus> statuses,
            Pageable pageable
    );

    List<WorkOrderEntity> findByScheduledDateAndStatusInOrderByIdDesc(
            LocalDate scheduledDate,
            List<WorkOrderStatus> statuses,
            Pageable pageable
    );

}
