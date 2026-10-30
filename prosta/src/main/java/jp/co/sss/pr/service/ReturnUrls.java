package jp.co.sss.pr.service;

import java.util.Map;

/** 画面の「戻る」先として受け付けるURLの許可リスト（オープンリダイレクト対策） */
public final class ReturnUrls {

    private static final Map<String, String> ALLOWED = Map.of(
            "/mypage", "マイページ",
            "/admin/menu", "管理メニュー",
            "/admin/questions", "問題管理",
            "/user/list", "ユーザー一覧");

    private ReturnUrls() {
    }

    /** 許可リストにある値だけを返し、それ以外は defaultUrl にする。 */
    public static String resolve(String requested, String defaultUrl) {
        return requested != null && ALLOWED.containsKey(requested) ? requested : defaultUrl;
    }

    public static String label(String url) {
        return ALLOWED.getOrDefault(url, "戻る");
    }
}
