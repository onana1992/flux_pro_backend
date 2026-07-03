package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.enumeration.FileStatus;
import com.nanotech.flux_pro_backend.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChainTemplateUsageService {

    private final FileRepository fileRepository;

    public boolean hasInProgressFiles(UUID templateId) {
        return fileRepository.existsByChainTemplateIdAndStatus(templateId, FileStatus.IN_PROGRESS)
                || fileRepository.existsByChainTemplateIdAndStatus(templateId, FileStatus.ON_HOLD);
    }
}
