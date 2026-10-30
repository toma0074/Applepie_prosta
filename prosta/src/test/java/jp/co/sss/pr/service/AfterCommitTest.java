package jp.co.sss.pr.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class AfterCommitTest {
    @Test
    void retainsRequestContextWhenTheFilterCommitsAfterTheServletReturns() {
        ServletRequestAttributes attributes = new ServletRequestAttributes(new MockHttpServletRequest());
        TransactionSynchronizationManager.initSynchronization();
        RequestContextHolder.setRequestAttributes(attributes);
        AtomicBoolean published = new AtomicBoolean();
        try {
            AfterCommit.run(() -> {
                assertSame(attributes, RequestContextHolder.currentRequestAttributes());
                published.set(true);
            });
            RequestContextHolder.resetRequestAttributes();
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
            assertTrue(published.get());
            assertNull(RequestContextHolder.getRequestAttributes());
        } finally {
            RequestContextHolder.resetRequestAttributes();
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void doesNotPublishStateUntilCommit() {
        AtomicBoolean published = new AtomicBoolean();
        TransactionSynchronizationManager.initSynchronization();
        try {
            AfterCommit.run(() -> published.set(true));
            assertFalse(published.get());
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
            assertTrue(published.get());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void rollbackDoesNotPublishState() {
        AtomicBoolean published = new AtomicBoolean();
        TransactionSynchronizationManager.initSynchronization();
        try {
            AfterCommit.run(() -> published.set(true));
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(sync -> sync.afterCompletion(1));
            assertFalse(published.get());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
