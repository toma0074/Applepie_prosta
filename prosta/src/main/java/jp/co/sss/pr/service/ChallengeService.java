package jp.co.sss.pr.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jp.co.sss.pr.entity.Achievement;
import jp.co.sss.pr.entity.Answer;
import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.FriendChallenge;
import jp.co.sss.pr.entity.FriendChallengeAnswer;
import jp.co.sss.pr.entity.FriendChallengeParticipant;
import jp.co.sss.pr.entity.FriendChallengeQuestion;
import jp.co.sss.pr.entity.FriendConnection;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.form.ChallengeAnswerForm;
import jp.co.sss.pr.form.ChallengeCreateForm;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.FriendChallengeAnswerRepository;
import jp.co.sss.pr.repository.FriendChallengeParticipantRepository;
import jp.co.sss.pr.repository.FriendChallengeQuestionRepository;
import jp.co.sss.pr.repository.FriendChallengeRepository;
import jp.co.sss.pr.repository.FriendConnectionRepository;
import jp.co.sss.pr.repository.QuestionRepository;

/**
 * 非同期フレンド対戦。作成時に同じ問題セットを固定し、各参加者が1回だけ提出する。
 * 結果の比較は両者の提出後にだけ公開する。
 */
@Service
public class ChallengeService {

    public static final Set<Integer> QUESTION_COUNTS = Set.of(5, 10, 15);
    public static final int MIN_OPTION = 1;
    /** 4 は「わからない」（通常テストと同じ扱い） */
    public static final int MAX_OPTION = 4;

    public enum Phase { ANSWERING, WAITING, COMPLETED }

    public record FriendOption(Integer userId, String userName) {}

    public record ChallengeSummary(Integer challengeId, String opponentName, String categoryName,
            Integer difficulty, Integer questionCount, LocalDateTime createdAt,
            boolean createdByMe, boolean mySubmitted, boolean opponentSubmitted, String statusLabel) {}

    /** 解答画面用。正解・解説は含めない。 */
    public record QuestionView(int position, String questionText,
            String optionA, String optionB, String optionC) {}

    public record ResultRow(int position, String questionText, String optionA, String optionB,
            String optionC, int correctOption, String explanation,
            Integer myOption, boolean myCorrect, Integer opponentOption, boolean opponentCorrect) {}

    public record ChallengeView(Integer challengeId, String categoryName, Integer difficulty,
            Integer questionCount, LocalDateTime createdAt, String myName, String opponentName,
            Phase phase, List<QuestionView> questions, Integer myCorrect, Integer opponentCorrect,
            String outcome, List<ResultRow> results) {}

    public record SubmitResult(int correctCount, int totalCount, List<Achievement> newAchievements) {}

