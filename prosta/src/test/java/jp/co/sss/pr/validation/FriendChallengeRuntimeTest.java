package jp.co.sss.pr.validation;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.FriendConnection;
import jp.co.sss.pr.entity.Question;

class FriendChallengeRuntimeTest extends ValidationSupport {

    @Autowired JdbcTemplate jdbc;

    /** ログイン済みブラウザ（ユーザーごとに別セッション）。 */
    record Browser(MockHttpSession session, String tab) {}

    Browser open(Customer c) throws Exception {
        browser = new MockHttpSession();
        String token = login(c);
        return new Browser(browser, token);
    }

    MockHttpServletRequestBuilder as(Browser b, MockHttpServletRequestBuilder req) {
        return req.session(b.session()).param("_tab", b.tab());
    }

    Integer createChallenge(Browser b, Customer opponent, int count) throws Exception {
        String redirect = mvc.perform(as(b, post("/challenges"))
                        .param("friendUserId", opponent.getUserId().toString())
                        .param("categoryId", category.getCategoryId().toString())
                        .param("difficulty", "1").param("questionCount", String.valueOf(count)))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        Matcher m = Pattern.compile("/challenges/(\\d+)").matcher(redirect);
        assertTrue(m.find(), redirect);
        return Integer.valueOf(m.group(1));
    }

    MockHttpServletRequestBuilder answerAll(Browser b, Integer id, int count, String option) {
        MockHttpServletRequestBuilder req = as(b, post("/challenges/" + id + "/submit"));
        for (int i = 1; i <= count; i++) {
            req.param("answer" + i, option);
        }
        return req;
    }

    int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    long exp(Customer c) {
        return jdbc.queryForObject("select total_exp from customer where user_id = ?", Long.class, c.getUserId());
    }

    int challengeCount() {
        return count("select count(*) from friend_challenge");
    }

