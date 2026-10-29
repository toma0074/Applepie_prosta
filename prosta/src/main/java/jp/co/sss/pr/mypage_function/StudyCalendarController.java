package jp.co.sss.pr.mypage_function;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jp.co.sss.pr.bean.CustomerBean;
import jp.co.sss.pr.entity.Customer;
import jp.co.sss.pr.entity.StudyPlan;
import jp.co.sss.pr.repository.AnswerRepository;
import jp.co.sss.pr.repository.DailyStudySummary;
import jp.co.sss.pr.repository.StudyPlanRepository;
import jp.co.sss.pr.repository.CustomerRepository;

@Controller
public class StudyCalendarController {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_DETAILS_LENGTH = 500;

    private final AnswerRepository answerRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final CustomerRepository customerRepository;

    public StudyCalendarController(
            AnswerRepository answerRepository,
            StudyPlanRepository studyPlanRepository,
            CustomerRepository customerRepository) {
        this.answerRepository = answerRepository;
        this.studyPlanRepository = studyPlanRepository;
        this.customerRepository = customerRepository;
    }

    @GetMapping("/study-calendar")
    public String calendar(
            HttpSession session,
            Model model,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer edit) {
        CustomerBean userBean = (CustomerBean) session.getAttribute("user");
        if (userBean == null) {
            return "redirect:/";
        }

        YearMonth selectedMonth = month == null ? YearMonth.now() : month;
        LocalDate selectedDate = date;
        if (selectedDate == null || !YearMonth.from(selectedDate).equals(selectedMonth)) {
            selectedDate = selectedMonth.equals(YearMonth.now()) ? LocalDate.now() : selectedMonth.atDay(1);
        }

        Customer customer = customerRepository.getReferenceById(userBean.getUserId());
        LocalDate firstDate = selectedMonth.atDay(1);
        LocalDate lastDate = selectedMonth.atEndOfMonth();
        List<DailyStudySummary> summaries =
                answerRepository.summarizeDailyStudy(customer, firstDate, lastDate);
        List<StudyPlan> monthPlans =
                studyPlanRepository.findAllByCustomerAndPlanDateBetweenOrderByPlanDateAscStudyPlanIdAsc(
                        customer, firstDate, lastDate);

        Map<LocalDate, DailyStudySummary> summariesByDate = new HashMap<>();
        summaries.forEach(summary -> summariesByDate.put(summary.getStudyDate(), summary));
        Map<LocalDate, Long> planCountsByDate = new HashMap<>();
        monthPlans.forEach(plan -> planCountsByDate.merge(plan.getPlanDate(), 1L, Long::sum));

        List<List<CalendarDay>> weeks = buildCalendar(selectedMonth, selectedDate, summariesByDate, planCountsByDate);
        List<StudyPlan> selectedPlans =
                studyPlanRepository.findAllByCustomerAndPlanDateOrderByStudyPlanIdAsc(customer, selectedDate);

        StudyPlan editingPlan = null;
        if (edit != null) {
            editingPlan = studyPlanRepository.findByStudyPlanIdAndCustomer(edit, customer)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            selectedDate = editingPlan.getPlanDate();
        }

        DailyStudySummary selectedSummary = summariesByDate.get(selectedDate);
        long answerCount = selectedSummary == null ? 0 : selectedSummary.getAnswerCount();
        long correctCount = selectedSummary == null ? 0 : selectedSummary.getCorrectCount();

        model.addAttribute("month", selectedMonth);
        model.addAttribute("monthLabel", selectedMonth.format(MONTH_FORMAT));
        model.addAttribute("previousMonth", selectedMonth.minusMonths(1));
        model.addAttribute("nextMonth", selectedMonth.plusMonths(1));
        model.addAttribute("weeks", weeks);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("selectedPlans", selectedPlans);
        model.addAttribute("editingPlan", editingPlan);
        model.addAttribute("answerCount", answerCount);
        model.addAttribute("correctCount", correctCount);
        model.addAttribute("incorrectCount", answerCount - correctCount);
        return "mypage/study_calendar";
    }

    @PostMapping("/study-calendar/plans")
    public String createPlan(
            HttpSession session,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate planDate,
            @RequestParam String title,
            @RequestParam(required = false) String details,
            RedirectAttributes redirectAttributes) {
        Customer customer = currentCustomer(session);
        String validationError = validatePlan(title, details);
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("planError", validationError);
            return redirectToCalendar(YearMonth.from(planDate), planDate);
        }

