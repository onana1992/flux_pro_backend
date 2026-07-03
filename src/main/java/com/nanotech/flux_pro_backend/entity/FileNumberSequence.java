package com.nanotech.flux_pro_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "file_number_sequences")
@IdClass(FileNumberSequenceId.class)
@Getter
@Setter
public class FileNumberSequence {

    @Id
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Id
    @Column(nullable = false)
    private int year;

    @Column(name = "last_sequence", nullable = false)
    private int lastSequence;
}
