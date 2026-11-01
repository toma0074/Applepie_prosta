package jp.co.sss.pr.mypage_function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Achievement;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.repository.AchievementRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.UserAchievementRepository;
import jp.co.sss.pr.service.LevelService;

@ExtendWith(MockitoExtension.class)
class AchievementControllerTest {
    @Mock CustomerRepository customers;
    @Mock AchievementRepository achievements;
    @Mock UserAchievementRepository userAchievements;
    @Mock LevelService levels;
    @InjectMocks AchievementController controller;

    @Test
    void displaysAchievementsInSqlIdOrder() {
        CustomerBean bean = new CustomerBean();
        bean.setUserId(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", bean);
        when(customers.getReferenceById(1)).thenReturn(new Customer());
        List<Achievement> rows = new ArrayList<>();
        for (int id : List.of(9, 7, 5, 8, 6)) {
            Achievement achievement = new Achievement();
            achievement.setAchievementId(id);
            rows.add(achievement);
        }
        when(achievements.findAll()).thenReturn(List.copyOf(rows));
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("mypage/achievement", controller.achievement(model, session));
        assertEquals(List.of(rows.get(2), rows.get(4), rows.get(1), rows.get(3), rows.get(0)),
                model.get("allAchievements"));
        assertEquals(List.of(9, 7, 5, 8, 6),
                rows.stream().map(achievement -> achievement.getAchievementId()).toList());
    }
}
