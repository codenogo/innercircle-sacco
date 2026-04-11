package com.innercircle.sacco.member.entity;

import com.innercircle.sacco.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "welfare_beneficiaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WelfareBeneficiary extends BaseEntity {

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false, length = 200)
    private String fullName;

    @Column(nullable = false, length = 50)
    private String relationship;

    @Column
    private LocalDate dateOfBirth;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 500)
    private String notes;
}
