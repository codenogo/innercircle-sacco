package com.innercircle.sacco.contribution.entity;

import com.innercircle.sacco.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a category of contribution (e.g., Shares, Welfare, Merry-Go-Round).
 * Categories can be dynamically created and managed.
 */
@Entity
@Table(name = "contribution_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContributionCategory extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "is_mandatory", nullable = false)
    private boolean mandatory = false;
}
