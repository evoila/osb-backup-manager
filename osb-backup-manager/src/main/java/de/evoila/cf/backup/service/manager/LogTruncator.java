package de.evoila.cf.backup.service.manager;

import de.evoila.cf.model.agent.response.*;

import java.util.Map;

final class LogTruncator {

    private static final int LOG_HEAD_BYTES = 64 * 1024;
    private static final int LOG_TAIL_BYTES = 64 * 1024;
    private static final int MAX_UTF8_BYTES_PER_CODEPOINT = 4;

    // Headroom under MongoDB's 16 MB BSON document limit. The remaining ~4 MB
    // covers everything else in the job document (DBRefs, jobLogs, metadata,
    // BSON structural overhead).
    static final int MAX_DOC_BYTES = 12 * 1024 * 1024;
    private static final int MIN_FIELD_HEAD_BYTES = 512;
    private static final int MIN_FIELD_TAIL_BYTES = 512;

    private static final int BACKUP_LOG_FIELDS = 10;
    private static final int RESTORE_LOG_FIELDS = 8;

    private LogTruncator() {}

    static String truncate(String s, int headBytes, int tailBytes) {
        if (s == null) return null;
        int max = headBytes + tailBytes;

        // Fast path: even at worst-case 4 bytes per codepoint the string fits.
        if ((long) s.length() * MAX_UTF8_BYTES_PER_CODEPOINT <= max) return s;

        int headEnd = findHeadEndCharIndex(s, headBytes);
        if (headEnd >= s.length()) return s;

        int tailStart = findTailStartCharIndex(s, tailBytes);
        if (tailStart <= headEnd) return s;

        int omittedBytes = utf8ByteSizeBetween(s, headEnd, tailStart);
        return s.substring(0, headEnd)
                + "\n... [truncated " + omittedBytes + " bytes] ...\n"
                + s.substring(tailStart);
    }

    private static int utf8ByteCount(int codePoint) {
        if (codePoint < 0x80)    return 1;
        if (codePoint < 0x800)   return 2;
        if (codePoint < 0x10000) return 3;
        return 4;
    }

