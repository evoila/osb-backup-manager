package de.evoila.cf.backup.service.manager;

import de.evoila.cf.model.agent.response.*;

final class LogTruncator {

    private static final int LOG_HEAD_BYTES = 64 * 1024;
    private static final int LOG_TAIL_BYTES = 64 * 1024;
    private static final int MAX_UTF8_BYTES_PER_CODEPOINT = 4;

    private LogTruncator() {}

    static String truncate(String s) {
        if (s == null) return null;
        int max = LOG_HEAD_BYTES + LOG_TAIL_BYTES;

        // Fast path: even at worst-case 4 bytes per codepoint the string fits.
        if ((long) s.length() * MAX_UTF8_BYTES_PER_CODEPOINT <= max) return s;

        int headEnd = findHeadEndCharIndex(s, LOG_HEAD_BYTES);
        if (headEnd >= s.length()) return s;

        int tailStart = findTailStartCharIndex(s, LOG_TAIL_BYTES);
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
        if (r instanceof AgentBackupResponse) {
            AgentBackupResponse b = (AgentBackupResponse) r;
            b.setPreBackupLockLog(truncate(b.getPreBackupLockLog()));
            b.setPreBackupLockErrorLog(truncate(b.getPreBackupLockErrorLog()));
            b.setPreBackCheckLog(truncate(b.getPreBackCheckLog()));
            b.setPreBackCheckErrorLog(truncate(b.getPreBackCheckErrorLog()));
            b.setBackupLog(truncate(b.getBackupLog()));
            b.setBackupErrorLog(truncate(b.getBackupErrorLog()));
            b.setBackupCleanupLog(truncate(b.getBackupCleanupLog()));
            b.setBackupCleanupErrorLog(truncate(b.getBackupCleanupErrorLog()));
            b.setPostBackupUnlockLog(truncate(b.getPostBackupUnlockLog()));
            b.setPostBackupUnlockErrorLog(truncate(b.getPostBackupUnlockErrorLog()));
        } else if (r instanceof AgentRestoreResponse) {
            AgentRestoreResponse b = (AgentRestoreResponse) r;
            b.setPreRestoreLockLog(truncate(b.getPreRestoreLockLog()));
            b.setPreRestoreLockErrorLog(truncate(b.getPreRestoreLockErrorLog()));
            b.setRestoreLog(truncate(b.getRestoreLog()));
            b.setRestoreErrorLog(truncate(b.getRestoreErrorLog()));
            b.setRestoreCleanupLog(truncate(b.getRestoreCleanupLog()));
            b.setRestoreCleanupErrorLog(truncate(b.getRestoreCleanupErrorLog()));
            b.setPostRestoreUnlockLog(truncate(b.getPostRestoreUnlockLog()));
            b.setPostRestoreUnlockErrorLog(truncate(b.getPostRestoreUnlockErrorLog()));
        }
    }
}
