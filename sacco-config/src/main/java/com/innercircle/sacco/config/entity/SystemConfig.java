package com.innercircle.sacco.config.entity;

import com.innercircle.sacco.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "system_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    @NotBlank
    private String configKey;

    @Column(nullable = false, length = 1000)
    @NotBlank
    private String configValue;

    @Column(length = 500)
    private String description;
}
