package jp.co.sss.pr.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class QuestionForm {
	
	private Integer questionId;

	private Integer category;

	private Integer difficulty;
	
	@NotBlank
	@Size(min=5,max=1000)
	@Pattern(regexp = "^(?!　+$)[\\s\\S]+$", message = "全角スペースのみの入力はできません")
	private String questionText;
	
	@NotBlank
	@Size(max=300,message="選択肢Aは300文字以内で入力してください")
	@Pattern(regexp = "^(?!　+$).+$", message = "全角スペースのみの入力はできません")
	private String optionA;
	
	@NotBlank
	@Size(max=300,message="選択肢Bは300文字以内で入力してください")
	@Pattern(regexp = "^(?!　+$).+$", message = "全角スペースのみの入力はできません")
	private String optionB;
	
	@NotBlank
	@Size(max=300,message="選択肢Cは300文字以内で入力してください")
	@Pattern(regexp = "^(?!　+$).+$", message = "全角スペースのみの入力はできません")
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
	
	

}
