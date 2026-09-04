package jp.co.sss.pr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "question_option")
public class QuestionOption {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_option_gen")
	@SequenceGenerator(name = "seq_option_gen",sequenceName = "seq_option",allocationSize = 1)
	private Integer optionId;
	
	@OneToOne
	@JoinColumn(name = "question_id",referencedColumnName = "questionId")
	private Question question;
	
	@Column(name = "option_a")
	private String optionA;
	
	@Column(name = "option_b")
	private String optionB;
	
	@Column(name = "option_c")
	private String optionC;
	
	@Column
	private Integer correctOption;

	public Integer getOptionId() {
		return optionId;
	}

	public void setOptionId(Integer optionId) {
		this.optionId = optionId;
	}

	public Question getQuestion() {
		return question;
	}

	public void setQuestion(Question question) {
		this.question = question;
	}

	public String getOptionA() {
		return optionA;
	}

	public void setOptionA(String optionA) {
		this.optionA = optionA;
	}

	public String getOptionB() {
		return optionB;
	}

	public void setOptionB(String optionB) {
		this.optionB = optionB;
	}

	public String getOptionC() {
		return optionC;
	}

	public void setOptionC(String optionC) {
		this.optionC = optionC;
	}

	public Integer getCorrectOption() {
		return correctOption;
	}

	public void setCorrectOption(Integer correctOption) {
		this.correctOption = correctOption;
	}
	
	

}
