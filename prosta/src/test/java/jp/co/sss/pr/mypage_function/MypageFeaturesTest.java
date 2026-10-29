package jp.co.sss.pr.mypage_function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Achievement;
import jp.co.sss.pr.entity.Answer;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.UserAchievement;
import jp.co.sss.pr.question_function.AdminMenuController;
import jp.co.sss.pr.repository.AchievementRepository;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.FriendConnectionRepository;
import jp.co.sss.pr.repository.UserAchievementRepository;
import jp.co.sss.pr.service.ChatService;
import jp.co.sss.pr.service.LevelService;
import jp.co.sss.pr.service.QuestService;

/** マイページ・実績・チャット・通知バッジ・管理メニューの正常系／異常系 */
class MypageFeaturesTest {

    private CustomerRepository customerRepository;
    private MockHttpSession session;
    private Customer customer;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        session = new MockHttpSession();
        customer = new Customer();
        customer.setUserId(1);
        customer.setUserLevel(12);
        customer.setLoginCount(5);
        when(customerRepository.getReferenceById(1)).thenReturn(customer);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
    }

    private void login(int permission) {
        CustomerBean bean = new CustomerBean();
        bean.setUserId(1);
        bean.setPermission(permission);
        session.setAttribute("user", bean);
    }

    // ---- マイページ ----

    private MypageController mypage(AnswerRepository answers, LevelService level, QuestService quest,
            UserAchievementRepository uaRepo) {
        MypageController c = new MypageController();
        c.answerRepository = answers;
        c.categoryRepository = mock(CategoryRepository.class);
        c.customerRepository = customerRepository;
        c.levelService = level;
        c.questService = quest;
        c.userAchievementRepository = uaRepo;
        return c;
    }

    @Test
    void mypageRedirectsToTopWhenNotLoggedIn() {
        MypageController c = mypage(mock(AnswerRepository.class), mock(LevelService.class),
                mock(QuestService.class), mock(UserAchievementRepository.class));
        assertEquals("redirect:/", c.mypage(new ExtendedModelMap(), session));
    }

    @Test
    void mypageShowsLevelStatsQuestsAndConsumesFlashAchievements() {
        login(0);
        LevelService level = mock(LevelService.class);
        QuestService quest = mock(QuestService.class);
        UserAchievementRepository uaRepo = mock(UserAchievementRepository.class);
        when(level.getTotalCorrect(customer)).thenReturn(30);
        when(level.getPerfectCount(customer)).thenReturn(2);
        when(quest.dailyQuests(eq(customer), any(LocalDate.class))).thenReturn(List.of());
        when(uaRepo.findByCustomerOrderByUnlockedDateDesc(customer)).thenReturn(List.of());
        session.setAttribute("newAchievements", List.of(new Achievement()));
        ExtendedModelMap model = new ExtendedModelMap();

        String view = mypage(mock(AnswerRepository.class), level, quest, uaRepo).mypage(model, session);

        assertEquals("mypage/mypage", view);
        assertEquals(12, model.get("currentLevel"));
        assertEquals(5, model.get("loginCount"));
        assertEquals(30, model.get("totalCorrect"));
        assertEquals(2, model.get("perfectCount"));
        assertEquals(1, ((List<?>) model.get("newAchievements")).size());
        assertNull(session.getAttribute("newAchievements"));
    }

    @Test
    void historyRedirectsToTopWhenNotLoggedIn() {
        MypageController c = mypage(mock(AnswerRepository.class), mock(LevelService.class),
                mock(QuestService.class), mock(UserAchievementRepository.class));
        assertEquals("redirect:/", c.history(new ExtendedModelMap(), session, null, null, null, null,
                null, null, null, 0));
    }

    private ArgumentCaptor<Pageable> runHistory(Integer sort, String keyword, Integer correct, int page,
            ExtendedModelMap model, ArgumentCaptor<String> kwCaptor, ArgumentCaptor<Integer> correctCaptor) {
        login(0);
        AnswerRepository answers = mock(AnswerRepository.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(answers.searchHistory(any(), any(), any(), kwCaptor.capture(), correctCaptor.capture(), any(),
                any(), pageable.capture())).thenReturn(new PageImpl<Answer>(List.of()));
        when(answers.summarizeByQuestion(any(), eq(1))).thenReturn(List.of());
        MypageController c = mypage(answers, mock(LevelService.class), mock(QuestService.class),
                mock(UserAchievementRepository.class));
        assertEquals("mypage/answer_history",
                c.history(model, session, sort, null, null, keyword, correct, null, null, page));
        return pageable;
    }

    @Test
    void historyDefaultsToNewestFirstWithSixPerPageAndWildcardKeyword() {
        ExtendedModelMap model = new ExtendedModelMap();
        ArgumentCaptor<String> kw = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> correct = ArgumentCaptor.forClass(Integer.class);

        Pageable p = runHistory(null, null, null, 0, model, kw, correct).getValue();

        assertEquals(6, p.getPageSize());
        assertTrue(p.getSort().getOrderFor("ansId").isDescending());
        assertEquals("%%", kw.getValue());
        assertNull(correct.getValue());
        assertEquals(1, model.get("sort"));
    }

    @Test
    void historyHonoursAscendingSortTrimmedKeywordAndCorrectFilter() {
        ExtendedModelMap model = new ExtendedModelMap();
        ArgumentCaptor<String> kw = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> correct = ArgumentCaptor.forClass(Integer.class);

        Pageable p = runHistory(2, "  TCP ", 0, 2, model, kw, correct).getValue();

        assertTrue(p.getSort().getOrderFor("ansId").isAscending());
        assertEquals(2, p.getPageNumber());
        assertEquals("%TCP%", kw.getValue());
        assertEquals(0, correct.getValue());
        assertEquals("TCP", model.get("keyword"));
    }

    @Test
    void historyIgnoresInvalidSortCorrectAndNegativePage() {
        ExtendedModelMap model = new ExtendedModelMap();
        ArgumentCaptor<String> kw = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> correct = ArgumentCaptor.forClass(Integer.class);

        Pageable p = runHistory(9, null, 7, -3, model, kw, correct).getValue();

        assertEquals(0, p.getPageNumber());
        assertNull(correct.getValue());
        assertEquals(1, model.get("sort"));
    }

    // ---- 実績 ----

    private AchievementController achievementController(LevelService level, AchievementRepository aRepo,
            UserAchievementRepository uaRepo) {
        AchievementController c = new AchievementController();
        c.customerRepository = customerRepository;
        c.achievementRepository = aRepo;
        c.userAchievementRepository = uaRepo;
        c.levelService = level;
        return c;
    }

    @Test
    void achievementPageRedirectsToTopWhenNotLoggedIn() {
        AchievementController c = achievementController(mock(LevelService.class),
                mock(AchievementRepository.class), mock(UserAchievementRepository.class));
        assertEquals("redirect:/", c.achievement(new ExtendedModelMap(), session));
    }

    @Test
    void achievementPageMarksUnseenAsSeenButReportsThemAsNew() {
        login(0);
        Achievement a1 = new Achievement();
        a1.setAchievementId(10);
        Achievement a2 = new Achievement();
        a2.setAchievementId(11);
        UserAchievement unseen = new UserAchievement();
        unseen.setAchievement(a1);
        unseen.setSeen(false);
        UserAchievement seen = new UserAchievement();
        seen.setAchievement(a2);
        seen.setSeen(true);
        AchievementRepository aRepo = mock(AchievementRepository.class);
        UserAchievementRepository uaRepo = mock(UserAchievementRepository.class);
        LevelService level = mock(LevelService.class);
        when(aRepo.findAll()).thenReturn(List.of(a1, a2, new Achievement()));
        when(uaRepo.findByCustomerAndSeenFalse(customer)).thenReturn(List.of(unseen));
        when(uaRepo.findByCustomerOrderByUnlockedDateDesc(customer)).thenReturn(List.of(unseen, seen));
        when(level.getPerfectCount(customer)).thenReturn(3);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = achievementController(level, aRepo, uaRepo).achievement(model, session);

        assertEquals("mypage/achievement", view);
        assertTrue(unseen.isSeen());
        assertEquals(List.of(10), model.get("newAchievementIds"));
        assertEquals(List.of(10, 11), model.get("unlockedIds"));
        assertEquals(2, model.get("unlockedCount"));
        assertEquals(3, model.get("totalCount"));
        assertEquals(3, model.get("perfectCount"));
        verify(uaRepo).saveAll(List.of(unseen));
    }

    @Test
    void achievementPageWorksWithNoAchievementsAtAll() {
        login(0);
        AchievementRepository aRepo = mock(AchievementRepository.class);
        UserAchievementRepository uaRepo = mock(UserAchievementRepository.class);
        when(aRepo.findAll()).thenReturn(List.of());
        when(uaRepo.findByCustomerAndSeenFalse(customer)).thenReturn(List.of());
        when(uaRepo.findByCustomerOrderByUnlockedDateDesc(customer)).thenReturn(List.of());
        ExtendedModelMap model = new ExtendedModelMap();

        achievementController(mock(LevelService.class), aRepo, uaRepo).achievement(model, session);

        assertEquals(0, model.get("unlockedCount"));
        assertEquals(0, model.get("totalCount"));
    }

    // ---- チャット ----

    private ChatController chat(ChatService service) {
        return new ChatController(service, customerRepository);
    }

    private MockHttpServletRequest http() {
        MockHttpServletRequest r = new MockHttpServletRequest();
        r.setContextPath("/prosta");
        return r;
    }

    private int statusOf(ChatController c, String message) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> c.chat(new ChatController.ChatRequest(message), session, http()));
        return e.getStatusCode().value();
    }

    @Test
    void chatRepliesForALoggedInUser() {
        login(0);
        ChatService service = mock(ChatService.class);
        ChatService.Reply reply = new ChatService.Reply("こんにちは", List.of());
        when(service.reply(customer, "こんにちは", "/prosta")).thenReturn(reply);

        assertEquals(reply, chat(service).chat(new ChatController.ChatRequest("  こんにちは "), session, http()));
    }

    @Test
    void chatAcceptsExactlyTwoHundredCharacters() {
        login(0);
        ChatService service = mock(ChatService.class);
        String msg = "あ".repeat(200);
        when(service.reply(customer, msg, "/prosta")).thenReturn(new ChatService.Reply("ok", List.of()));

        assertEquals("ok", chat(service).chat(new ChatController.ChatRequest(msg), session, http()).text());
    }

    @Test
    void chatRequiresLogin() {
        assertEquals(401, statusOf(chat(mock(ChatService.class)), "hi"));
    }

    @Test
    void chatRejectsEmptyBlankNullAndTooLongMessages() {
        login(0);
        ChatController c = chat(mock(ChatService.class));
        assertEquals(400, statusOf(c, ""));
        assertEquals(400, statusOf(c, "   "));
        assertEquals(400, statusOf(c, null));
        assertEquals(400, statusOf(c, "あ".repeat(201)));
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> c.chat(null, session, http())).getStatusCode().value());
    }

    @Test
    void chatForADeletedAccountIsUnauthorized() {
        login(0);
        when(customerRepository.findById(1)).thenReturn(Optional.empty());
        ChatService service = mock(ChatService.class);

        assertEquals(401, statusOf(chat(service), "hi"));
        verify(service, never()).reply(any(), any(), any());
    }

    // ---- 通知バッジ ----

    private FriendNotificationAdvice advice(UserAchievementRepository uaRepo, FriendConnectionRepository fRepo) {
        @SuppressWarnings("unchecked")
        ObjectProvider<UserAchievementRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(uaRepo);
        return new FriendNotificationAdvice(fRepo, customerRepository, provider);
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        r.setRequestURI(uri);
        return r;
    }

    @Test
    void badgesAreCountedForALoggedInGeneralUser() {
        login(0);
        UserAchievementRepository uaRepo = mock(UserAchievementRepository.class);
        FriendConnectionRepository fRepo = mock(FriendConnectionRepository.class);
        when(uaRepo.countByCustomerAndSeenFalse(customer)).thenReturn(2L);
        when(fRepo.countIncomingPending(customer)).thenReturn(3L);
        customer.setUserLevel(30);
        customer.setBackgroundSeenLevel(1);
        FriendNotificationAdvice a = advice(uaRepo, fRepo);
        MockHttpServletRequest r = request("/prosta/mypage");

        assertEquals(2L, a.unreadAchievementCount(session, r));
        assertEquals(3L, a.pendingFriendRequestCount(session, r));
        assertEquals(2L, a.newBackgroundCount(session, r));
    }

    @Test
    void badgesAreZeroWhenNotLoggedIn() {
        FriendNotificationAdvice a = advice(mock(UserAchievementRepository.class),
                mock(FriendConnectionRepository.class));
        MockHttpServletRequest r = request("/prosta/");

        assertEquals(0L, a.unreadAchievementCount(session, r));
        assertEquals(0L, a.pendingFriendRequestCount(session, r));
        assertEquals(0L, a.newBackgroundCount(session, r));
    }

    @Test
    void badgesAreZeroForApiRequests() {
        login(0);
        FriendNotificationAdvice a = advice(mock(UserAchievementRepository.class),
                mock(FriendConnectionRepository.class));
        MockHttpServletRequest r = request("/prosta/api/chat");

        assertEquals(0L, a.unreadAchievementCount(session, r));
        assertEquals(0L, a.pendingFriendRequestCount(session, r));
        assertEquals(0L, a.newBackgroundCount(session, r));
    }

    @Test
    void friendRequestBadgeIsNotShownToAdministrators() {
        login(1);
        FriendConnectionRepository fRepo = mock(FriendConnectionRepository.class);
        when(fRepo.countIncomingPending(any())).thenReturn(5L);

        assertEquals(0L, advice(mock(UserAchievementRepository.class), fRepo)
                .pendingFriendRequestCount(session, request("/prosta/mypage")));
    }

    @Test
    void achievementBadgeIsZeroWhenTheRepositoryIsUnavailable() {
        login(0);
        FriendNotificationAdvice a = advice(null, mock(FriendConnectionRepository.class));
        assertEquals(0L, a.unreadAchievementCount(session, request("/prosta/mypage")));
    }

    @Test
    void badgesAreZeroWhenTheAccountNoLongerExists() {
        login(0);
        when(customerRepository.findById(1)).thenReturn(Optional.empty());
        FriendNotificationAdvice a = advice(mock(UserAchievementRepository.class),
                mock(FriendConnectionRepository.class));
        MockHttpServletRequest r = request("/prosta/mypage");

        assertEquals(0L, a.unreadAchievementCount(session, r));
        assertEquals(0L, a.pendingFriendRequestCount(session, r));
        assertEquals(0L, a.newBackgroundCount(session, r));
    }

    // ---- 管理メニュー ----

    @Test
    void adminMenuShowsItsView() {
        assertEquals("admin/admin_menu", new AdminMenuController().menu());
        assertFalse(false);
    }
}
