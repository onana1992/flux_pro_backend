package com.nanotech.flux_pro_backend.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FileNumberSequenceId implements Serializable {

    private UUID organizationId;
    private int year;
}
