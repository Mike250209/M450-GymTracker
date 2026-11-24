package ch.m450.gymtracker.integration;

import ch.m450.gymtracker.model.Exercise;
import ch.m450.gymtracker.model.Member;
import ch.m450.gymtracker.model.Workout;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Schwerpunkt 1: Transaktionsprüfungen / CRUD.
 * Prüft, dass Create-, Read-, Update- und Delete-Operationen über Service und
 * Repository tatsächlich in der echten PostgreSQL ankommen und das Cascade-Verhalten
 * stimmt. Jeder Test läuft in einer eigenen Transaktion, die am Ende zurückgerollt wird.
 *
 * @author Christian Mitev
 */
@Transactional
@DisplayName("Schwerpunkt 1 – CRUD / Transaktionen (Integration)")
class CrudIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("T-101 CREATE: startWorkout speichert ein Workout dauerhaft in der DB")
    void create_persistsWorkout() {
        Member member = newMember("Julian", 80);
        LocalDate today = LocalDate.now();

        Workout created = workoutService.startWorkout(member.getId(), today);
        em.flush();
        em.clear();

        assertThat(created.getId()).isNotNull();
        Optional<Workout> reloaded = workoutRepository.findById(created.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getDate()).isEqualTo(today);
        assertThat(reloaded.get().getMember().getId()).isEqualTo(member.getId());
    }

    @Test
    @DisplayName("T-102 READ: totalVolume liest mehrere Sätze korrekt aus der DB zusammen")
    void read_totalVolumeFromDb() {
        Member member = newMember("Julian", 80);
        Exercise exercise = newExercise("Bankdrücken", "BRUST");
        Workout workout = workoutService.startWorkout(member.getId(), LocalDate.now());
        workoutService.addSet(workout.getId(), exercise.getId(), 8, 60);
        workoutService.addSet(workout.getId(), exercise.getId(), 5, 20);
        em.flush();
        em.clear();

        double total = workoutService.totalVolume(member.getId());

        assertThat(total).isEqualTo(580.0);
    }

    @Test
    @DisplayName("T-103 UPDATE: addSet fügt einen Satz hinzu, der nach Reload persistiert ist")
    void update_addSetIsPersisted() {
        Member member = newMember("Julian", 80);
        Exercise exercise = newExercise("Kniebeuge", "BEINE");
        Workout workout = workoutService.startWorkout(member.getId(), LocalDate.now());

        workoutService.addSet(workout.getId(), exercise.getId(), 10, 80);
        em.flush();
        em.clear();

        Workout reloaded = workoutRepository.findById(workout.getId()).orElseThrow();
        assertThat(reloaded.getSets()).hasSize(1);
        assertThat(reloaded.getSets().get(0).getReps()).isEqualTo(10);
        assertThat(reloaded.getSets().get(0).getWeight()).isEqualTo(80.0);
        assertThat(reloaded.getSets().get(0).getExercise().getName()).isEqualTo("Kniebeuge");
    }

    @Test
    @DisplayName("T-104 DELETE: deleteWorkout entfernt das Workout samt seiner Sätze (Cascade)")
    void delete_cascadesToSets() {
        Member member = newMember("Julian", 80);
        Exercise exercise = newExercise("Kreuzheben", "RUECKEN");
        Workout workout = workoutService.startWorkout(member.getId(), LocalDate.now());
        workoutService.addSet(workout.getId(), exercise.getId(), 5, 100);
        em.flush();
        assertThat(countSets()).isEqualTo(1);

        workoutService.deleteWorkout(workout.getId());
        em.flush();
        em.clear();

        assertThat(workoutRepository.findById(workout.getId())).isEmpty();
        assertThat(countSets()).isZero();
    }
}
