package jp.co.sss.pr.form;

import jakarta.validation.constraints.NotNull;

public class AnswerForm {
	
	private Integer ansId;
	
	private Integer userId;
	
	private Integer category;
	
	private Integer questionId;
	
	@NotNull
	private Integer ansOption;
	
	private Integer correctOption;
	
	private String questionText;
	
	private String optionA;
	
	private String optionB;
	
	private String optionC;

	public Integer getAnsId() {
		return ansId;
	}

	public void setAnsId(Integer ansId) {
		this.ansId = ansId;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public Integer getQuestionId() {
		return questionId;
	}

	public void setQuestionId(Integer questionId) {
		this.questionId = questionId;
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

	public String getQuestionText() {
		return questionText;
	}

	public void setQuestionText(String questionText) {
		this.questionText = questionText;
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

	public Integer getCategory() {
		return category;
	}

	public void setCategory(Integer category) {
		this.category = category;
	}
	
	

}
