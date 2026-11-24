package ru.nto.team

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface StaffRepository : JpaRepository<Staff, Long> {
    fun findByCode(code: String): Staff?
}

interface PassRequestRepository : JpaRepository<PassRequest, Long> {

    @Query("select r from PassRequest r where r.id = :id and r.host.id = :hostId")
    fun findOwned(@Param("id") id: Long, @Param("hostId") hostId: Long): PassRequest?

    fun findByHostIdOrderByValidFromAscIdAsc(hostId: Long): List<PassRequest>

    fun findByHostIdAndStatusOrderByValidFromAscIdAsc(hostId: Long, status: RequestStatus): List<PassRequest>

    @Query(
        """
        select count(r) from PassRequest r
        where r.host.id = :hostId
          and r.phone = :phone
          and r.status in :statuses
          and r.validFrom < :newTo
          and :newFrom < r.validTo
        """,
    )
    fun countOverlapping(
        @Param("hostId") hostId: Long,
        @Param("phone") phone: String,
        @Param("statuses") statuses: Collection<RequestStatus>,
        @Param("newFrom") newFrom: LocalDateTime,
        @Param("newTo") newTo: LocalDateTime,
    ): Long
}
