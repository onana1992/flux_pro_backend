package com.nanotech.flux_pro_backend.entity;

import com.nanotech.flux_pro_backend.enumeration.DelayUnit;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "chain_step_templates")
@Getter
@Setter
public class ChainStepTemplate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chain_template_id", nullable = false)
    private ChainTemplate chainTemplate;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "responsible_role", nullable = false, length = 30)
    private UserRole responsibleRole;

    @Column(name = "delay_value", nullable = false)
    private int delayValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "delay_unit", nullable = false, length = 20)
    private DelayUnit delayUnit = DelayUnit.WORKING_DAYS;

    @Column(name = "expected_action", length = 500)
    private String expectedAction;

    @Column(nullable = false)
    private boolean optional;

    @Column(name = "closure_step", nullable = false)
    private boolean closureStep;
}