        StudyPlan plan = new StudyPlan();
        plan.setCustomer(customer);
        plan.setPlanDate(planDate);
        plan.setTitle(title.trim());
        plan.setDetails(normalizeDetails(details));
        studyPlanRepository.save(plan);
        redirectAttributes.addFlashAttribute("planMessage", "学習予定を登録しました。");
        return redirectToCalendar(YearMonth.from(planDate), planDate);
    }

    @PostMapping("/study-calendar/plans/{id}")
    public String updatePlan(
            HttpSession session,
            @PathVariable Integer id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate planDate,
            @RequestParam String title,
            @RequestParam(required = false) String details,
            RedirectAttributes redirectAttributes) {
        Customer customer = currentCustomer(session);
        StudyPlan plan = findOwnedPlan(id, customer);
        String validationError = validatePlan(title, details);
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("planError", validationError);
            return redirectToCalendar(YearMonth.from(plan.getPlanDate()), plan.getPlanDate());
        }

        plan.setPlanDate(planDate);
        plan.setTitle(title.trim());
        plan.setDetails(normalizeDetails(details));
        studyPlanRepository.save(plan);
        redirectAttributes.addFlashAttribute("planMessage", "学習予定を更新しました。");
        return redirectToCalendar(YearMonth.from(planDate), planDate);
    }

    @PostMapping("/study-calendar/plans/{id}/delete")
    public String deletePlan(
            HttpSession session,
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {
        Customer customer = currentCustomer(session);
        StudyPlan plan = findOwnedPlan(id, customer);
        studyPlanRepository.delete(plan);
        redirectAttributes.addFlashAttribute("planMessage", "学習予定を削除しました。");
        return redirectToCalendar(YearMonth.from(plan.getPlanDate()), plan.getPlanDate());
    }

    private List<List<CalendarDay>> buildCalendar(
            YearMonth month,
            LocalDate selectedDate,
            Map<LocalDate, DailyStudySummary> summaries,
            Map<LocalDate, Long> planCounts) {
        LocalDate firstDate = month.atDay(1);
        int mondayBasedOffset = firstDate.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        LocalDate gridStart = firstDate.minusDays(mondayBasedOffset);
        int cellCount = ((mondayBasedOffset + month.lengthOfMonth() + 6) / 7) * 7;

        List<List<CalendarDay>> weeks = new ArrayList<>();
        for (int index = 0; index < cellCount; index++) {
            LocalDate cellDate = gridStart.plusDays(index);
            DailyStudySummary summary = summaries.get(cellDate);
            long answers = summary == null ? 0 : summary.getAnswerCount();
            long plans = planCounts.getOrDefault(cellDate, 0L);
            CalendarDay day = new CalendarDay(
                    cellDate,
                    cellDate.getDayOfMonth(),
                    YearMonth.from(cellDate).equals(month),
                    cellDate.equals(LocalDate.now()),
                    cellDate.equals(selectedDate),
                    answers,
                    plans);
            if (index % 7 == 0) {
                weeks.add(new ArrayList<>());
            }
            weeks.get(weeks.size() - 1).add(day);
        }
        return weeks;
    }

    private Customer currentCustomer(HttpSession session) {
        CustomerBean user = (CustomerBean) session.getAttribute("user");
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ログインが必要です");
        }
        return customerRepository.getReferenceById(user.getUserId());
    }

    private StudyPlan findOwnedPlan(Integer id, Customer customer) {
        // URLの予定IDだけを信用せず、ログイン中の所有者とセットで取得する。
        return studyPlanRepository.findByStudyPlanIdAndCustomer(id, customer)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private String validatePlan(String title, String details) {
        if (title == null || title.trim().isEmpty()) {
            return "予定のタイトルを入力してください。";
        }
        if (title.trim().length() > MAX_TITLE_LENGTH) {
            return "タイトルは100文字以内で入力してください。";
        }
        if (details != null && details.length() > MAX_DETAILS_LENGTH) {
            return "詳細は500文字以内で入力してください。";
        }
        return null;
    }

    private String normalizeDetails(String details) {
        return details == null || details.isBlank() ? null : details.trim();
    }

    private String redirectToCalendar(YearMonth month, LocalDate date) {
        return "redirect:/study-calendar?month=" + month.format(MONTH_FORMAT) + "&date=" + date;
    }

    public static class CalendarDay {
        private final LocalDate date;
        private final int dayNumber;
        private final boolean currentMonth;
        private final boolean today;
        private final boolean selected;
        private final long answerCount;
        private final long planCount;

        CalendarDay(
                LocalDate date,
                int dayNumber,
                boolean currentMonth,
                boolean today,
                boolean selected,
                long answerCount,
                long planCount) {
            this.date = date;
            this.dayNumber = dayNumber;
            this.currentMonth = currentMonth;
            this.today = today;
            this.selected = selected;
            this.answerCount = answerCount;
            this.planCount = planCount;
        }

        public LocalDate getDate() { return date; }
        public int getDayNumber() { return dayNumber; }
        public boolean isCurrentMonth() { return currentMonth; }
        public boolean isToday() { return today; }
        public boolean isSelected() { return selected; }
        public long getAnswerCount() { return answerCount; }
        public long getPlanCount() { return planCount; }
    }
}
