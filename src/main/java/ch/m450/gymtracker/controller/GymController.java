package ch.m450.gymtracker.controller;

import ch.m450.gymtracker.model.Exercise;
import ch.m450.gymtracker.model.Member;
import ch.m450.gymtracker.model.Workout;
import ch.m450.gymtracker.repository.ExerciseRepository;
import ch.m450.gymtracker.repository.MemberRepository;
import ch.m450.gymtracker.repository.WorkoutRepository;
import ch.m450.gymtracker.service.TrainingCalculator;
import ch.m450.gymtracker.service.WorkoutService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST-Schnittstelle, damit man die Logik von aussen aufrufen kann
 * (Browser, Postman, Fachgespräch). Gibt DTOs zurück statt Entitäten,
 * damit es keine Endlosschleife Workout ↔ WorkoutSet beim JSON gibt.
 */
@RestController
public class GymController {

    private final WorkoutService workoutService;
    private final TrainingCalculator calculator;
    private final MemberRepository memberRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutRepository workoutRepository;

    public GymController(WorkoutService workoutService, TrainingCalculator calculator,
                         MemberRepository memberRepository, ExerciseRepository exerciseRepository,
                         WorkoutRepository workoutRepository) {
        this.workoutService = workoutService;
        this.calculator = calculator;
        this.memberRepository = memberRepository;
        this.exerciseRepository = exerciseRepository;
        this.workoutRepository = workoutRepository;
    }

    // ---------- Lesen ----------

    @GetMapping("/members")
    public List<Member> members() {
        return memberRepository.findAll();
    }

    @GetMapping("/exercises")
    public List<Exercise> exercises() {
        return exerciseRepository.findAll();
    }

    @GetMapping("/members/{memberId}/workouts")
    public List<WorkoutDto> workouts(@PathVariable Long memberId) {
        return workoutRepository.findByMemberId(memberId).stream().map(WorkoutDto::from).toList();
    }

    @GetMapping("/members/{memberId}/volume")
    public Map<String, Object> totalVolume(@PathVariable Long memberId) {
        return Map.of("memberId", memberId, "totalVolumeKg", workoutService.totalVolume(memberId));
    }

    // ---------- Schreiben ----------

    public record StartWorkoutRequest(Long memberId, LocalDate date) {}

    @PostMapping("/workouts")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutDto startWorkout(@RequestBody StartWorkoutRequest request) {
        return WorkoutDto.from(workoutService.startWorkout(request.memberId(), request.date()));
    }

    public record AddSetRequest(Long exerciseId, int reps, double weight) {}

    @PostMapping("/workouts/{workoutId}/sets")
    public WorkoutDto addSet(@PathVariable Long workoutId, @RequestBody AddSetRequest request) {
        return WorkoutDto.from(workoutService.addSet(workoutId, request.exerciseId(), request.reps(), request.weight()));
    }

    @DeleteMapping("/workouts/{workoutId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkout(@PathVariable Long workoutId) {
        workoutService.deleteWorkout(workoutId);
    }

    // ---------- Rechner ----------

    @GetMapping("/calculator/one-rep-max")
    public Map<String, Object> oneRepMax(@RequestParam int reps, @RequestParam double weight) {
        return Map.of("reps", reps, "weight", weight, "oneRepMax", calculator.oneRepMax(reps, weight));
    }

    @GetMapping("/calculator/strength-level")
    public Map<String, Object> strengthLevel(@RequestParam double oneRepMax, @RequestParam double bodyWeight) {
        return Map.of("level", calculator.strengthLevel(oneRepMax, bodyWeight));
    }

    // ---------- DTOs ----------

    public record SetDto(String exercise, int reps, double weight) {}

    public record WorkoutDto(Long id, LocalDate date, String member, List<SetDto> sets) {
        static WorkoutDto from(Workout w) {
            return new WorkoutDto(w.getId(), w.getDate(), w.getMember().getName(),
                    w.getSets().stream()
                            .map(s -> new SetDto(s.getExercise().getName(), s.getReps(), s.getWeight()))
                            .toList());
        }
    }
}
