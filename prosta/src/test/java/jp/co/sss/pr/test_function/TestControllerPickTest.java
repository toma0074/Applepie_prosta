package jp.co.sss.pr.test_function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Question;

class TestControllerPickTest {

    @Test
    void pickRandomCapsAtFifteenWithoutDuplicates() {
        List<Question> all = questions(1, 40);

        List<Question> picked = TestController.pickRandom(all);

        assertEquals(15, picked.size());
        assertEquals(15, new HashSet<>(picked).size());
        assertTrue(all.containsAll(picked));
        assertEquals(40, all.size());
    }

    @Test
    void pickRandomReturnsEverythingWhenUnderTheLimit() {
        assertEquals(7, TestController.pickRandom(questions(1, 7)).size());
        assertTrue(TestController.pickRandom(List.of()).isEmpty());
    }

    @Test
    void pickBalancedDoesNotFavourTheLargestCategory() {
        List<Question> all = new ArrayList<>();
        all.addAll(questions(1, 30));
        all.addAll(questions(2, 30));
        all.addAll(questions(3, 30));

        for (int i = 0; i < 20; i++) {
            List<Question> picked = TestController.pickBalanced(all);
            assertEquals(15, picked.size());
            for (int cat = 1; cat <= 3; cat++) {
                final int c = cat;
                assertEquals(5, picked.stream().filter(q -> q.getCategory().getCategoryId() == c).count());
            }
        }
    }

    @Test
    void pickBalancedFillsFromLargerCategoriesWhenOthersRunOut() {
        List<Question> all = new ArrayList<>();
        all.addAll(questions(1, 2));
        all.addAll(questions(2, 30));

        List<Question> picked = TestController.pickBalanced(all);
        Set<Question> unique = new HashSet<>(picked);

        assertEquals(15, picked.size());
        assertEquals(15, unique.size());
        assertEquals(2, picked.stream().filter(q -> q.getCategory().getCategoryId() == 1).count());
    }

    private List<Question> questions(int categoryId, int count) {
        Category c = new Category();
        c.setCategoryId(categoryId);
        List<Question> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Question q = new Question();
            q.setQuestionId(categoryId * 1000 + i);
            q.setCategory(c);
            list.add(q);
        }
        return list;
    }
}
