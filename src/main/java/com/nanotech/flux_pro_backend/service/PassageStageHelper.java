package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.dto.request.ChainStepTemplateRequest;
import com.nanotech.flux_pro_backend.entity.ChainStepTemplate;
import com.nanotech.flux_pro_backend.entity.FilePassage;
import com.nanotech.flux_pro_backend.enumeration.PassageStatus;

import java.util.Comparator;
import java.util.List;

public final class PassageStageHelper {

    private PassageStageHelper() {
    }

    public static List<Integer> distinctStagesFromSteps(List<ChainStepTemplate> steps) {
        return steps.stream().map(ChainStepTemplate::getStepOrder).distinct().sorted().toList();
    }

    public static List<Integer> distinctStagesFromRequests(List<ChainStepTemplateRequest> steps) {
        return steps.stream().map(ChainStepTemplateRequest::stepOrder).distinct().sorted().toList();
    }

    public static List<Integer> distinctStagesFromPassages(List<FilePassage> passages) {
        return passages.stream().map(FilePassage::getStepOrder).distinct().sorted().toList();
    }

    public static List<FilePassage> passagesInStage(List<FilePassage> passages, int stageOrder) {
        return passages.stream()
                .filter(p -> p.getStepOrder() == stageOrder)
                .sorted(Comparator.comparing(
                        p -> p.getChainStepTemplate() != null
                                ? p.getChainStepTemplate().getLabel()
                                : "",
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public static boolean isStageComplete(List<FilePassage> passages, int stageOrder) {
        List<FilePassage> stagePassages = passagesInStage(passages, stageOrder);
        return !stagePassages.isEmpty()
                && stagePassages.stream().allMatch(p -> p.getStatus() == PassageStatus.COMPLETED);
    }

    public static Integer nextStage(List<FilePassage> passages, int currentStageOrder) {
        return passages.stream()
                .map(FilePassage::getStepOrder)
                .filter(order -> order > currentStageOrder)
                .min(Integer::compareTo)
                .orElse(null);
    }

    public static List<FilePassage> activePassages(List<FilePassage> passages) {
        return passages.stream()
                .filter(p -> p.getStatus() == PassageStatus.IN_PROGRESS
                        || p.getStatus() == PassageStatus.SUSPENDED)
                .sorted(Comparator.comparingInt(FilePassage::getStepOrder)
                        .thenComparing(p -> p.getChainStepTemplate() != null
                                ? p.getChainStepTemplate().getLabel()
                                : ""))
                .toList();
    }
}
