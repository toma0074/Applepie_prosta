package jp.co.sss.pr.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /** カテゴリ・難易度が一致する問題を取得（カテゴリ別テスト用） */
    List<Question> findAllByCategoryAndDifficulty(
            Category category, Integer difficulty);

    /** 複数カテゴリ・難易度が一致する問題を取得（苦手範囲特定テスト用） */
    List<Question> findAllByCategoryInAndDifficulty(
            List<Category> categories, Integer difficulty);

    /** 管理者画面用の絞り込み検索（keyword は LIKE パターン） */
    @Query("""
            SELECT q FROM Question q
            WHERE (:categoryId IS NULL OR q.category.categoryId = :categoryId)
              AND (:difficulty IS NULL OR q.difficulty = :difficulty)
              AND q.questionText LIKE :keyword
              AND (:noExplanation = false OR q.explanation IS NULL OR q.explanation = '')
            ORDER BY q.questionId DESC
            """)
    Page<Question> search(@Param("categoryId") Integer categoryId,
            @Param("difficulty") Integer difficulty,
            @Param("keyword") String keyword,
            @Param("noExplanation") boolean noExplanation,
            Pageable pageable);
}
