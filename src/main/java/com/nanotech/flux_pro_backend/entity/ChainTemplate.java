package com.nanotech.flux_pro_backend.entity;

import com.nanotech.flux_pro_backend.enumeration.DelayUnit;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chain_templates")
@Getter
@Setter
public class ChainTemplate extends BaseEntity {

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_type_code", length = 32)
    private String fileTypeCode;

    @Column(name = "total_delay_days", nullable = false)
    private int totalDelayDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "delay_unit", nullable = false, length = 20)
    private DelayUnit delayUnit = DelayUnit.WORKING_DAYS;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "system_template", nullable = false)
    private boolean systemTemplate = false;

    @OneToMany(mappedBy = "chainTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    private List<ChainStepTemplate> steps = new ArrayList<>();
}