    private static int findHeadEndCharIndex(String s, int byteBudget) {
        int bytes = 0;
        int i = 0;
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            int cost = utf8ByteCount(cp);
            if (bytes + cost > byteBudget) return i;
            bytes += cost;
            i += Character.charCount(cp);
        }
        return i;
    }

    private static int findTailStartCharIndex(String s, int byteBudget) {
        int bytes = 0;
        int i = s.length();
        while (i > 0) {
            int cp = s.codePointBefore(i);
            int cost = utf8ByteCount(cp);
            if (bytes + cost > byteBudget) return i;
            bytes += cost;
            i -= Character.charCount(cp);
        }
        return i;
    }

    private static int utf8ByteSizeBetween(String s, int fromCharIndex, int toCharIndex) {
        int bytes = 0;
        int i = fromCharIndex;
        while (i < toCharIndex) {
            int cp = s.codePointAt(i);
            bytes += utf8ByteCount(cp);
            i += Character.charCount(cp);
        }
        return bytes;
    }

    static void truncateLogs(AgentExecutionResponse r) {
        truncateLogs(r, LOG_HEAD_BYTES, LOG_TAIL_BYTES);
    }

    private static void truncateLogs(AgentExecutionResponse r, int headBytes, int tailBytes) {
        if (r instanceof AgentBackupResponse) {
            AgentBackupResponse b = (AgentBackupResponse) r;
            b.setPreBackupLockLog(truncate(b.getPreBackupLockLog(), headBytes, tailBytes));
            b.setPreBackupLockErrorLog(truncate(b.getPreBackupLockErrorLog(), headBytes, tailBytes));
            b.setPreBackCheckLog(truncate(b.getPreBackCheckLog(), headBytes, tailBytes));
            b.setPreBackCheckErrorLog(truncate(b.getPreBackCheckErrorLog(), headBytes, tailBytes));
            b.setBackupLog(truncate(b.getBackupLog(), headBytes, tailBytes));
            b.setBackupErrorLog(truncate(b.getBackupErrorLog(), headBytes, tailBytes));
            b.setBackupCleanupLog(truncate(b.getBackupCleanupLog(), headBytes, tailBytes));
            b.setBackupCleanupErrorLog(truncate(b.getBackupCleanupErrorLog(), headBytes, tailBytes));
            b.setPostBackupUnlockLog(truncate(b.getPostBackupUnlockLog(), headBytes, tailBytes));
            b.setPostBackupUnlockErrorLog(truncate(b.getPostBackupUnlockErrorLog(), headBytes, tailBytes));
        } else if (r instanceof AgentRestoreResponse) {
            AgentRestoreResponse b = (AgentRestoreResponse) r;
            b.setPreRestoreLockLog(truncate(b.getPreRestoreLockLog(), headBytes, tailBytes));
            b.setPreRestoreLockErrorLog(truncate(b.getPreRestoreLockErrorLog(), headBytes, tailBytes));
            b.setRestoreLog(truncate(b.getRestoreLog(), headBytes, tailBytes));
            b.setRestoreErrorLog(truncate(b.getRestoreErrorLog(), headBytes, tailBytes));
            b.setRestoreCleanupLog(truncate(b.getRestoreCleanupLog(), headBytes, tailBytes));
            b.setRestoreCleanupErrorLog(truncate(b.getRestoreCleanupErrorLog(), headBytes, tailBytes));
            b.setPostRestoreUnlockLog(truncate(b.getPostRestoreUnlockLog(), headBytes, tailBytes));
            b.setPostRestoreUnlockErrorLog(truncate(b.getPostRestoreUnlockErrorLog(), headBytes, tailBytes));
        }
    }

    static boolean enforceDocumentBudget(Map<String, AgentExecutionResponse> responses) {
        return enforceDocumentBudget(responses, MAX_DOC_BYTES);
    }

    /**
     * Re-truncates all log fields in {@code responses} when their combined UTF-8 byte
     * size exceeds {@code maxBytes}, distributing the budget evenly across every log
     * field of every entry. A per-field floor ({@link #MIN_FIELD_HEAD_BYTES} +
     * {@link #MIN_FIELD_TAIL_BYTES}) keeps logs minimally useful even at extreme item
     * counts; in that pathological case the total may still exceed the target and the
     * persistence-failure handling in the service manager acts as the final safety net.
     */
    static boolean enforceDocumentBudget(Map<String, AgentExecutionResponse> responses, long maxBytes) {
        if (responses == null || responses.isEmpty()) return false;

        long currentSize = totalUtf8Bytes(responses);
        if (currentSize <= maxBytes) return false;

        int totalFields = countLogFields(responses);
        if (totalFields == 0) return false;

        long budgetPerField = maxBytes / totalFields;
        int headBudget = (int) Math.max(budgetPerField / 2, MIN_FIELD_HEAD_BYTES);
        int tailBudget = (int) Math.max(budgetPerField / 2, MIN_FIELD_TAIL_BYTES);

        for (AgentExecutionResponse r : responses.values()) {
            truncateLogs(r, headBudget, tailBudget);
        }
        return true;
    }

    private static long totalUtf8Bytes(Map<String, AgentExecutionResponse> responses) {
        long total = 0;
        for (AgentExecutionResponse r : responses.values()) {
            total += responseUtf8Bytes(r);
        }
        return total;
    }

    private static long responseUtf8Bytes(AgentExecutionResponse r) {
        long total = 0;
        if (r instanceof AgentBackupResponse) {
            AgentBackupResponse b = (AgentBackupResponse) r;
            total += utf8ByteSize(b.getPreBackupLockLog());
            total += utf8ByteSize(b.getPreBackupLockErrorLog());
            total += utf8ByteSize(b.getPreBackCheckLog());
            total += utf8ByteSize(b.getPreBackCheckErrorLog());
            total += utf8ByteSize(b.getBackupLog());
            total += utf8ByteSize(b.getBackupErrorLog());
            total += utf8ByteSize(b.getBackupCleanupLog());
            total += utf8ByteSize(b.getBackupCleanupErrorLog());
            total += utf8ByteSize(b.getPostBackupUnlockLog());
            total += utf8ByteSize(b.getPostBackupUnlockErrorLog());
        } else if (r instanceof AgentRestoreResponse) {
            AgentRestoreResponse b = (AgentRestoreResponse) r;
            total += utf8ByteSize(b.getPreRestoreLockLog());
            total += utf8ByteSize(b.getPreRestoreLockErrorLog());
            total += utf8ByteSize(b.getRestoreLog());
            total += utf8ByteSize(b.getRestoreErrorLog());
            total += utf8ByteSize(b.getRestoreCleanupLog());
            total += utf8ByteSize(b.getRestoreCleanupErrorLog());
            total += utf8ByteSize(b.getPostRestoreUnlockLog());
            total += utf8ByteSize(b.getPostRestoreUnlockErrorLog());
        }
        return total;
    }

    private static long utf8ByteSize(String s) {
        if (s == null || s.isEmpty()) return 0;
        return utf8ByteSizeBetween(s, 0, s.length());
    }

    private static int countLogFields(Map<String, AgentExecutionResponse> responses) {
        int count = 0;
        for (AgentExecutionResponse r : responses.values()) {
            if (r instanceof AgentBackupResponse) count += BACKUP_LOG_FIELDS;
            else if (r instanceof AgentRestoreResponse) count += RESTORE_LOG_FIELDS;
        }
        return count;
    }
}
