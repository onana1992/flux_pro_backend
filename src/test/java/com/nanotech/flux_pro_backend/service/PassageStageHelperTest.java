package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.entity.FilePassage;
import com.nanotech.flux_pro_backend.enumeration.PassageStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PassageStageHelperTest {

    @Test
    void isStageComplete_requiresAllParallelMaillonsCompleted() {
        List<FilePassage> passages = List.of(
                passage(2, PassageStatus.COMPLETED),
                passage(2, PassageStatus.IN_PROGRESS),
                passage(3, PassageStatus.PENDING));

        assertFalse(PassageStageHelper.isStageComplete(passages, 2));
    }

    @Test
    void isStageComplete_trueWhenAllParallelMaillonsCompleted() {
        List<FilePassage> passages = List.of(
                passage(2, PassageStatus.COMPLETED),
                passage(2, PassageStatus.COMPLETED),
                passage(2, PassageStatus.COMPLETED),
                passage(3, PassageStatus.PENDING));

        assertTrue(PassageStageHelper.isStageComplete(passages, 2));
    }

    @Test
    void nextStage_skipsToNextDistinctStepOrder() {
        List<FilePassage> passages = List.of(
                passage(1, PassageStatus.COMPLETED),
                passage(2, PassageStatus.COMPLETED),
                passage(2, PassageStatus.COMPLETED),
                passage(4, PassageStatus.PENDING));

        assertEquals(4, PassageStageHelper.nextStage(passages, 2));
        assertNull(PassageStageHelper.nextStage(passages, 4));
    }

    private static FilePassage passage(int stepOrder, PassageStatus status) {
        FilePassage passage = new FilePassage();
        passage.setStepOrder(stepOrder);
        passage.setStatus(status);
        return passage;
    }
}
