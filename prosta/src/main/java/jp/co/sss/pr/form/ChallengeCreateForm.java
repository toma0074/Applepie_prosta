package jp.co.sss.pr.form;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** フレンド対戦の作成フォーム。値の妥当性はサービス側でも再確認する。 */
public class ChallengeCreateForm {

    @NotNull(message = "対戦する友達を選択してください")
    private Integer friendUserId;

    @NotNull(message = "カテゴリを選択してください")
    private Integer categoryId;

    @NotNull(message = "難易度を選択してください")
    @Min(value = 1, message = "難易度が不正です")
    @Max(value = 3, message = "難易度が不正です")
    private Integer difficulty;

    @NotNull(message = "問題数を選択してください")
    private Integer questionCount;

    public Integer getFriendUserId() { return friendUserId; }
    public void setFriendUserId(Integer friendUserId) { this.friendUserId = friendUserId; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }
}