    /** 画面にそのまま表示できる日本語メッセージを持つ業務エラー。 */
    public static class ChallengeException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ChallengeException(String message) {
            super(message);
        }
    }

    /** すでに提出済みの参加者が再提出しようとした。 */
    public static class AlreadySubmittedException extends ChallengeException {
        private static final long serialVersionUID = 1L;

        public AlreadySubmittedException() {
            super("この対戦はすでに提出済みです。");
        }
    }

    @PersistenceContext
    private EntityManager entityManager;

    private final FriendChallengeRepository challengeRepository;
    private final FriendChallengeQuestionRepository challengeQuestionRepository;
    private final FriendChallengeParticipantRepository participantRepository;
    private final FriendChallengeAnswerRepository challengeAnswerRepository;
    private final FriendConnectionRepository friendConnectionRepository;
    private final CustomerRepository customerRepository;
    private final CategoryRepository categoryRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final LevelService levelService;
    private final Random random = new SecureRandom();

    public ChallengeService(FriendChallengeRepository challengeRepository,
            FriendChallengeQuestionRepository challengeQuestionRepository,
            FriendChallengeParticipantRepository participantRepository,
            FriendChallengeAnswerRepository challengeAnswerRepository,
            FriendConnectionRepository friendConnectionRepository,
            CustomerRepository customerRepository,
            CategoryRepository categoryRepository,
            QuestionRepository questionRepository,
            AnswerRepository answerRepository,
            LevelService levelService) {
        this.challengeRepository = challengeRepository;
        this.challengeQuestionRepository = challengeQuestionRepository;
        this.participantRepository = participantRepository;
        this.challengeAnswerRepository = challengeAnswerRepository;
        this.friendConnectionRepository = friendConnectionRepository;
        this.customerRepository = customerRepository;
        this.categoryRepository = categoryRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.levelService = levelService;
    }

    // ----------------------------------------------------------------
    // 作成
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<FriendOption> acceptedFriends(Integer userId) {
        Customer me = activeUser(userId);
        return friendConnectionRepository.findAllForUserAndStatus(me, FriendConnection.ACCEPTED).stream()
                .map(connection -> other(connection, me))
                .filter(this::isActiveGeneralUser)
                .map(friend -> new FriendOption(friend.getUserId(), friend.getUserName()))
                .sorted(Comparator.comparing(FriendOption::userName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(noRollbackFor = ChallengeException.class)
    public Integer create(Integer userId, ChallengeCreateForm form) {
        Customer me = activeUser(userId);
        if (form.getFriendUserId() == null || form.getCategoryId() == null
                || form.getDifficulty() == null || form.getQuestionCount() == null) {
            throw new ChallengeException("友達・カテゴリ・難易度・問題数をすべて選択してください。");
        }
        if (form.getDifficulty() < 1 || form.getDifficulty() > 3) {
            throw new ChallengeException("難易度が不正です。");
        }
        if (!QUESTION_COUNTS.contains(form.getQuestionCount())) {
            throw new ChallengeException("問題数は5問・10問・15問から選択してください。");
        }
        if (me.getUserId().equals(form.getFriendUserId())) {
            throw new ChallengeException("自分自身とは対戦できません。");
        }
        Customer friend = customerRepository.findById(form.getFriendUserId())
                .filter(this::isActiveGeneralUser)
                .orElseThrow(() -> new ChallengeException("承認済みの友達とのみ対戦できます。"));
        lockAcceptedFriendship(me, friend);

        Category category = categoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new ChallengeException("カテゴリが不正です。"));
        List<Question> candidates = new ArrayList<>(
                questionRepository.findAllByCategoryAndDifficulty(category, form.getDifficulty()));
        candidates.removeIf(q -> q.getQuestionId() == null || q.getCorrectOption() == null
                || q.getCorrectOption() < 1 || q.getCorrectOption() > 3);
        if (candidates.size() < form.getQuestionCount()) {
            throw new ChallengeException("選択した条件の問題が" + form.getQuestionCount()
                    + "問に足りません（現在" + candidates.size() + "問）。条件を変えてください。");
        }
        Collections.shuffle(candidates, random);

        FriendChallenge challenge = new FriendChallenge();
        challenge.setCreator(me);
        challenge.setOpponent(friend);
        challenge.setCategoryId(category.getCategoryId());
        challenge.setCategoryName(category.getCategoryName());
        challenge.setDifficulty(form.getDifficulty());
        challenge.setQuestionCount(form.getQuestionCount());
        challenge.setCreatedAt(LocalDateTime.now());
        challenge = challengeRepository.save(challenge);

        for (int i = 0; i < form.getQuestionCount(); i++) {
            Question source = candidates.get(i);
            FriendChallengeQuestion snapshot = new FriendChallengeQuestion();
            snapshot.setChallenge(challenge);
            snapshot.setPosition(i + 1);
            snapshot.setQuestionId(source.getQuestionId());
            snapshot.setQuestionText(source.getQuestionText());
            snapshot.setOptionA(source.getOptionA());
            snapshot.setOptionB(source.getOptionB());
            snapshot.setOptionC(source.getOptionC());
            snapshot.setCorrectOption(source.getCorrectOption());
            snapshot.setExplanation(source.getExplanation());
            challengeQuestionRepository.save(snapshot);
        }
        participantRepository.save(participant(challenge, me));
        participantRepository.save(participant(challenge, friend));
        return challenge.getChallengeId();
    }

    // ----------------------------------------------------------------
    // 一覧・詳細（参加者のみ）
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ChallengeSummary> listFor(Integer userId) {
        List<ChallengeSummary> result = new ArrayList<>();
        for (FriendChallengeParticipant mine : participantRepository.findAllForUser(userId)) {
            FriendChallenge challenge = mine.getChallenge();
            FriendChallengeParticipant theirs = opponentParticipant(challenge, userId);
            boolean createdByMe = challenge.getCreator().getUserId().equals(userId);
            result.add(new ChallengeSummary(challenge.getChallengeId(),
                    theirs.getUser().getUserName(), challenge.getCategoryName(),
                    challenge.getDifficulty(), challenge.getQuestionCount(), challenge.getCreatedAt(),
                    createdByMe, mine.isSubmitted(), theirs.isSubmitted(), statusLabel(mine, theirs)));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ChallengeView view(Integer challengeId, Integer userId) {
        FriendChallengeParticipant mine = participantOrNotFound(challengeId, userId);
        FriendChallenge challenge = mine.getChallenge();
        FriendChallengeParticipant theirs = opponentParticipant(challenge, userId);
        List<FriendChallengeQuestion> questions =
                challengeQuestionRepository.findByChallengeOrderByPositionAsc(challenge);

        if (!mine.isSubmitted()) {
            List<QuestionView> views = questions.stream()
                    .map(q -> new QuestionView(q.getPosition(), q.getQuestionText(),
                            q.getOptionA(), q.getOptionB(), q.getOptionC()))
                    .toList();
            return base(challenge, mine, theirs, Phase.ANSWERING, views, null, null, null, List.of());
        }
        if (!theirs.isSubmitted()) {
            return base(challenge, mine, theirs, Phase.WAITING, List.of(),
                    mine.getCorrectCount(), null, null, List.of());
        }

        Map<Integer, Map<Integer, FriendChallengeAnswer>> answersByParticipant = new HashMap<>();
        for (FriendChallengeAnswer answer : challengeAnswerRepository.findAllByChallenge(challenge)) {
            answersByParticipant
                    .computeIfAbsent(answer.getParticipant().getParticipantId(), k -> new HashMap<>())
                    .put(answer.getChallengeQuestion().getChallengeQuestionId(), answer);
        }
        Map<Integer, FriendChallengeAnswer> myAnswers =
                answersByParticipant.getOrDefault(mine.getParticipantId(), Map.of());
        Map<Integer, FriendChallengeAnswer> theirAnswers =
                answersByParticipant.getOrDefault(theirs.getParticipantId(), Map.of());
        List<ResultRow> rows = new ArrayList<>();
        for (FriendChallengeQuestion q : questions) {
            FriendChallengeAnswer my = myAnswers.get(q.getChallengeQuestionId());
            FriendChallengeAnswer their = theirAnswers.get(q.getChallengeQuestionId());
            rows.add(new ResultRow(q.getPosition(), q.getQuestionText(), q.getOptionA(),
                    q.getOptionB(), q.getOptionC(), q.getCorrectOption(), q.getExplanation(),
                    my == null ? null : my.getAnsOption(), my != null && Boolean.TRUE.equals(my.getCorrectFlag()),
                    their == null ? null : their.getAnsOption(),
                    their != null && Boolean.TRUE.equals(their.getCorrectFlag())));
        }
        int myCorrect = mine.getCorrectCount();
        int theirCorrect = theirs.getCorrectCount();
        String outcome = myCorrect > theirCorrect ? "あなたの勝ち！"
                : myCorrect < theirCorrect ? "あなたの負け…次は勝とう！" : "引き分け！";
        return base(challenge, mine, theirs, Phase.COMPLETED, List.of(),
                myCorrect, theirCorrect, outcome, rows);
    }

    // ----------------------------------------------------------------
    // 提出・採点
    // ----------------------------------------------------------------

    /**
     * 解答を採点し、対戦解答・通常の解答履歴・EXPを同じトランザクションで保存する。
     * 顧客行→参加者行の順にロックし、同じ参加者の同時提出は1件だけ成功させる。
     */
    @Transactional(noRollbackFor = ChallengeException.class)
    public SubmitResult submit(Integer challengeId, Integer userId, ChallengeAnswerForm form) {
        participantOrNotFound(challengeId, userId);
        Customer me = entityManager.find(Customer.class, userId, LockModeType.PESSIMISTIC_WRITE);
        if (me == null || !isActiveGeneralUser(me)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "一般ユーザーのみ利用できます");
        }
        FriendChallengeParticipant mine = participantRepository.lockByChallengeIdAndUserId(challengeId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "対戦が見つかりません"));
        // 先に読み込んだ参加者Entityでも、ロック取得後の最新状態で提出済みか判定する。
        entityManager.refresh(mine, LockModeType.PESSIMISTIC_WRITE);
        if (mine.isSubmitted()) {
            throw new AlreadySubmittedException();
        }

        List<FriendChallengeQuestion> questions =
                challengeQuestionRepository.findByChallengeOrderByPositionAsc(mine.getChallenge());
        Map<Integer, Integer> submitted = form == null ? Map.of() : form.getAnswers();
        for (FriendChallengeQuestion q : questions) {
            Integer option = submitted.get(q.getPosition());
            if (option == null) {
                throw new ChallengeException("すべての問題に解答してください（わからない場合は「わからない」を選択）。");
            }
            if (option < MIN_OPTION || option > MAX_OPTION) {
                throw new ChallengeException("解答の値が不正です。もう一度選択してください。");
            }
        }

        LocalDate today = LocalDate.now();
        int correct = 0;
        for (FriendChallengeQuestion q : questions) {
            int option = submitted.get(q.getPosition());
            boolean isCorrect = option == q.getCorrectOption();
            if (isCorrect) {
                correct++;
            }
            FriendChallengeAnswer answer = new FriendChallengeAnswer();
            answer.setParticipant(mine);
            answer.setChallengeQuestion(q);
            answer.setAnsOption(option);
            answer.setCorrectFlag(isCorrect);
            challengeAnswerRepository.save(answer);
            recordHistory(me, q, option, isCorrect, today);
        }

        mine.setCorrectCount(correct);
        mine.setSubmittedAt(LocalDateTime.now());
        participantRepository.save(mine);

        List<Achievement> unlocked = levelService.processTestResult(me, correct, questions.size());
        return new SubmitResult(correct, questions.size(), unlocked);
    }

    /** 通常テストと同じ形式で解答履歴へ保存する（元の問題が削除済みなら履歴は残せないため省略）。 */
    private void recordHistory(Customer me, FriendChallengeQuestion q, int option,
            boolean isCorrect, LocalDate today) {
        if (!questionRepository.existsById(q.getQuestionId())) {
            return;
        }
        Answer history = new Answer();
        history.setCustomer(me);
        history.setQuestion(questionRepository.getReferenceById(q.getQuestionId()));
        history.setAnsOption(option);
        history.setCorrectOption(q.getCorrectOption());
        history.setCorrectFlag(isCorrect);
        history.setAnsDate(today);
        history.setAnsCount(1);
        answerRepository.save(history);
    }

    // ----------------------------------------------------------------
    // 内部処理
    // ----------------------------------------------------------------

    private ChallengeView base(FriendChallenge challenge, FriendChallengeParticipant mine,
            FriendChallengeParticipant theirs, Phase phase, List<QuestionView> questions,
            Integer myCorrect, Integer opponentCorrect, String outcome, List<ResultRow> rows) {
        return new ChallengeView(challenge.getChallengeId(), challenge.getCategoryName(),
                challenge.getDifficulty(), challenge.getQuestionCount(), challenge.getCreatedAt(),
                mine.getUser().getUserName(), theirs.getUser().getUserName(), phase, questions,
                myCorrect, opponentCorrect, outcome, rows);
    }

    private String statusLabel(FriendChallengeParticipant mine, FriendChallengeParticipant theirs) {
        if (!mine.isSubmitted()) {
            return "あなたの解答待ち";
        }
        if (!theirs.isSubmitted()) {
            return "相手の解答待ち";
        }
        int my = mine.getCorrectCount();
        int their = theirs.getCorrectCount();
        return my > their ? "勝ち" : my < their ? "負け" : "引き分け";
    }

    private FriendChallengeParticipant participantOrNotFound(Integer challengeId, Integer userId) {
        if (challengeId == null || userId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "対戦が見つかりません");
        }
        return participantRepository.findByChallengeIdAndUserId(challengeId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "対戦が見つかりません"));
    }

    private FriendChallengeParticipant opponentParticipant(FriendChallenge challenge, Integer userId) {
        return participantRepository.findAllByChallenge(challenge).stream()
                .filter(p -> !p.getUser().getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "対戦が見つかりません"));
    }

    private FriendChallengeParticipant participant(FriendChallenge challenge, Customer user) {
        FriendChallengeParticipant participant = new FriendChallengeParticipant();
        participant.setChallenge(challenge);
        participant.setUser(user);
        return participant;
    }

    /** 作成中に友達解除されないよう、承認済みの関係行をロックして状態を再確認する。 */
    private void lockAcceptedFriendship(Customer me, Customer friend) {
        Customer low = me.getUserId() < friend.getUserId() ? me : friend;
        Customer high = low == me ? friend : me;
        Optional<FriendConnection> found = friendConnectionRepository.findByUserLowAndUserHigh(low, high);
        if (found.isEmpty()) {
            throw new ChallengeException("承認済みの友達とのみ対戦できます。");
        }
        FriendConnection connection = found.get();
        try {
            entityManager.refresh(connection, LockModeType.PESSIMISTIC_WRITE);
        } catch (EntityNotFoundException e) {
            throw new ChallengeException("承認済みの友達とのみ対戦できます。");
        }
        if (!FriendConnection.ACCEPTED.equals(connection.getStatus())) {
            throw new ChallengeException("承認済みの友達とのみ対戦できます。");
        }
    }

    private Customer activeUser(Integer userId) {
        return customerRepository.findById(userId)
                .filter(this::isActiveGeneralUser)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "一般ユーザーのみ利用できます"));
    }

    private boolean isActiveGeneralUser(Customer customer) {
        return customer != null && !Boolean.TRUE.equals(customer.getDeleteFlag())
                && Integer.valueOf(0).equals(customer.getPermission());
    }

    private Customer other(FriendConnection connection, Customer me) {
        return connection.getUserLow().getUserId().equals(me.getUserId())
                ? connection.getUserHigh() : connection.getUserLow();
    }
}