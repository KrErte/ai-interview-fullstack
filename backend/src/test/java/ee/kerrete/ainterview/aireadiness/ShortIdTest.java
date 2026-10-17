package ee.kerrete.ainterview.aireadiness;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** FP-60d: the echoed id is cut by code point, never inside a surrogate pair. */
class ShortIdTest {

    private static final String EMOJI = "\uD83D\uDE00"; // one code point, two UTF-16 units

    @Test
    void shortIdsAreUnchanged() {
        assertThat(AiReadinessService.shortId("translator")).isEqualTo("translator");
        assertThat(AiReadinessService.shortId(EMOJI.repeat(64))).isEqualTo(EMOJI.repeat(64));
    }

    @Test
    void longIdsKeep64CodePointsAndNeverSplitASurrogatePair() {
        String cut = AiReadinessService.shortId("a" + EMOJI.repeat(100));
        assertThat(cut).isEqualTo("a" + EMOJI.repeat(63) + "…");
        assertThat(cut.codePointCount(0, cut.length())).isEqualTo(65);
        assertThat(cut.chars().filter(ch -> Character.isSurrogate((char) ch)).count() % 2).isZero();
        assertThat(Character.isHighSurrogate(cut.charAt(cut.length() - 2))).isFalse();
        assertThat(AiReadinessService.shortId("y".repeat(500))).isEqualTo("y".repeat(64) + "…");
    }
}
