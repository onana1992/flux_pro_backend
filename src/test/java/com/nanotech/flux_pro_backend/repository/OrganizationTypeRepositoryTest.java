package com.nanotech.flux_pro_backend.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional(readOnly = true)
class OrganizationTypeRepositoryTest {

    @Autowired
    private OrganizationTypeRepository organizationTypeRepository;

    @Test
    void findAllByOrderBySortOrderAsc_returnsAllSeededTypes() {
        var types = organizationTypeRepository.findAllByOrderBySortOrderAsc();
        assertThat(types).hasSize(5);
        assertThat(types.stream().map(t -> t.getCode()).toList())
                .containsExactly(
                        "MINISTRY",
                        "DIRECTORATE",
                        "DIVISION",
                        "SERVICE",
                        "REGIONAL_DIRECTORATE");
        assertThat(types.stream().map(t -> t.getId()).distinct()).hasSize(5);
    }
}
