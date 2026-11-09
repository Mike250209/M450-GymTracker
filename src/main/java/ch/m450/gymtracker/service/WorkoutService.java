package ch.m450.gymtracker.service;

import ch.m450.gymtracker.model.Exercise;
import ch.m450.gymtracker.model.Member;
import ch.m450.gymtracker.model.Workout;
import ch.m450.gymtracker.model.WorkoutSet;
import ch.m450.gymtracker.repository.ExerciseRepository;
import ch.m450.gymtracker.repository.MemberRepository;
import ch.m450.gymtracker.repository.WorkoutRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Service mit Abhängigkeiten zu Repositories und zum Calculator.
 * → Baustein 1: Unit-Test mit Mocking
 */
@Service
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final MemberRepository memberRepository;
    private final ExerciseRepository exerciseRepository;
    private final TrainingCalculator calculator;

    public WorkoutService(WorkoutRepository workoutRepository,
                          MemberRepository memberRepository,
                          ExerciseRepository exerciseRepository,
                          TrainingCalculator calculator) {
        this.workoutRepository = workoutRepository;
        this.memberRepository = memberRepository;
        this.exerciseRepository = exerciseRepository;
        this.calculator = calculator;
    }

    /** Legt ein neues Workout an. Pro Mitglied und Tag ist nur ein Workout erlaubt. */
    public Workout startWorkout(Long memberId, LocalDate date) {
        if (date == null || date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Datum darf nicht leer oder in der Zukunft sein");
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("Mitglied nicht gefunden: " + memberId));
        if (workoutRepository.existsByMemberIdAndDate(memberId, date)) {
            throw new IllegalStateException("Für dieses Datum existiert bereits ein Workout");
        }
        return workoutRepository.save(new Workout(member, date));
    }

    /** Fügt einem Workout einen Satz hinzu (wird vorher validiert). */
    public Workout addSet(Long workoutId, Long exerciseId, int reps, double weight) {
        calculator.validateSet(reps, weight);
        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new NoSuchElementException("Workout nicht gefunden: " + workoutId));
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new NoSuchElementException("Übung nicht gefunden: " + exerciseId));
        workout.addSet(new WorkoutSet(exercise, reps, weight));
        return workoutRepository.save(workout);
    }

    /** Gesamtvolumen aller Workouts eines Mitglieds. */
    public double totalVolume(Long memberId) {
        List<Workout> workouts = workoutRepository.findByMemberId(memberId);
        double total = 0;
        for (Workout workout : workouts) {
            total += calculator.volume(workout.getSets());
        }
        return total;
    }

    /**
     * Durchschnittliches Trainingsvolumen pro Workout eines Mitglieds.
     * Statistischer Wert, berechnet aus den Workout-Daten der Datenbank.
     * → Baustein 4: TDD (Red → Green → Refactor)
     */
    public double averageVolumePerWorkout(Long memberId) {
        List<Workout> workouts = workoutRepository.findByMemberId(memberId);
        if (workouts.isEmpty()) {
            return 0;
        }
        double total = 0;
        for (Workout workout : workouts) {
            total += calculator.volume(workout.getSets());
        }
        return BigDecimal.valueOf(total)
                .divide(BigDecimal.valueOf(workouts.size()), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /** Löscht ein Workout, falls vorhanden. */
    public void deleteWorkout(Long workoutId) {
        if (!workoutRepository.existsById(workoutId)) {
            throw new NoSuchElementException("Workout nicht gefunden: " + workoutId);
        }
        workoutRepository.deleteById(workoutId);
    }
}
