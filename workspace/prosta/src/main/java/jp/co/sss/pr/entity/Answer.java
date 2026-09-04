package jp.co.sss.pr.entity;

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
	@SequenceGenerator(name = "seq_ans_gen",sequenceName = "seq_answer",allocationSize = 1)
	private Integer ansId;
	
	@ManyToOne
	@JoinColumn(name = "user_id",referencedColumnName = "userId")
	private Customer customer;
	
	@ManyToOne
	@JoinColumn(name = "question_id",referencedColumnName = "questionId")
	private Question question;
	
	@Column
	private Integer ansOption;
	
	@Column
	private Integer correctOption;

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
	
	

}
