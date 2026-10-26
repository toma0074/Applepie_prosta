package jp.co.sss.pr.mypage_function;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.FriendConnection;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.CategoryRepository;
import jp.co.sss.pr.repository.CustomerRepository;
import jp.co.sss.pr.repository.FriendConnectionRepository;
import jp.co.sss.pr.repository.FriendStudySummary;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private static final int ONLINE_WINDOW_MINUTES = 10;
    private static final int MAX_COMPARISON_DAYS = 366;
    private static final int MAX_USER_SEARCH_RESULTS = 20;

    private final FriendConnectionRepository friendConnectionRepository;
    private final CustomerRepository customerRepository;
    private final AnswerRepository answerRepository;
    private final CategoryRepository categoryRepository;

    public FriendController(
            FriendConnectionRepository friendConnectionRepository,
            CustomerRepository customerRepository,
            AnswerRepository answerRepository,
            CategoryRepository categoryRepository) {
        this.friendConnectionRepository = friendConnectionRepository;
        this.customerRepository = customerRepository;
        this.answerRepository = answerRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public List<UserSearchResult> searchUsers(
            @RequestParam String query, HttpSession session) {
        Customer currentUser = currentUser(session);
        String normalizedQuery = query.trim();
        if (normalizedQuery.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ユーザー名を入力してください");
        }

        return customerRepository.findByDeleteFlagAndPermissionAndUserNameContaining(
                        false,
                        1,
                        normalizedQuery,
                        PageRequest.of(0, MAX_USER_SEARCH_RESULTS + 1, Sort.by("userName").ascending()))
                .stream()
                .filter(user -> !user.getUserId().equals(currentUser.getUserId()))
                .limit(MAX_USER_SEARCH_RESULTS)
                .map(user -> new UserSearchResult(user.getUserId(), user.getUserName()))
                .toList();
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<FriendSummary> friends(HttpSession session) {
        Customer currentUser = currentUser(session);
        List<Customer> friends = friendConnectionRepository
                .findAllForUserAndStatus(currentUser, FriendConnection.ACCEPTED)
                .stream()
                .map(connection -> otherUser(connection, currentUser))
                .filter(friend -> !Boolean.TRUE.equals(friend.getDeleteFlag()))
                .toList();

        Map<Integer, FriendStudySummary> studyByUser = studySummaries(
                friends.stream().map(Customer::getUserId).toList(), null, null, null);
        LocalDateTime onlineCutoff = LocalDateTime.now().minusMinutes(ONLINE_WINDOW_MINUTES);

        return friends.stream()
                .map(friend -> toFriendSummary(friend, studyByUser.get(friend.getUserId()), onlineCutoff))
                .sorted(Comparator.comparing(FriendSummary::userName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public FriendSummary currentUserSummary(HttpSession session) {
        Customer user = currentUser(session);
        FriendStudySummary study = studySummaries(
                List.of(user.getUserId()), null, null, null).get(user.getUserId());
        return toFriendSummary(
                user, study, LocalDateTime.now().minusMinutes(ONLINE_WINDOW_MINUTES));
    }

    @GetMapping("/requests")
    @Transactional(readOnly = true)
    public FriendRequests requests(HttpSession session) {
        Customer currentUser = currentUser(session);
        List<FriendRequest> incoming = new ArrayList<>();
        List<FriendRequest> outgoing = new ArrayList<>();

        for (FriendConnection connection : friendConnectionRepository
                .findAllForUserAndStatus(currentUser, FriendConnection.PENDING)) {
            Customer other = otherUser(connection, currentUser);
            FriendRequest request = new FriendRequest(
                    connection.getFriendConnectionId(),
                    other.getUserId(),
                    other.getUserName(),
                    connection.getCreatedAt());
            if (connection.getRequestedBy().getUserId().equals(currentUser.getUserId())) {
                outgoing.add(request);
            } else {
                incoming.add(request);
            }
        }
        return new FriendRequests(incoming, outgoing);
    }

    @PostMapping("/requests/{targetUserId}")
    @Transactional
    public ResponseEntity<FriendRequestResult> sendRequest(
            @PathVariable Integer targetUserId, HttpSession session) {
        Customer currentUser = currentUser(session);
        Customer target = customerRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません"));
        if (currentUser.getUserId().equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "自分自身には申請できません");
        }
        if (Boolean.TRUE.equals(target.getDeleteFlag()) || !Integer.valueOf(0).equals(target.getPermission())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ユーザーが見つかりません");
        }

        Customer userLow = currentUser.getUserId() < target.getUserId() ? currentUser : target;
        Customer userHigh = currentUser.getUserId() < target.getUserId() ? target : currentUser;
        FriendConnection connection = friendConnectionRepository
                .findByUserLowAndUserHigh(userLow, userHigh)
                .orElse(null);

        if (connection != null) {
            if (FriendConnection.ACCEPTED.equals(connection.getStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "既に友達です");
            }
            if (FriendConnection.PENDING.equals(connection.getStatus())) {
                String message = connection.getRequestedBy().getUserId().equals(currentUser.getUserId())
                        ? "申請は送信済みです"
                        : "相手からの申請が届いています";
                throw new ResponseStatusException(HttpStatus.CONFLICT, message);
            }
            connection.setRequestedBy(currentUser);
            connection.setStatus(FriendConnection.PENDING);
            connection.setCreatedAt(LocalDateTime.now());
            connection.setUpdatedAt(LocalDateTime.now());
        } else {
            connection = new FriendConnection();
            connection.setUserLow(userLow);
            connection.setUserHigh(userHigh);
            connection.setRequestedBy(currentUser);
            connection.setStatus(FriendConnection.PENDING);
            connection.setCreatedAt(LocalDateTime.now());
            connection.setUpdatedAt(LocalDateTime.now());
        }

        connection = friendConnectionRepository.save(connection);
        return ResponseEntity.status(HttpStatus.CREATED).body(new FriendRequestResult(
                connection.getFriendConnectionId(), target.getUserId(), target.getUserName(),
                connection.getStatus()));
    }

    @PostMapping("/requests/{requestId}/accept")
    @Transactional
    public ResponseEntity<Void> acceptRequest(
            @PathVariable Integer requestId, HttpSession session) {
        Customer currentUser = currentUser(session);
        FriendConnection connection = pendingRequest(requestId, currentUser);
        if (connection.getRequestedBy().getUserId().equals(currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "自分が送信した申請は承認できません");
        }
        connection.setStatus(FriendConnection.ACCEPTED);
        connection.setUpdatedAt(LocalDateTime.now());
        friendConnectionRepository.save(connection);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/{requestId}/reject")
    @Transactional
    public ResponseEntity<Void> rejectRequest(
            @PathVariable Integer requestId, HttpSession session) {
        Customer currentUser = currentUser(session);
        FriendConnection connection = pendingRequest(requestId, currentUser);
        if (connection.getRequestedBy().getUserId().equals(currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "自分が送信した申請は拒否できません");
        }
        connection.setStatus(FriendConnection.REJECTED);
        connection.setUpdatedAt(LocalDateTime.now());
        friendConnectionRepository.save(connection);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/requests/{requestId}")
    @Transactional
    public ResponseEntity<Void> cancelRequest(
            @PathVariable Integer requestId, HttpSession session) {
        Customer currentUser = currentUser(session);
        FriendConnection connection = pendingRequest(requestId, currentUser);
        if (!connection.getRequestedBy().getUserId().equals(currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "受信した申請は取り消せません");
        }
        friendConnectionRepository.delete(connection);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{friendUserId}")
    @Transactional
    public ResponseEntity<Void> removeFriend(
            @PathVariable Integer friendUserId, HttpSession session) {
        Customer currentUser = currentUser(session);
        Customer friend = customerRepository.findById(friendUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "友達が見つかりません"));
        FriendConnection connection = findConnection(currentUser, friend);
        if (!FriendConnection.ACCEPTED.equals(connection.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "友達が見つかりません");
        }
        friendConnectionRepository.delete(connection);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/leaderboard")
    @Transactional(readOnly = true)
    public Leaderboard leaderboard(
            @RequestParam Integer categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpSession session) {
        Customer currentUser = currentUser(session);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "カテゴリが無効です"));
        LocalDate today = LocalDate.now();
        LocalDate fromDate = from == null ? today.withDayOfMonth(1) : from;
        LocalDate toDate = to == null ? today : to;
        if (fromDate.isAfter(toDate) || toDate.isAfter(today)
                || fromDate.isBefore(toDate.minusDays(MAX_COMPARISON_DAYS))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "期間は過去366日以内で指定してください");
        }

        List<Customer> participants = new ArrayList<>();
        participants.add(currentUser);
        friendConnectionRepository.findAllForUserAndStatus(currentUser, FriendConnection.ACCEPTED)
                .stream()
                .map(connection -> otherUser(connection, currentUser))
                .filter(friend -> !Boolean.TRUE.equals(friend.getDeleteFlag()))
                .forEach(participants::add);

        List<Integer> participantIds = participants.stream()
                .map(Customer::getUserId)
                .distinct()
                .toList();
        Map<Integer, FriendStudySummary> studyByUser = studySummaries(
                participantIds, categoryId, fromDate, toDate);
        Map<Integer, Customer> usersById = participants.stream()
                .collect(Collectors.toMap(Customer::getUserId, user -> user, (first, ignored) -> first));

        List<LeaderboardEntry> entries = participantIds.stream()
                .map(userId -> {
                    Customer user = usersById.get(userId);
                    FriendStudySummary study = studyByUser.get(userId);
                    long answers = answerCount(study);
                    long correct = correctCount(study);
                    double accuracy = answers == 0 ? 0.0 : (double) correct * 100 / answers;
                    return new LeaderboardEntry(
                            userId, user.getUserName(), answers, correct, accuracy);
                })
                .sorted(Comparator.comparingDouble(LeaderboardEntry::accuracy).reversed()
                        .thenComparing(Comparator.comparingLong(LeaderboardEntry::correctCount).reversed())
                        .thenComparing(Comparator.comparingLong(LeaderboardEntry::answerCount).reversed())
                        .thenComparing(LeaderboardEntry::userName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new Leaderboard(category.getCategoryId(), category.getCategoryName(),
                fromDate, toDate, entries);
    }

    private Customer currentUser(HttpSession session) {
        CustomerBean userBean = (CustomerBean) session.getAttribute("user");
        if (userBean == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ログインが必要です");
        }
        Customer user = customerRepository.findById(userBean.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ログインが必要です"));
        if (Boolean.TRUE.equals(user.getDeleteFlag()) || !Integer.valueOf(0).equals(user.getPermission())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "一般ユーザーのみ利用できます");
        }
        return user;
    }

    private FriendConnection pendingRequest(Integer requestId, Customer currentUser) {
        FriendConnection connection = friendConnectionRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません"));
        if (!connection.getUserLow().getUserId().equals(currentUser.getUserId())
                && !connection.getUserHigh().getUserId().equals(currentUser.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "申請が見つかりません");
        }
        if (!FriendConnection.PENDING.equals(connection.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "申請は保留中ではありません");
        }
        return connection;
    }

    private FriendConnection findConnection(Customer user, Customer other) {
        Customer userLow = user.getUserId() < other.getUserId() ? user : other;
        Customer userHigh = user.getUserId() < other.getUserId() ? other : user;
        return friendConnectionRepository.findByUserLowAndUserHigh(userLow, userHigh)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "友達が見つかりません"));
    }

    private Customer otherUser(FriendConnection connection, Customer currentUser) {
        return connection.getUserLow().getUserId().equals(currentUser.getUserId())
                ? connection.getUserHigh()
                : connection.getUserLow();
    }

    private Map<Integer, FriendStudySummary> studySummaries(
            List<Integer> userIds, Integer categoryId, LocalDate from, LocalDate to) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return answerRepository.summarizeStudy(userIds, categoryId, from, to).stream()
                .collect(Collectors.toMap(FriendStudySummary::getUserId, summary -> summary));
    }

    private FriendSummary toFriendSummary(
            Customer friend, FriendStudySummary study, LocalDateTime onlineCutoff) {
        LocalDateTime lastSeenAt = friend.getLastSeenAt();
        return new FriendSummary(
                friend.getUserId(),
                friend.getUserName(),
                lastSeenAt != null && lastSeenAt.isAfter(onlineCutoff),
                friend.getConsecutiveLoginDays(),
                answerCount(study),
                correctCount(study));
    }

    private long answerCount(FriendStudySummary study) {
        return study == null || study.getAnswerCount() == null ? 0 : study.getAnswerCount();
    }

    private long correctCount(FriendStudySummary study) {
        return study == null || study.getCorrectCount() == null ? 0 : study.getCorrectCount();
    }

    public record FriendSummary(
            Integer userId,
            String userName,
            boolean online,
            int consecutiveLoginDays,
            long answerCount,
            long correctCount) {
    }

    public record UserSearchResult(Integer userId, String userName) {
    }

    public record FriendRequest(
            Integer requestId,
            Integer userId,
            String userName,
            LocalDateTime createdAt) {
    }

    public record FriendRequests(
            List<FriendRequest> incoming,
            List<FriendRequest> outgoing) {
    }

    public record FriendRequestResult(
            Integer requestId,
            Integer userId,
            String userName,
            String status) {
    }

    public record LeaderboardEntry(
            Integer userId,
            String userName,
            long answerCount,
            long correctCount,
            double accuracy) {
    }

    public record Leaderboard(
            Integer categoryId,
            String categoryName,
            LocalDate from,
            LocalDate to,
            List<LeaderboardEntry> entries) {
    }
}
