package de.evoila.cf.backup.service.manager;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

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
        assertNull(LogTruncator.truncate(null));
    }

    @Test
    void returnsEmptyForEmptyInput() {
        assertEquals("", LogTruncator.truncate(""));
    }

    @Test
    void asciiUnderLimitIsUnchanged() {
        String s = "ein kurzer log eintrag";
        assertEquals(s, LogTruncator.truncate(s));
    }

    @Test
    void asciiOverLimitIsTruncatedToByteBudget() {
        // 'a' is 1 byte in UTF-8 → char count equals byte count.
        String s = repeat("a", MAX_BYTES * 2);
        assertWithinByteBudget("ASCII", LogTruncator.truncate(s));
    }

    @Test
    void umlautOverLimitIsTruncatedToByteBudget() {
        // 'Ä' is 2 bytes in UTF-8. MAX_BYTES copies produce MAX_BYTES chars but
        // 2 * MAX_BYTES bytes — a char-based truncator returns this unchanged,
        // a byte-based one must truncate.
        String s = repeat("Ä", MAX_BYTES);
        assertWithinByteBudget("Umlaut (2-byte UTF-8)", LogTruncator.truncate(s));
    }

    @Test
    void cjkOverLimitIsTruncatedToByteBudget() {
        // '字' is 3 bytes in UTF-8.
        String s = repeat("字", MAX_BYTES);
        assertWithinByteBudget("CJK (3-byte UTF-8)", LogTruncator.truncate(s));
    }

    @Test
    void emojiOverLimitIsTruncatedToByteBudget() {
        // '😀' (U+1F600) is 4 bytes in UTF-8 and 2 Java chars (surrogate pair).
        // A byte-based truncator must respect the byte budget AND never cut
        // between high and low surrogate.
        String s = repeat("😀", MAX_BYTES);
        String result = LogTruncator.truncate(s);
        assertWithinByteBudget("Emoji (4-byte UTF-8)", result);
        assertValidUtf8RoundTrip(result);
    }

    @Test
    void truncationPreservesHeadAndTailContent() {
        String head = "BEGIN_OF_LOG";
        String tail = "END_OF_LOG";
        String middle = repeat("x", MAX_BYTES * 3);
        String result = LogTruncator.truncate(head + middle + tail);

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
}
