package jp.co.sss.pr.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "answer")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_ans_gen")
    @SequenceGenerator(name = "seq_ans_gen", sequenceName = "seq_answer", allocationSize = 1)
    @Column(name = "ans_id")
    private Integer ansId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(name = "ans_option", nullable = false)
    private Integer ansOption;

    @Column(name = "correct_option", nullable = false)
    private Integer correctOption;

    /** 正解フラグ（true=正解） */
    @Column(name = "correct_flag", nullable = false)
    private Boolean correctFlag;

    /** 解答日時 */
    @Column(name = "ans_date", nullable = false)
    private LocalDate ansDate;

    // --- getters / setters ---

    public Integer getAnsId() {
        return ansId;
    }

    public void setAnsId(Integer ansId) {
        this.ansId = ansId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public Integer getAnsOption() {
        return ansOption;
    }

    public void setAnsOption(Integer ansOption) {
        this.ansOption = ansOption;
    }

    public Integer getCorrectOption() {
        return correctOption;
    }

    public void setCorrectOption(Integer correctOption) {
        this.correctOption = correctOption;
    }

    public Boolean getCorrectFlag() {
        return correctFlag == null ? false : correctFlag;
    }

    public void setCorrectFlag(Boolean correctFlag) {
        this.correctFlag = correctFlag;
    }

    public LocalDate getAnsDate() {
        return ansDate;
    }

    public void setAnsDate(LocalDate ansDate) {
        this.ansDate = ansDate;
    }
}
