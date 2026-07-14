package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.enumeration.FileStatus;
import com.nanotech.flux_pro_backend.repository.FilePassageRepository;
import com.nanotech.flux_pro_backend.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChainTemplateUsageService {

    private final FileRepository fileRepository;
    private final FilePassageRepository filePassageRepository;

    public boolean hasInProgressFiles(UUID templateId) {
        return fileRepository.existsByChainTemplateIdAndStatus(templateId, FileStatus.IN_PROGRESS)
                || fileRepository.existsByChainTemplateIdAndStatus(templateId, FileStatus.ON_HOLD);
    }

    /** True si le template est déjà lié à au moins un dossier (tous statuts). */
    public boolean isAssociatedWithFiles(UUID templateId) {
        return fileRepository.existsByChainTemplateId(templateId);
    }

    public boolean isStepInstantiated(UUID chainStepTemplateId) {
        return filePassageRepository.existsByChainStepTemplateId(chainStepTemplateId);
    }
}
