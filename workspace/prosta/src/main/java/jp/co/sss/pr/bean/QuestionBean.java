package jp.co.sss.pr.bean;

public class QuestionBean {
	private Integer questionId;

	private Integer category;

	private Integer difficulty;
	
	private String questionText;
	
	private Integer questionDelete;
	
	private String optionA;
	
	private String optionB;
	
	private String optionC;
	
	private Integer correctOption;

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

	public Integer getQuestionId() {
		return questionId;
	}

	public void setQuestionId(Integer questionId) {
		this.questionId = questionId;
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
