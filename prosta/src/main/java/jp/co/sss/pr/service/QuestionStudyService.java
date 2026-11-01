package jp.co.sss.pr.service;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.Question;
import jp.co.sss.pr.entity.QuestionNote;
import jp.co.sss.pr.entity.QuestionReport;
import jp.co.sss.pr.entity.QuestionReportReason;
import jp.co.sss.pr.entity.QuestionReportStatus;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.QuestionNoteRepository;
import jp.co.sss.pr.repository.QuestionReportRepository;
import jp.co.sss.pr.repository.QuestionRepository;

/** 問題ごとの本人専用学習メモと、問題・解説の報告を扱う。 */
@Service
public class QuestionStudyService {

    public static final int MAX_TEXT_LENGTH = 1000;
    public static final int ADMIN_PAGE_SIZE = 20;

    private final QuestionRepository questionRepository;
    private final CustomerRepository customerRepository;
    private final QuestionNoteRepository noteRepository;
    private final QuestionReportRepository reportRepository;

    public QuestionStudyService(QuestionRepository questionRepository, CustomerRepository customerRepository,
            QuestionNoteRepository noteRepository, QuestionReportRepository reportRepository) {
        this.questionRepository = questionRepository;
        this.customerRepository = customerRepository;
        this.noteRepository = noteRepository;
        this.reportRepository = reportRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Question> findQuestion(Integer questionId) {
        if (questionId == null || questionId <= 0) {
            return Optional.empty();
        }
        return questionRepository.findById(questionId);
    }

    @Transactional(readOnly = true)
    public Optional<QuestionNote> findNote(Integer userId, Integer questionId) {
        if (userId == null || questionId == null) {
            return Optional.empty();
        }
        return noteRepository.findByCustomerUserIdAndQuestionQuestionId(userId, questionId);
    }

    @Transactional(noRollbackFor = QuestionStudyException.class)
    public QuestionNote saveNote(Integer userId, Integer questionId, String content) {
        String text = normalize(content);
        if (text.isBlank()) {
            throw new QuestionStudyException("メモを入力してください。");
        }
        checkLength(text, "メモは" + MAX_TEXT_LENGTH + "文字以内で入力してください。");
        Question question = requireQuestion(questionId);
        Customer customer = requireCustomer(userId);
        LocalDateTime now = LocalDateTime.now();
        QuestionNote note = noteRepository.findByCustomerUserIdAndQuestionQuestionId(userId, questionId)
                .orElseGet(() -> {
                    QuestionNote created = new QuestionNote();
                    created.setCustomer(customer);
                    created.setQuestion(question);
                    created.setCreatedAt(now);
                    return created;
                });
        note.setContent(text);
        note.setUpdatedAt(now);
        return noteRepository.save(note);
    }

    @Transactional(noRollbackFor = QuestionStudyException.class)
    public boolean deleteNote(Integer userId, Integer questionId) {
        requireQuestion(questionId);
        requireCustomer(userId);
        return noteRepository.deleteByCustomerUserIdAndQuestionQuestionId(userId, questionId) > 0;
    }

    @Transactional(noRollbackFor = QuestionStudyException.class)
    public QuestionReport submitReport(Integer userId, Integer questionId, String reasonValue, String detailValue) {
        QuestionReportReason reason = parseEnum(QuestionReportReason.class, reasonValue);
        if (reason == null) {
            throw new QuestionStudyException("報告理由を選択してください。");
        }
        String detail = normalize(detailValue);
        checkLength(detail, "報告内容は" + MAX_TEXT_LENGTH + "文字以内で入力してください。");
        if (reason == QuestionReportReason.OTHER && detail.isBlank()) {
            throw new QuestionStudyException("「その他」を選んだ場合は報告内容を入力してください。");
        }
        Question question = requireQuestion(questionId);
        Customer customer = requireCustomer(userId);
        if (reportRepository.existsByCustomerUserIdAndQuestionQuestionIdAndReasonAndStatus(
                userId, questionId, reason, QuestionReportStatus.OPEN)) {
            throw new QuestionStudyException("同じ理由の報告はすでに受け付けています。対応をお待ちください。");
        }
        LocalDateTime now = LocalDateTime.now();
        QuestionReport report = new QuestionReport();
        report.setCustomer(customer);
        report.setQuestion(question);
        report.setReason(reason);
        report.setDetail(detail.isBlank() ? null : detail);
        report.setStatus(QuestionReportStatus.OPEN);
        report.setCreatedAt(now);
        report.setUpdatedAt(now);
        return reportRepository.save(report);
    }

    /** 本人がこの問題に送った報告の直近5件 */
    @Transactional(readOnly = true)
    public List<QuestionReport> recentOwnReports(Integer userId, Integer questionId) {
        if (userId == null || questionId == null) {
            return List.of();
        }
        return reportRepository.findTop5ByCustomerUserIdAndQuestionQuestionIdOrderByCreatedAtDescReportIdDesc(
                userId, questionId);
    }

    /** 管理者向けの報告一覧(新しい順、1ページ ADMIN_PAGE_SIZE 件)。範囲外のページは最終ページに丸める。 */
    @Transactional(readOnly = true)
    public Page<QuestionReport> adminReports(QuestionReportStatus status, int page) {
        Page<QuestionReport> result = fetchReports(status, Math.max(page, 0));
        if (result.getNumber() > 0 && result.getNumber() >= result.getTotalPages()) {
            result = fetchReports(status, Math.max(result.getTotalPages() - 1, 0));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<QuestionReportStatus, Long> statusCounts() {
        Map<QuestionReportStatus, Long> counts = new EnumMap<>(QuestionReportStatus.class);
        for (QuestionReportStatus status : QuestionReportStatus.values()) {
            counts.put(status, reportRepository.countByStatus(status));
        }
        return counts;
    }

    @Transactional(noRollbackFor = QuestionStudyException.class)
    public QuestionReport updateStatus(Integer reportId, String statusValue) {
        QuestionReportStatus status = parseEnum(QuestionReportStatus.class, statusValue);
        if (status == null) {
            throw new QuestionStudyException("対応状況の指定が正しくありません。");
        }
        QuestionReport report = (reportId == null || reportId <= 0 ? Optional.<QuestionReport>empty()
                : reportRepository.findById(reportId))
                .orElseThrow(() -> new QuestionStudyException("対象の報告が見つかりません。"));
        report.setStatus(status);
        report.setUpdatedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }

    public static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Page<QuestionReport> fetchReports(QuestionReportStatus status, int page) {
        Pageable pageable = PageRequest.of(page, ADMIN_PAGE_SIZE,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("reportId")));
        return status == null ? reportRepository.findAllBy(pageable) : reportRepository.findByStatus(status, pageable);
    }

    private Question requireQuestion(Integer questionId) {
        return findQuestion(questionId).orElseThrow(() -> new QuestionStudyException("対象の問題が見つかりません。"));
    }

    private Customer requireCustomer(Integer userId) {
        if (userId == null) {
            throw new QuestionStudyException("ユーザー情報が見つかりません。再度ログインしてください。");
        }
        return customerRepository.findById(userId)
                .filter(c -> !Boolean.TRUE.equals(c.getDeleteFlag()))
                .orElseThrow(() -> new QuestionStudyException("ユーザー情報が見つかりません。再度ログインしてください。"));
    }

    // ブラウザが送る改行(CRLF)を1文字として数え、DBの文字数(CHAR)とそろえるためにコードポイント数で判定する
    private static String normalize(String value) {
        return value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n').strip();
    }

    private static void checkLength(String text, String message) {
        if (text.codePointCount(0, text.length()) > MAX_TEXT_LENGTH) {
            throw new QuestionStudyException(message);
        }
    }
}