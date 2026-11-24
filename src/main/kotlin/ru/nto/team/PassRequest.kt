package ru.nto.team

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "pass_request")
class PassRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    var host: Staff? = null,

    @Column(name = "full_name", nullable = false, length = 150)
    var fullName: String = "",

    @Column(name = "phone", nullable = false, length = 12)
    var phone: String = "",

    @Column(name = "organization", length = 150)
    var organization: String? = null,

    @Column(name = "purpose", nullable = false, length = 200)
    var purpose: String = "",

    @Column(name = "valid_from", nullable = false)
    var validFrom: LocalDateTime = LocalDateTime.MIN,

    @Column(name = "valid_to", nullable = false)
    var validTo: LocalDateTime = LocalDateTime.MIN,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: RequestStatus = RequestStatus.PENDING,

    @Column(name = "comment", length = 500)
    var comment: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.MIN,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.MIN,
)
