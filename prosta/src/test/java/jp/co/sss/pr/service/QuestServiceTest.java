package jp.co.sss.pr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import jp.co.sss.pr.service.QuestService.Quest;

class QuestServiceTest {

    private final QuestService service = new QuestService(null);

    @Test
    void noAnswersCompletesNothing() {
        List<Quest> quests = service.buildQuests(0, 0);

        assertTrue(quests.stream().noneMatch(quest -> quest.completed()));
    }

    @Test
    void partialProgressIsReported() {
        List<Quest> quests = service.buildQuests(2, 1);

        assertEquals(2, quests.get(0).progress());
        assertEquals(40, quests.get(0).percent());
        assertFalse(quests.get(1).completed());
    }

    @Test
    void allQuestsCompleteWithFiveCorrectAnswers() {
        List<Quest> quests = service.buildQuests(5, 5);

        assertTrue(quests.stream().allMatch(quest -> quest.completed()));
    }

    @Test
    void accuracyQuestNeedsEnoughAnswers() {
        assertFalse(service.buildQuests(3, 3).get(2).completed());
        assertFalse(service.buildQuests(10, 7).get(2).completed());
        assertTrue(service.buildQuests(10, 8).get(2).completed());
    }
}
