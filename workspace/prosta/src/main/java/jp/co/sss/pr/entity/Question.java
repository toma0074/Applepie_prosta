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
@Table(name = "question")
public class Question {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_question_gen")
	@SequenceGenerator(name = "seq_question_gen",sequenceName = "seq_question",allocationSize = 1)
	private Integer questionId;
	
	@OneToOne
	@JoinColumn(name = "questionId",referencedColumnName = "question_id")
	private QuestionOption questionOption;

	@Column
	private Integer category;
	
	@Column
	private Integer difficulty;
	
	@Column
	private String questionText;
	
	@Column
	private Integer questionDelete;
	
	public Integer getQuestionId() {
		return questionId;
	}
	
	public void setQuestionId(Integer questionId) {
		this.questionId = questionId;
	}

	public QuestionOption getQuestionOption() {
		return questionOption;
	}

	public void setQuestionOption(QuestionOption questionOption) {
		this.questionOption = questionOption;
	}

	public Integer getCategory() {
		return category;
	}

	public void setCategory(Integer category) {
		this.category = category;
	}

	public Integer getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(Integer difficulty) {
		this.difficulty = difficulty;
	}

	public String getQuestionText() {
		return questionText;
	}

	public void setQuestionText(String questionText) {
		this.questionText = questionText;
	}

	public Integer getQuestionDelete() {
		return questionDelete;
	}

	public void setQuestionDelete(Integer questionDelete) {
		this.questionDelete = questionDelete;
	}

	
	

}
