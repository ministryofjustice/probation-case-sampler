package uk.gov.justice.hmiprobation.casesampler.utils

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class CounterTest {

    @Test
    fun `non existent key size is 0`() {

        val counter = Counter()

        assertThat(counter.size("Non existent key")).isZero();
    }

    @Test
    fun `initialise key with inc`() {

        val counter = Counter()

        counter.inc("key")
        assertThat(counter.size("key")).isOne();
    }

    @Test
    fun `can track multiple keys`() {

        val counter = Counter()

        counter.inc("key1")
        counter.inc("key1")
        counter.inc("key2")

        assertThat(counter.size("key1")).isEqualTo(2);
        assertThat(counter.size("key2")).isOne();
        assertThat(counter.size("key3")).isZero();
    }

    @Test
    fun `can add to non existent keys`() {

        val counter = Counter()

        counter.add("key", 5)

        assertThat(counter.size("key")).isEqualTo(5);
    }

    @Test
    fun `can add to an existing keys`() {

        val counter = Counter()

        counter.inc("key")
        counter.add("key", 5)

        assertThat(counter.size("key")).isEqualTo(6)
    }

    @Test
    fun `limit works with inc`() {

        val counter = Counter(6)

        counter.add("key", 6)

        assertThatThrownBy { counter.inc("key") }.hasMessage("key will be larger than 6")
    }

    @Test
    fun `limit works with add`() {

        val counter = Counter(5)

        counter.inc("key")

        assertThatThrownBy { counter.add("key", 5) }.hasMessage("key will be larger than 5")
        assertThat(counter.size("key")).isEqualTo(1)
    }

    @Test
    fun `limit doesn't apply across keys`() {

        val counter = Counter(5)

        counter.add("key1", 5)
        counter.add("key2", 5)

        assertThat(counter.size("key1")).isEqualTo(5)
        assertThat(counter.size("key2")).isEqualTo(5)
    }
}