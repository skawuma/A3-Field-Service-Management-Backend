package com.a3solutions.fsm.workorder;

import com.a3solutions.fsm.dashboard.DashboardBucketProjection;
import com.a3solutions.fsm.dashboard.DashboardDateBucketProjection;
import com.a3solutions.fsm.dashboard.DashboardTechnicianWorkloadProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    long countByAssignedTechIdAndScheduledDateAndStatusNotIn(
            Long assignedTechId,
            LocalDate scheduledDate,
            Collection<WorkOrderStatus> statuses
    );

    long countByAssignedTechIdAndScheduledDateBeforeAndStatusNotIn(
            Long assignedTechId,
            LocalDate scheduledDate,
            Collection<WorkOrderStatus> statuses
    );

    List<WorkOrderEntity> findByAssignedTechIdAndScheduledDateBeforeAndStatusInOrderByScheduledDateAscIdAsc(
            Long assignedTechId,
            LocalDate scheduledDate,
            List<WorkOrderStatus> statuses,
            Pageable pageable
    );

    List<WorkOrderEntity> findByAssignedTechIdAndScheduledDateAndStatusInOrderByIdDesc(
            Long assignedTechId,
            LocalDate scheduledDate,
            List<WorkOrderStatus> statuses,
            Pageable pageable
    );

    @Query(value = """
            select
                t.id as technicianId,
                trim(concat(coalesce(t.first_name, ''), ' ', coalesce(t.last_name, ''))) as technicianName,
                coalesce(sum(case when wo.status not in (:terminalStatuses) then 1 else 0 end), 0) as totalAssignedWorkOrders,
                coalesce(sum(case when wo.status in ('OPEN', 'ASSIGNED') then 1 else 0 end), 0) as openAssignedWorkOrders,
                coalesce(sum(case when wo.status = 'IN_PROGRESS' then 1 else 0 end), 0) as inProgressAssignedWorkOrders,
                coalesce(sum(case
                    when wo.status not in (:terminalStatuses) and wo.scheduled_date = :currentDate then 1
                    else 0
                end), 0) as dueTodayAssignedWorkOrders,
                coalesce(sum(case
                    when wo.status not in (:terminalStatuses) and wo.scheduled_date < :currentDate then 1
                    else 0
                end), 0) as overdueAssignedWorkOrders
            from technicians t
            left join work_orders wo on wo.assigned_tech_id = t.id
            group by t.id, t.first_name, t.last_name
            order by overdueAssignedWorkOrders desc,
                     dueTodayAssignedWorkOrders desc,
                     inProgressAssignedWorkOrders desc,
                     technicianName asc
            """, nativeQuery = true)
    List<DashboardTechnicianWorkloadProjection> findTechnicianWorkloads(
            @Param("currentDate") LocalDate currentDate,
            @Param("terminalStatuses") Collection<String> terminalStatuses
    );



}
