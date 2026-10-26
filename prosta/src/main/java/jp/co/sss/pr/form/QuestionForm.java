package jp.co.sss.pr.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class QuestionForm {

    private Integer questionId;

    @NotNull(message = "カテゴリーを選択してください")
    private Integer category;

    @NotNull(message = "難易度を選択してください")
    @Min(value = 1, message = "難易度を選択してください")
    @Max(value = 3, message = "難易度を選択してください")
    private Integer difficulty;

    @NotBlank
    @Size(min = 5, max = 1000)
    @Pattern(regexp = "^(?!　+$)[\\s\\S]+$", message = "全角スペースのみの入力はできません")
    private String questionText;

    @NotBlank
    @Size(max = 300, message = "選択肢Aは300文字以内で入力してください")
    @Pattern(regexp = "^(?!　+$).+$", message = "全角スペースのみの入力はできません")
    private String optionA;

    @NotBlank
    @Size(max = 300, message = "選択肢Bは300文字以内で入力してください")
    @Pattern(regexp = "^(?!　+$).+$", message = "全角スペースのみの入力はできません")
    private String optionB;

    @NotBlank
    @Size(max = 300, message = "選択肢Cは300文字以内で入力してください")
    @Pattern(regexp = "^(?!　+$).+$", message = "全角スペースのみの入力はできません")
    private String optionC;

    @NotNull(message = "正解を選択してください")
    @Min(value = 1, message = "正解を選択してください")
    @Max(value = 3, message = "正解を選択してください")
    private Integer correctOption;

    @Size(max = 1000, message = "解説は1000文字以内で入力してください")
    private String explanation;

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    // --- getters / setters ---

    public Integer getQuestionId() { return questionId; }
    public void setQuestionId(Integer questionId) { this.questionId = questionId; }

    public Integer getCategory() { return category; }
    public void setCategory(Integer category) { this.category = category; }

    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }

    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }

    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }

    public Integer getCorrectOption() { return correctOption; }
    public void setCorrectOption(Integer correctOption) { this.correctOption = correctOption; }
}
