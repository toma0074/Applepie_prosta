package jp.co.sss.pr.service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/** DB保存が確定したときだけ、採点結果やログイン情報をセッションへ公開する。 */
public final class AfterCommit {
    private AfterCommit() {
    }

    public static void run(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            RequestAttributes request = RequestContextHolder.getRequestAttributes();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    RequestAttributes previous = RequestContextHolder.getRequestAttributes();
                    try {
                        // フィルター側での確定時はDispatcherServletのコンテキストが外れていることがある。
                        // セッション等のリクエストスコープProxyを使えるよう、登録時の文脈を一時的に復元する。
                        RequestContextHolder.setRequestAttributes(request);
                        action.run();
                    } finally {
                        RequestContextHolder.setRequestAttributes(previous);
                    }
                }
            });
        } else {
            action.run();
        }
    }
}
