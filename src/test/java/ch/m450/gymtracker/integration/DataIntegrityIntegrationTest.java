package ch.m450.gymtracker.integration;

import ch.m450.gymtracker.model.Exercise;
import ch.m450.gymtracker.model.Member;
import ch.m450.gymtracker.model.Workout;
import ch.m450.gymtracker.model.WorkoutSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Schwerpunkt 2: Datenintegrität.
 * Prüft, dass die aus den Entitäten generierten DB-Constraints (Unique, NOT NULL,
 * Fremdschlüssel, Cascade) tatsächlich in der PostgreSQL greifen und dass Werte
 * verlustfrei gespeichert und wieder gelesen werden.
 *
 * @author Mike Mamie
 */
@Transactional
@DisplayName("Schwerpunkt 2 – Datenintegrität (Integration)")
class DataIntegrityIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("T-201 UNIQUE: Zwei Übungen mit gleichem Namen werden abgewiesen")
    void uniqueExerciseNameIsEnforced() {
        exerciseRepository.saveAndFlush(new Exercise("Bankdrücken", "BRUST"));

        assertThatThrownBy(() ->
                exerciseRepository.saveAndFlush(new Exercise("Bankdrücken", "BRUST")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("T-202 NOT NULL: Ein Workout ohne Mitglied wird abgewiesen")
    void workoutRequiresMember() {
        Workout workout = new Workout(null, LocalDate.now());

        assertThatThrownBy(() -> workoutRepository.saveAndFlush(workout))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("T-203 NOT NULL/FK: Ein Satz ohne Übung wird abgewiesen")
    void setRequiresExercise() {
        Member member = newMember("Julian", 80);
        Workout workout = workoutRepository.save(new Workout(member, LocalDate.now()));
        workout.addSet(new WorkoutSet(null, 8, 60));

        assertThatThrownBy(() -> workoutRepository.saveAndFlush(workout))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("T-204 ROUND-TRIP: Gespeicherte Werte werden unverändert wieder gelesen")
    void valuesAreStoredAndReadBackExactly() {
        Member member = newMember("Julian", 80);
        Exercise exercise = newExercise("Bankdrücken", "BRUST");
        Workout workout = workoutService.startWorkout(member.getId(), LocalDate.now());
        workoutService.addSet(workout.getId(), exercise.getId(), 8, 62.5);
        em.flush();
        em.clear();

        Workout reloaded = workoutRepository.findById(workout.getId()).orElseThrow();
        WorkoutSet set = reloaded.getSets().get(0);
        assertThat(set.getReps()).isEqualTo(8);
        assertThat(set.getWeight()).isEqualTo(62.5);
        assertThat(reloaded.getMember().getBodyWeight()).isEqualTo(80.0);
    }

    @Test
    @DisplayName("T-205 CASCADE: Löschen eines Workouts hinterlässt keine verwaisten Sätze")
    void deletingWorkoutLeavesNoOrphanSets() {
        Member member = newMember("Julian", 80);
        Exercise exercise = newExercise("Kniebeuge", "BEINE");
        Workout workout = workoutService.startWorkout(member.getId(), LocalDate.now());
        workoutService.addSet(workout.getId(), exercise.getId(), 10, 80);
        workoutService.addSet(workout.getId(), exercise.getId(), 8, 85);
        em.flush();
        assertThat(countSets()).isEqualTo(2);

        workoutRepository.deleteById(workout.getId());
        em.flush();

        assertThat(countSets()).isZero();
        assertThat(exerciseRepository.findById(exercise.getId())).isPresent();
    }
}
