package jp.co.sss.pr.mypage_function;

import java.util.List;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jp.co.sss.pr.entity.Category;
import jp.co.sss.pr.repository.CategoryRepository;

/** サイドバーのカテゴリ検索で使う、カテゴリ一覧を全画面に渡す。 */
@ControllerAdvice
public class SidebarCategoryAdvice {

    private final CategoryRepository categoryRepository;

    public SidebarCategoryAdvice(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @ModelAttribute("sidebarCategoryList")
    public List<Category> sidebarCategoryList() {
        return categoryRepository.findAll();
    }
}
