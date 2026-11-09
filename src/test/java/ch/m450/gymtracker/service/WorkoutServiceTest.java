package ch.m450.gymtracker.service;

import ch.m450.gymtracker.model.Exercise;
import ch.m450.gymtracker.model.Member;
import ch.m450.gymtracker.model.Workout;
import ch.m450.gymtracker.model.WorkoutSet;
import ch.m450.gymtracker.repository.ExerciseRepository;
import ch.m450.gymtracker.repository.MemberRepository;
import ch.m450.gymtracker.repository.WorkoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkoutService (Mocking)")
class WorkoutServiceTest {

    @Mock
    private WorkoutRepository workoutRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ExerciseRepository exerciseRepository;
    @Mock
    private TrainingCalculator calculator;

    @InjectMocks
    private WorkoutService service;

    private Member member;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        member = new Member("Julian", 80);
        member.setId(1L);
        exercise = new Exercise("Bankdrücken", "BRUST");
        exercise.setId(2L);
    }

    @Nested
    @DisplayName("startWorkout")
    class StartWorkout {

        @Test
        @DisplayName("legt ein Workout an, wenn alles gültig ist")
        void createsWorkout() {
            LocalDate today = LocalDate.now();
            Workout saved = new Workout(member, today);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            when(workoutRepository.existsByMemberIdAndDate(1L, today)).thenReturn(false);
            when(workoutRepository.save(any(Workout.class))).thenReturn(saved);

            Workout result = service.startWorkout(1L, today);

            assertThat(result).isSameAs(saved);
            verify(workoutRepository).save(any(Workout.class));
        }

        @Test
        @DisplayName("akzeptiert das heutige Datum (Grenzfall, nicht in der Zukunft)")
        void acceptsToday() {
            LocalDate today = LocalDate.now();
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            when(workoutRepository.existsByMemberIdAndDate(1L, today)).thenReturn(false);
            when(workoutRepository.save(any(Workout.class))).thenReturn(new Workout(member, today));

            assertThat(service.startWorkout(1L, today)).isNotNull();
        }

        @Test
        @DisplayName("wirft bei null-Datum und fasst die DB nicht an")
        void rejectsNullDate() {
            assertThatThrownBy(() -> service.startWorkout(1L, null))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(memberRepository, never()).findById(any());
        }

        @Test
        @DisplayName("wirft bei Datum in der Zukunft")
        void rejectsFutureDate() {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            assertThatThrownBy(() -> service.startWorkout(1L, tomorrow))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(memberRepository, never()).findById(any());
        }

        @Test
        @DisplayName("wirft, wenn das Mitglied nicht existiert")
        void rejectsUnknownMember() {
            LocalDate today = LocalDate.now();
            when(memberRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.startWorkout(1L, today))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("Mitglied");
            verify(workoutRepository, never()).save(any());
        }

        @Test
        @DisplayName("wirft, wenn für das Datum schon ein Workout existiert")
        void rejectsDuplicateWorkout() {
            LocalDate today = LocalDate.now();
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            when(workoutRepository.existsByMemberIdAndDate(1L, today)).thenReturn(true);

            assertThatThrownBy(() -> service.startWorkout(1L, today))
                    .isInstanceOf(IllegalStateException.class);
            verify(workoutRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("addSet")
    class AddSet {

        @Test
        @DisplayName("validiert, fügt den Satz hinzu und speichert")
        void addsSet() {
            Workout workout = new Workout(member, LocalDate.now());
            when(workoutRepository.findById(10L)).thenReturn(Optional.of(workout));
            when(exerciseRepository.findById(2L)).thenReturn(Optional.of(exercise));
            when(workoutRepository.save(workout)).thenReturn(workout);

            Workout result = service.addSet(10L, 2L, 8, 60);

            verify(calculator).validateSet(8, 60);
            assertThat(result).isSameAs(workout);
            assertThat(workout.getSets()).hasSize(1);
            WorkoutSet added = workout.getSets().get(0);
            assertThat(added.getReps()).isEqualTo(8);
            assertThat(added.getWeight()).isEqualTo(60.0);
            assertThat(added.getExercise()).isSameAs(exercise);
        }

        @Test
        @DisplayName("bricht ab, wenn die Validierung fehlschlägt (DB wird nicht angefasst)")
        void stopsOnInvalidSet() {
            doThrow(new IllegalArgumentException("ungültig"))
                    .when(calculator).validateSet(anyInt(), anyDouble());

            assertThatThrownBy(() -> service.addSet(10L, 2L, 0, 60))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(workoutRepository, never()).findById(any());
            verify(workoutRepository, never()).save(any());
        }

        @Test
        @DisplayName("wirft, wenn das Workout nicht existiert")
        void rejectsUnknownWorkout() {
            when(workoutRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addSet(10L, 2L, 8, 60))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("Workout");
            verify(workoutRepository, never()).save(any());
        }

        @Test
        @DisplayName("wirft, wenn die Übung nicht existiert")
        void rejectsUnknownExercise() {
            Workout workout = new Workout(member, LocalDate.now());
            when(workoutRepository.findById(10L)).thenReturn(Optional.of(workout));
            when(exerciseRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addSet(10L, 2L, 8, 60))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("Übung");
            verify(workoutRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("totalVolume")
    class TotalVolume {

        @Test
        @DisplayName("summiert das Volumen über alle Workouts des Mitglieds")
        void sumsVolumeOverWorkouts() {
            Workout w1 = new Workout(member, LocalDate.now());
            Workout w2 = new Workout(member, LocalDate.now());
            when(workoutRepository.findByMemberId(1L)).thenReturn(List.of(w1, w2));
            when(calculator.volume(any())).thenReturn(300.0, 150.0);

            assertThat(service.totalVolume(1L)).isEqualTo(450.0);
        }

        @Test
        @DisplayName("gibt 0 zurück, wenn das Mitglied keine Workouts hat")
        void returnsZeroWithoutWorkouts() {
            when(workoutRepository.findByMemberId(1L)).thenReturn(List.of());

            assertThat(service.totalVolume(1L)).isZero();
        }
    }

    @Nested
    @DisplayName("averageVolumePerWorkout (TDD – statistischer Wert aus DB-Daten)")
    class AverageVolumePerWorkout {

        @Test
        @DisplayName("berechnet das durchschnittliche Volumen pro Workout und rundet auf 2 Stellen")
        void computesRoundedAverage() {
            Workout w1 = new Workout(member, LocalDate.now());
            Workout w2 = new Workout(member, LocalDate.now());
            Workout w3 = new Workout(member, LocalDate.now());
            when(workoutRepository.findByMemberId(1L)).thenReturn(List.of(w1, w2, w3));
            when(calculator.volume(any())).thenReturn(40.0, 30.0, 30.0);

            assertThat(service.averageVolumePerWorkout(1L)).isEqualTo(33.33);
        }

        @Test
        @DisplayName("berechnet einen exakten Durchschnitt über zwei Workouts")
        void computesExactAverage() {
            Workout w1 = new Workout(member, LocalDate.now());
            Workout w2 = new Workout(member, LocalDate.now());
            when(workoutRepository.findByMemberId(1L)).thenReturn(List.of(w1, w2));
            when(calculator.volume(any())).thenReturn(300.0, 150.0);

            assertThat(service.averageVolumePerWorkout(1L)).isEqualTo(225.0);
        }

        @Test
        @DisplayName("gibt 0 zurück, wenn das Mitglied keine Workouts hat")
        void returnsZeroWithoutWorkouts() {
            when(workoutRepository.findByMemberId(1L)).thenReturn(List.of());

            assertThat(service.averageVolumePerWorkout(1L)).isZero();
        }
    }

    @Nested
    @DisplayName("deleteWorkout")
    class DeleteWorkout {

        @Test
        @DisplayName("löscht ein vorhandenes Workout")
        void deletesExisting() {
            when(workoutRepository.existsById(10L)).thenReturn(true);

            service.deleteWorkout(10L);

            verify(workoutRepository).deleteById(10L);
        }

        @Test
        @DisplayName("wirft und löscht nicht, wenn das Workout fehlt")
        void rejectsMissing() {
            when(workoutRepository.existsById(10L)).thenReturn(false);

            assertThatThrownBy(() -> service.deleteWorkout(10L))
                    .isInstanceOf(NoSuchElementException.class);
            verify(workoutRepository, never()).deleteById(eq(10L));
        }
    }
}
