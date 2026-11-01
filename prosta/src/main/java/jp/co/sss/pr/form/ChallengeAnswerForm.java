package jp.co.sss.pr.form;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * フレンド対戦の解答。リクエストの answer1〜answer15 だけを読み取り、
 * 正解や問題IDなどクライアントから送られた他の値は採点に使わない。
 */
public class ChallengeAnswerForm {

    public static final int MAX_POSITION = 15;
    /** 数値として読めなかった値。サービス側で不正値として扱う。 */
    public static final int INVALID = -1;

    private final Map<Integer, Integer> answers;

    public ChallengeAnswerForm(Map<Integer, Integer> answers) {
        this.answers = Collections.unmodifiableMap(new LinkedHashMap<>(answers));
    }

    public static ChallengeAnswerForm fromParameters(Map<String, String> params) {
        Map<Integer, Integer> answers = new LinkedHashMap<>();
        for (int position = 1; position <= MAX_POSITION; position++) {
            String raw = params.get("answer" + position);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                answers.put(position, Integer.valueOf(raw.trim()));
            } catch (NumberFormatException e) {
                answers.put(position, INVALID);
            }
        }
        return new ChallengeAnswerForm(answers);
    }

    public Map<Integer, Integer> getAnswers() {
        return answers;
    }
}