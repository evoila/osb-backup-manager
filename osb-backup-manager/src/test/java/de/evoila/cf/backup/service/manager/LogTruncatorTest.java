package de.evoila.cf.backup.service.manager;

import de.evoila.cf.model.agent.response.AgentBackupResponse;
import de.evoila.cf.model.agent.response.AgentExecutionResponse;
import de.evoila.cf.model.agent.response.AgentRestoreResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogTruncatorTest {

    // Mirror the production constants. Kept in sync manually since they are private.
    private static final int LOG_HEAD_BYTES = 64 * 1024;
    private static final int LOG_TAIL_BYTES = 64 * 1024;
    private static final int MAX_BYTES = LOG_HEAD_BYTES + LOG_TAIL_BYTES;

    // Upper bound for the truncation marker the truncator inserts.
    // Anything realistic ("\n... [truncated N bytes] ...\n") fits comfortably.
    private static final int MARKER_OVERHEAD_TOLERANCE_BYTES = 128;

    @Test
    void returnsNullForNullInput() {
        assertNull(LogTruncator.truncate(null, LOG_HEAD_BYTES, LOG_TAIL_BYTES));
    }

    @Test
    void returnsEmptyForEmptyInput() {
        assertEquals("", LogTruncator.truncate("", LOG_HEAD_BYTES, LOG_TAIL_BYTES));
    }

    @Test
    void asciiUnderLimitIsUnchanged() {
        String s = "ein kurzer log eintrag";
        assertEquals(s, LogTruncator.truncate(s, LOG_HEAD_BYTES, LOG_TAIL_BYTES));
    }

    @Test
    void asciiOverLimitIsTruncatedToByteBudget() {
        // 'a' is 1 byte in UTF-8 → char count equals byte count.
        String s = repeat("a", MAX_BYTES * 2);
        assertWithinByteBudget("ASCII", LogTruncator.truncate(s, LOG_HEAD_BYTES, LOG_TAIL_BYTES));
    }

    @Test
    void umlautOverLimitIsTruncatedToByteBudget() {
        // 'Ä' is 2 bytes in UTF-8. MAX_BYTES copies produce MAX_BYTES chars but
        // 2 * MAX_BYTES bytes — a char-based truncator returns this unchanged,
        // a byte-based one must truncate.
        String s = repeat("Ä", MAX_BYTES);
        assertWithinByteBudget("Umlaut (2-byte UTF-8)", LogTruncator.truncate(s, LOG_HEAD_BYTES, LOG_TAIL_BYTES));
    }

    @Test
    void cjkOverLimitIsTruncatedToByteBudget() {
        // '字' is 3 bytes in UTF-8.
        String s = repeat("字", MAX_BYTES);
        assertWithinByteBudget("CJK (3-byte UTF-8)", LogTruncator.truncate(s, LOG_HEAD_BYTES, LOG_TAIL_BYTES));
    }

    @Test
    void emojiOverLimitIsTruncatedToByteBudget() {
        // '😀' (U+1F600) is 4 bytes in UTF-8 and 2 Java chars (surrogate pair).
        // A byte-based truncator must respect the byte budget AND never cut
        // between high and low surrogate.
        String s = repeat("😀", MAX_BYTES);
        String result = LogTruncator.truncate(s, LOG_HEAD_BYTES, LOG_TAIL_BYTES);
        assertWithinByteBudget("Emoji (4-byte UTF-8)", result);
        assertValidUtf8RoundTrip(result);
    }

    @Test
    void truncationPreservesHeadAndTailContent() {
        String head = "BEGIN_OF_LOG";
        String tail = "END_OF_LOG";
        String middle = repeat("x", MAX_BYTES * 3);
        String result = LogTruncator.truncate(head + middle + tail, LOG_HEAD_BYTES, LOG_TAIL_BYTES);

        assertTrue(result.startsWith(head), "Head marker must survive truncation");
        assertTrue(result.endsWith(tail), "Tail marker must survive truncation");
    }

    private static void assertWithinByteBudget(String label, String result) {
        int byteSize = result.getBytes(StandardCharsets.UTF_8).length;
        int budget = MAX_BYTES + MARKER_OVERHEAD_TOLERANCE_BYTES;
        assertTrue(byteSize <= budget,
                label + ": expected truncated UTF-8 size <= " + budget + " B but got " + byteSize + " B");
    }

    private static void assertValidUtf8RoundTrip(String s) {
        byte[] encoded = s.getBytes(StandardCharsets.UTF_8);
        String decoded = new String(encoded, StandardCharsets.UTF_8);
        assertFalse(decoded.contains("�"),
                "Truncated string must not contain U+FFFD (broken UTF-8 sequence)");
        assertEquals(s, decoded, "Truncated string must round-trip through UTF-8 unchanged");
    }

    private static String repeat(String unit, int times) {
        StringBuilder sb = new StringBuilder(unit.length() * times);
        for (int i = 0; i < times; i++) {
            sb.append(unit);
        }
        return sb.toString();
    }

    // Number of log fields per response type. Mirrors the production constants.
    private static final int BACKUP_LOG_FIELDS = 10;
    private static final int RESTORE_LOG_FIELDS = 8;

    @Test
    void enforceDocumentBudget_emptyMap_isNoOp() {
        assertFalse(LogTruncator.enforceDocumentBudget(new HashMap<>()));
    }

    @Test
    void enforceDocumentBudget_underBudget_isNoOp() {
        // 5 items × ~1.28 MB ≈ 6.4 MB << 12 MB
        Map<String, AgentExecutionResponse> responses = backupResponses(5);
        long before = totalUtf8Size(responses);

        boolean retruncated = LogTruncator.enforceDocumentBudget(responses);

        assertFalse(retruncated, "should not re-truncate when already within budget");
        assertEquals(before, totalUtf8Size(responses), "size must remain unchanged");
    }

    @Test
    void enforceDocumentBudget_overBudget_keepsTotalWithinBudget() {
        // 15 items × ~1.28 MB ≈ 19.2 MB → exceeds 12 MB
        Map<String, AgentExecutionResponse> responses = backupResponses(15);

        boolean retruncated = LogTruncator.enforceDocumentBudget(responses);

        assertTrue(retruncated, "should re-truncate when over budget");
        assertWithinDocBudget(responses);
    }

    @Test
    void enforceDocumentBudget_manyItems_keepsTotalWithinBudget() {
        // 100 items would naively be ~128 MB — re-truncation must crunch it down.
        Map<String, AgentExecutionResponse> responses = backupResponses(100);

        assertTrue(LogTruncator.enforceDocumentBudget(responses));
        assertWithinDocBudget(responses);
    }

    @Test
    void enforceDocumentBudget_mixedBackupAndRestore_keepsTotalWithinBudget() {
        Map<String, AgentExecutionResponse> responses = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            responses.put("backup-" + i, maxSizedBackupResponse());
        }
        for (int i = 0; i < 10; i++) {
            responses.put("restore-" + i, maxSizedRestoreResponse());
        }

        assertTrue(LogTruncator.enforceDocumentBudget(responses));
        assertWithinDocBudget(responses);
    }

    @Test
    void enforceDocumentBudget_preservesAllEntries() {
        Map<String, AgentExecutionResponse> responses = backupResponses(20);

        LogTruncator.enforceDocumentBudget(responses);

        assertEquals(20, responses.size(), "no entries must be dropped");
        for (AgentExecutionResponse r : responses.values()) {
            AgentBackupResponse b = (AgentBackupResponse) r;
            assertTrue(b.getBackupLog() != null && !b.getBackupLog().isEmpty(),
                    "log content must be preserved (truncated, not nulled)");
        }
    }

    // Helpers

    private static Map<String, AgentExecutionResponse> backupResponses(int count) {
        Map<String, AgentExecutionResponse> map = new HashMap<>();
        for (int i = 0; i < count; i++) {
            map.put("item-" + i, maxSizedBackupResponse());
        }
        return map;
    }

    private static AgentBackupResponse maxSizedBackupResponse() {
        AgentBackupResponse r = new AgentBackupResponse();
        String content = preTruncatedLog();
        r.setPreBackupLockLog(content);
        r.setPreBackupLockErrorLog(content);
        r.setPreBackCheckLog(content);
        r.setPreBackCheckErrorLog(content);
        r.setBackupLog(content);
        r.setBackupErrorLog(content);
        r.setBackupCleanupLog(content);
        r.setBackupCleanupErrorLog(content);
        r.setPostBackupUnlockLog(content);
        r.setPostBackupUnlockErrorLog(content);
        return r;
    }

    private static AgentRestoreResponse maxSizedRestoreResponse() {
        AgentRestoreResponse r = new AgentRestoreResponse();
        String content = preTruncatedLog();
        r.setPreRestoreLockLog(content);
        r.setPreRestoreLockErrorLog(content);
        r.setRestoreLog(content);
        r.setRestoreErrorLog(content);
        r.setRestoreCleanupLog(content);
        r.setRestoreCleanupErrorLog(content);
        r.setPostRestoreUnlockLog(content);
        r.setPostRestoreUnlockErrorLog(content);
        return r;
    }

    // Shape after per-field truncation: head + marker + tail (~128 KB).
    private static String preTruncatedLog() {
        return repeat("a", LOG_HEAD_BYTES)
                + "\n... [truncated 1000000 bytes] ...\n"
                + repeat("z", LOG_TAIL_BYTES);
    }

    private static void assertWithinDocBudget(Map<String, AgentExecutionResponse> responses) {
        long total = totalUtf8Size(responses);
        int totalFields = countLogFields(responses);
        // Each re-truncated field carries the marker overhead; allow a per-field tolerance.
        long tolerance = (long) totalFields * MARKER_OVERHEAD_TOLERANCE_BYTES;
        long budget = LogTruncator.MAX_DOC_BYTES + tolerance;
        assertTrue(total <= budget,
                "expected total UTF-8 size <= " + budget + " B but got " + total + " B");
    }

    private static long totalUtf8Size(Map<String, AgentExecutionResponse> responses) {
        long total = 0;
        for (AgentExecutionResponse r : responses.values()) {
            if (r instanceof AgentBackupResponse) {
                AgentBackupResponse b = (AgentBackupResponse) r;
                total += utf8Len(b.getPreBackupLockLog());
                total += utf8Len(b.getPreBackupLockErrorLog());
                total += utf8Len(b.getPreBackCheckLog());
                total += utf8Len(b.getPreBackCheckErrorLog());
                total += utf8Len(b.getBackupLog());
                total += utf8Len(b.getBackupErrorLog());
                total += utf8Len(b.getBackupCleanupLog());
                total += utf8Len(b.getBackupCleanupErrorLog());
                total += utf8Len(b.getPostBackupUnlockLog());
                total += utf8Len(b.getPostBackupUnlockErrorLog());
            } else if (r instanceof AgentRestoreResponse) {
                AgentRestoreResponse rs = (AgentRestoreResponse) r;
                total += utf8Len(rs.getPreRestoreLockLog());
                total += utf8Len(rs.getPreRestoreLockErrorLog());
                total += utf8Len(rs.getRestoreLog());
                total += utf8Len(rs.getRestoreErrorLog());
                total += utf8Len(rs.getRestoreCleanupLog());
                total += utf8Len(rs.getRestoreCleanupErrorLog());
                total += utf8Len(rs.getPostRestoreUnlockLog());
                total += utf8Len(rs.getPostRestoreUnlockErrorLog());
            }
        }
        return total;
    }

    private static long utf8Len(String s) {
        return s == null ? 0 : s.getBytes(StandardCharsets.UTF_8).length;
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
