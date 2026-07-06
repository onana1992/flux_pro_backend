package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.FileException;
import com.nanotech.flux_pro_backend.dto.request.FileUpdateRequest;
import com.nanotech.flux_pro_backend.entity.ChainTemplate;
import com.nanotech.flux_pro_backend.entity.FileEntity;
import com.nanotech.flux_pro_backend.entity.FileNumberSequence;
import com.nanotech.flux_pro_backend.entity.FileType;
import com.nanotech.flux_pro_backend.entity.Organization;
import com.nanotech.flux_pro_backend.enumeration.FilePriority;
import com.nanotech.flux_pro_backend.enumeration.FileStatus;
import com.nanotech.flux_pro_backend.repository.ChainTemplateRepository;
import com.nanotech.flux_pro_backend.repository.FileAttachmentRepository;
import com.nanotech.flux_pro_backend.repository.FileNumberSequenceRepository;
import com.nanotech.flux_pro_backend.repository.FileRepository;
import com.nanotech.flux_pro_backend.repository.FileTypeRepository;
import com.nanotech.flux_pro_backend.repository.OrganizationRepository;
import com.nanotech.flux_pro_backend.repository.UserRepository;
import com.nanotech.flux_pro_backend.security.AccessControlService;
import com.nanotech.flux_pro_backend.security.OrganizationScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;
    @Mock
    private FileAttachmentRepository fileAttachmentRepository;
    @Mock
    private FileNumberSequenceRepository fileNumberSequenceRepository;
    @Mock
    private FileTypeRepository fileTypeRepository;
    @Mock
    private ChainTemplateRepository chainTemplateRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrganizationScopeService organizationScopeService;
    @Mock
    private AccessControlService accessControlService;

    @InjectMocks
    private FileService fileService;

    private UUID orgId;
    private Organization organization;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        organization = new Organization();
        organization.setId(orgId);
        organization.setCode("DAG");
        organization.setActive(true);
    }

    @Test
    void allocateReferenceNumber_incrementsSequenceForSameOrgAndYear() {
        FileNumberSequence sequence = new FileNumberSequence();
        sequence.setOrganizationId(orgId);
        sequence.setYear(java.time.Year.now(java.time.ZoneId.of("Africa/Douala")).getValue());
        sequence.setLastSequence(41);

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(fileNumberSequenceRepository.findForUpdate(eq(orgId), any(Integer.class)))
                .thenReturn(Optional.of(sequence));
        when(fileNumberSequenceRepository.save(any(FileNumberSequence.class))).thenAnswer(inv -> inv.getArgument(0));

        String reference = fileService.allocateReferenceNumber(orgId);

        assertThat(reference).matches("MINTP-DAG-\\d{4}-0042");
        assertThat(sequence.getLastSequence()).isEqualTo(42);
    }

    @Test
    void allocateReferenceNumber_createsSequenceForNewOrgYear() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(fileNumberSequenceRepository.findForUpdate(eq(orgId), any(Integer.class)))
                .thenReturn(Optional.empty());
        when(fileNumberSequenceRepository.save(any(FileNumberSequence.class))).thenAnswer(inv -> {
            FileNumberSequence seq = inv.getArgument(0);
            if (seq.getLastSequence() == 0) {
                return seq;
            }
            return seq;
        });

        String reference = fileService.allocateReferenceNumber(orgId);

        assertThat(reference).endsWith("-0001");
        ArgumentCaptor<FileNumberSequence> captor = ArgumentCaptor.forClass(FileNumberSequence.class);
        verify(fileNumberSequenceRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().stream().mapToInt(FileNumberSequence::getLastSequence).max())
                .hasValue(1);
    }

    @Test
    void resolveTemplate_courStdReturnsLinkedTemplate() {
        stubActiveFileType("COUR-STD");
        ChainTemplate t01 = template("T01", "COUR-STD");
        when(chainTemplateRepository.findFirstByFileTypeCodeIgnoreCaseAndActiveTrue("COUR-STD"))
                .thenReturn(Optional.of(t01));

        ChainTemplate resolved = fileService.resolveTemplate("COUR-STD", FilePriority.NORMAL);

        assertThat(resolved.getCode()).isEqualTo("T01");
    }

    @Test
    void resolveTemplate_veryUrgentCourStdUsesT02() {
        stubActiveFileType("COUR-STD");
        ChainTemplate t02 = template("T02", "COUR-STD");
        when(chainTemplateRepository.findByCodeIgnoreCase("T02")).thenReturn(Optional.of(t02));

        ChainTemplate resolved = fileService.resolveTemplate("COUR-STD", FilePriority.VERY_URGENT);

        assertThat(resolved.getCode()).isEqualTo("T02");
    }

    @Test
    void update_rejectsInProgressFile() {
        FileEntity file = draftFile();
        file.setStatus(FileStatus.IN_PROGRESS);
        UUID fileId = file.getId();

        when(fileRepository.findByIdWithDetails(fileId)).thenReturn(Optional.of(file));
        doNothing().when(accessControlService).assertCanAccessFile(any(), any());

        FileUpdateRequest request = new FileUpdateRequest(
                "COUR-STD", orgId, "Subject", "Sender", LocalDate.now(), FilePriority.NORMAL, null);

        assertThatThrownBy(() -> fileService.update(fileId, request, null))
                .isInstanceOf(FileException.class)
                .hasMessageContaining("cannot be edited");
    }

    @Test
    void resolveTemplate_throwsWhenNoActiveTemplate() {
        stubActiveFileType("MARCHE-SMP");
        when(chainTemplateRepository.findFirstByFileTypeCodeIgnoreCaseAndActiveTrue("MARCHE-SMP"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.resolveTemplate("MARCHE-SMP", FilePriority.NORMAL))
                .isInstanceOf(FileException.class)
                .extracting(ex -> ((FileException) ex).getCode())
                .isEqualTo("FILE_TEMPLATE_NOT_FOUND_BY_TYPE");
    }

    private void stubActiveFileType(String code) {
        FileType fileType = new FileType();
        fileType.setCode(code);
        fileType.setActive(true);
        when(fileTypeRepository.findByCodeIgnoreCase(code)).thenReturn(Optional.of(fileType));
    }

    private ChainTemplate template(String code, String fileTypeCode) {
        ChainTemplate template = new ChainTemplate();
        template.setId(UUID.randomUUID());
        template.setCode(code);
        template.setFileTypeCode(fileTypeCode);
        template.setActive(true);
        return template;
    }

    private FileEntity draftFile() {
        FileEntity file = new FileEntity();
        file.setId(UUID.randomUUID());
        file.setOrganization(organization);
        file.setStatus(FileStatus.DRAFT);
        return file;
    }
}