    @Test
    void acceptedFriendsShareIdenticalFixedSnapshotAndComparisonAppearsOnlyAfterBothSubmit() throws Exception {
        acceptedFriendship();
        Browser me = open(user);
        mvc.perform(as(me, get("/challenges"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("フレンド対戦")))
                .andExpect(content().string(containsString(friend.getUserName())));
        Integer id = createChallenge(me, friend, 5);

        List<String> snapshot = jdbc.queryForList(
                "select question_text from friend_challenge_question where challenge_id = ? order by question_order",
                String.class, id);
        assertEquals(5, snapshot.size());
        assertEquals(5, count("select count(distinct question_id) from friend_challenge_question where challenge_id = ?", id));
        assertEquals(2, count("select count(*) from friend_challenge_participant where challenge_id = ?", id));

        // 元問題を後から編集しても、対戦の出題・採点はスナップショットのまま
        for (Question q : questions.findAll()) {
            q.setQuestionText("編集後の問題文");
            q.setCorrectOption(2);
            questions.saveAndFlush(q);
        }

        String myPage = mvc.perform(as(me, get("/challenges/" + id))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (String text : snapshot) {
            assertTrue(myPage.contains(text), "snapshot text shown");
        }
        assertFalse(myPage.contains("編集後の問題文"));
        assertFalse(myPage.contains("int は整数を表す型です。"), "explanation must be hidden before submit");
        assertFalse(myPage.contains("正解："), "correct answer must be hidden before submit");

        Browser other = open(friend);
        String friendPage = mvc.perform(as(other, get("/challenges/" + id))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (String text : snapshot) {
            assertTrue(friendPage.contains(text));
        }

        long myExpBefore = exp(user);
        mvc.perform(answerAll(me, id, 5, "1")).andExpect(status().is3xxRedirection());
        assertEquals(5, count("select correct_count from friend_challenge_participant where challenge_id = ? and user_id = ?",
                id, user.getUserId()));
        assertEquals(5, count("select count(*) from answer where user_id = ? and correct_flag = true", user.getUserId()));
        assertTrue(exp(user) >= myExpBefore + 5 * 2 + 5, "EXP for 5 correct + perfect bonus");

        // 相手が未提出の間は比較・正解を出さない
        mvc.perform(as(me, get("/challenges/" + id))).andExpect(status().isOk())
                .andExpect(content().string(containsString("提出を待っています")))
                .andExpect(content().string(containsString("提出しました")))
                .andExpect(content().string(not(containsString("int は整数を表す型です。"))))
                .andExpect(content().string(not(containsString("正解："))));
        mvc.perform(as(other, get("/challenges/" + id))).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("int は整数を表す型です。"))))
                .andExpect(content().string(not(containsString("提出を待っています"))));

        mvc.perform(answerAll(other, id, 5, "4")).andExpect(status().is3xxRedirection());
        mvc.perform(as(me, get("/challenges/" + id))).andExpect(status().isOk())
                .andExpect(content().string(containsString("勝ち")))
                .andExpect(content().string(containsString("int は整数を表す型です。")))
                .andExpect(content().string(containsString("5 / 5")))
                .andExpect(content().string(containsString("0 / 5")));
        mvc.perform(as(other, get("/challenges/" + id))).andExpect(status().isOk())
                .andExpect(content().string(containsString("負け")));
        mvc.perform(as(other, get("/challenges"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("負け")));
    }

    @Test
    void creationRequiresAcceptedFriendshipAndValidInput() throws Exception {
        Browser me = open(user);
        Customer stranger = customer("S" + user.getUserName(), 0, 1);
        int before = challengeCount();
        // 非フレンド
        mvc.perform(as(me, post("/challenges")).param("friendUserId", stranger.getUserId().toString())
                        .param("categoryId", category.getCategoryId().toString())
                        .param("difficulty", "1").param("questionCount", "5"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("承認済みの友達とのみ対戦できます。")));
        // 申請中
        FriendConnection pending = new FriendConnection();
        pending.setUserLow(user);
        pending.setUserHigh(friend);
        pending.setRequestedBy(user);
        pending.setStatus(FriendConnection.PENDING);
        pending.setCreatedAt(java.time.LocalDateTime.now());
        pending.setUpdatedAt(java.time.LocalDateTime.now());
        connections.saveAndFlush(pending);
        mvc.perform(as(me, post("/challenges")).param("friendUserId", friend.getUserId().toString())
                        .param("categoryId", category.getCategoryId().toString())
                        .param("difficulty", "1").param("questionCount", "5"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("承認済みの友達とのみ対戦できます。")));
        // 自分自身・不正な問題数・不正な型・問題数不足・存在しないカテゴリ
        mvc.perform(as(me, post("/challenges")).param("friendUserId", user.getUserId().toString())
                        .param("categoryId", category.getCategoryId().toString())
                        .param("difficulty", "1").param("questionCount", "5"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("自分自身とは対戦できません。")));
        connections.deleteAll();
        acceptedFriendship();
        mvc.perform(as(me, post("/challenges")).param("friendUserId", friend.getUserId().toString())
                        .param("categoryId", category.getCategoryId().toString())
                        .param("difficulty", "1").param("questionCount", "7"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("問題数は5問・10問・15問から選択してください。")));
        mvc.perform(as(me, post("/challenges")).param("friendUserId", "abc")
                        .param("categoryId", category.getCategoryId().toString())
                        .param("difficulty", "1").param("questionCount", "5"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("error-message")));
        mvc.perform(as(me, post("/challenges")).param("friendUserId", friend.getUserId().toString())
                        .param("categoryId", category.getCategoryId().toString())
                        .param("difficulty", "1").param("questionCount", "10"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("選択した条件の問題が")));
        mvc.perform(as(me, post("/challenges")).param("friendUserId", friend.getUserId().toString())
                        .param("categoryId", "999999")
                        .param("difficulty", "1").param("questionCount", "5"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("カテゴリが不正です。")));
        mvc.perform(as(me, post("/challenges")).param("friendUserId", friend.getUserId().toString())
                        .param("categoryId", category.getCategoryId().toString())
                        .param("difficulty", "5").param("questionCount", "5"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("error-message")));
        assertEquals(before, challengeCount());
    }

    @Test
    void cancelledFriendCannotCreateButExistingChallengeStaysForParticipantsOnly() throws Exception {
        acceptedFriendship();
        Browser me = open(user);
        Integer id = createChallenge(me, friend, 5);
        connections.deleteAll(); // フレンド解除
        int before = challengeCount();
        mvc.perform(as(me, post("/challenges")).param("friendUserId", friend.getUserId().toString())
                        .param("categoryId", category.getCategoryId().toString())
                        .param("difficulty", "1").param("questionCount", "5"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("承認済みの友達とのみ対戦できます。")));
        assertEquals(before, challengeCount());
        mvc.perform(as(me, get("/challenges/" + id))).andExpect(status().isOk());

        Customer outsider = customer("O" + user.getUserName(), 0, 1);
        Browser stranger = open(outsider);
        mvc.perform(as(stranger, get("/challenges/" + id))).andExpect(status().isNotFound());
        mvc.perform(answerAll(stranger, id, 5, "1")).andExpect(status().isNotFound());
        mvc.perform(as(stranger, get("/challenges/999999"))).andExpect(status().isNotFound());
        mvc.perform(as(stranger, get("/challenges"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("まだ対戦はありません。")));
        assertEquals(0, count("select count(*) from friend_challenge_answer a join friend_challenge_participant p on a.participant_id = p.participant_id where p.challenge_id = ?", id));
    }

    @Test
    void gradingUsesDatabaseSnapshotAndRejectsTamperedOrIncompleteAnswers() throws Exception {
        acceptedFriendship();
        Browser me = open(user);
        Integer id = createChallenge(me, friend, 5);
        long expBefore = exp(user);

        // 範囲外の選択肢
        mvc.perform(answerAll(me, id, 4, "1").param("answer5", "9")).andExpect(status().isOk())
                .andExpect(content().string(containsString("解答の値が不正です。")));
        // 未解答
        mvc.perform(answerAll(me, id, 4, "1")).andExpect(status().isOk())
                .andExpect(content().string(containsString("すべての問題に解答してください")));
        // 数値以外
        mvc.perform(answerAll(me, id, 4, "1").param("answer5", "x")).andExpect(status().isOk())
                .andExpect(content().string(containsString("解答の値が不正です。")));
        assertEquals(0, count("select count(*) from friend_challenge_answer a join friend_challenge_participant p on a.participant_id = p.participant_id where p.challenge_id = ?", id));
        assertEquals(0, count("select count(*) from answer where user_id = ?", user.getUserId()));
        assertNull(jdbc.queryForObject("select submitted_at from friend_challenge_participant where challenge_id = ? and user_id = ?",
                java.sql.Timestamp.class, id, user.getUserId()));
        assertEquals(expBefore, exp(user));

        // 正解・問題ID・正解数を偽装しても DB のスナップショットで採点する
        mvc.perform(answerAll(me, id, 5, "2")
                        .param("correctOption", "2").param("correct_option", "2")
                        .param("correctCount", "5").param("questionId", "1").param("answer16", "1"))
                .andExpect(status().is3xxRedirection());
        assertEquals(0, count("select correct_count from friend_challenge_participant where challenge_id = ? and user_id = ?",
                id, user.getUserId()));
        assertEquals(5, count("select count(*) from friend_challenge_answer a join friend_challenge_participant p on a.participant_id = p.participant_id where p.challenge_id = ?", id));
        assertEquals(0, count("select count(*) from friend_challenge_answer a join friend_challenge_participant p on a.participant_id = p.participant_id where p.challenge_id = ? and a.correct_flag = true", id));
        assertEquals(5, count("select count(*) from answer where user_id = ? and correct_flag = false and correct_option = 1 and ans_option = 2",
                user.getUserId()));

        // 二重提出は拒否し、履歴・EXP は増えない
        long expAfter = exp(user);
        mvc.perform(answerAll(me, id, 5, "1")).andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", "この対戦はすでに提出済みです。"));
        assertEquals(5, count("select count(*) from friend_challenge_answer a join friend_challenge_participant p on a.participant_id = p.participant_id where p.challenge_id = ?", id));
        assertEquals(5, count("select count(*) from answer where user_id = ?", user.getUserId()));
        assertEquals(0, count("select correct_count from friend_challenge_participant where challenge_id = ? and user_id = ?",
                id, user.getUserId()));
        assertEquals(expAfter, exp(user));
    }

    @Test
    void nonGeneralUsersAndAnonymousCannotUseChallenges() throws Exception {
        mvc.perform(get("/challenges").session(new MockHttpSession()).param("_tab", java.util.UUID.randomUUID().toString()))
                .andExpect(status().is3xxRedirection()).andExpect(header().string("Location", startsWith("/login/error")));
        Browser adminBrowser = open(admin);
        mvc.perform(as(adminBrowser, get("/challenges"))).andExpect(status().is3xxRedirection());
    }
}