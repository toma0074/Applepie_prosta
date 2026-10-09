package jp.co.sss.pr.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.entity.Question;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {

    /** 全問題を難易度昇順・カテゴリ昇順で取得 */
    List<Question> findAllByOrderByDifficultyAscCategoryAsc();

    /** カテゴリ別に問題を取得 */
    List<Question> findByCategoryOrderByDifficultyAscCategoryAsc(
            Category category);

    /** 問題文のあいまい検索 */
    List<Question> findByQuestionTextContainingOrderByDifficultyAscCategoryAsc(
            String questionText);

    /** 難易度別に問題を取得 */
    List<Question> findByDifficultyOrderByCategoryAsc(
            Integer difficulty);

    /** カテゴリ・難易度以下で問題を取得（カテゴリ別テスト用） */
    List<Question> findAllByCategoryAndDifficultyLessThanEqual(
            Category category, Integer difficulty);

    /** 複数カテゴリ・難易度以下で問題を取得（苦手範囲特定テスト用） */
    List<Question> findAllByCategoryInAndDifficultyLessThanEqual(
            List<Category> categories, Integer difficulty);
}
