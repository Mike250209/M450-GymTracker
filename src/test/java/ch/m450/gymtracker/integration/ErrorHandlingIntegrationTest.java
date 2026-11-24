package ch.m450.gymtracker.integration;

import ch.m450.gymtracker.model.Exercise;
import ch.m450.gymtracker.model.Member;
import ch.m450.gymtracker.model.Workout;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Schwerpunkt 3: Fehlerbehandlung.
 * Prüft, wie der Service auf fehlerhafte Anfragen gegen die echte DB reagiert:
 * unbekannte Datensätze und Verletzungen der Geschäftsregeln führen zu klaren,
 * definierten Exceptions statt zu Abstürzen oder inkonsistenten Daten.
 *
 * @author Julian Schiller
 */
@Transactional
@DisplayName("Schwerpunkt 3 – Fehlerbehandlung (Integration)")
class ErrorHandlingIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("T-301 Unbekanntes Mitglied: startWorkout wirft NoSuchElementException")
    void startWorkoutWithUnknownMember() {
        assertThatThrownBy(() -> workoutService.startWorkout(999_999L, LocalDate.now()))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Mitglied");
    }

    @Test
    @DisplayName("T-302 Unbekanntes Workout: addSet wirft NoSuchElementException")
    void addSetToUnknownWorkout() {
        Exercise exercise = newExercise("Bankdrücken", "BRUST");

        assertThatThrownBy(() -> workoutService.addSet(999_999L, exercise.getId(), 8, 60))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Workout");
    }

    @Test
    @DisplayName("T-303 Unbekannte Übung: addSet wirft NoSuchElementException")
    void addSetWithUnknownExercise() {
        Member member = newMember("Julian", 80);
        Workout workout = workoutService.startWorkout(member.getId(), LocalDate.now());

        assertThatThrownBy(() -> workoutService.addSet(workout.getId(), 999_999L, 8, 60))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Übung");
    }

    @Test
    @DisplayName("T-304 Doppeltes Workout: zweites Workout am selben Tag wirft IllegalStateException")
    void duplicateWorkoutPerDayIsRejected() {
        Member member = newMember("Julian", 80);
        LocalDate today = LocalDate.now();
        workoutService.startWorkout(member.getId(), today);
        em.flush();

        assertThatThrownBy(() -> workoutService.startWorkout(member.getId(), today))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bereits");
    }

    @Test
    @DisplayName("T-305 Unbekanntes Workout löschen: deleteWorkout wirft NoSuchElementException")
    void deleteUnknownWorkout() {
        assertThatThrownBy(() -> workoutService.deleteWorkout(999_999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Workout");
    }
}
