package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.FileException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class FileAttachmentServiceTest {

    @InjectMocks
    private FileAttachmentService fileAttachmentService;

    @Test
    void validateFile_rejectsOversizedAttachment() {
        byte[] content = new byte[100];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.pdf",
                "application/pdf",
                content) {
            @Override
            public long getSize() {
                return FileAttachmentService.MAX_SIZE_BYTES + 1;
            }
        };

        assertThatThrownBy(() -> fileAttachmentService.validateFile(file))
                .isInstanceOf(FileException.class)
                .hasMessageContaining("20 MB");
    }
}
