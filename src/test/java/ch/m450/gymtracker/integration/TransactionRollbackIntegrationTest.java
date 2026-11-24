package ch.m450.gymtracker.integration;

import ch.m450.gymtracker.model.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Schwerpunkt 1: Transaktionsprüfungen / CRUD – Atomicity (ACID).
 * Prüft, dass eine Transaktion, die mit einem Fehler abbricht, keine Teildaten
 * hinterlässt. Dieser Test läuft bewusst ohne {@code @Transactional}, damit ein
 * echter Commit/Rollback gegen die DB stattfindet.
 *
 * @author Christian Mitev
 */
@DisplayName("Schwerpunkt 1 – Transaktion Atomicity (Integration)")
class TransactionRollbackIntegrationTest extends IntegrationTestBase {

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("T-105 ATOMICITY: Ein Fehler in der Transaktion rollt alle Schreibvorgänge zurück")
    void failedTransactionRollsBackCompletely() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        long before = memberRepository.count();

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            memberRepository.save(new Member("Ghost", 70));
            throw new IllegalStateException("Fehler mitten in der Transaktion");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(memberRepository.count()).isEqualTo(before);
        assertThat(memberRepository.findAll()).noneMatch(m -> "Ghost".equals(m.getName()));
    }
}
