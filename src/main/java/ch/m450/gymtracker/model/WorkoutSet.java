package ch.m450.gymtracker.model;

import jakarta.persistence.*;

@Entity
public class WorkoutSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Workout workout;

    @ManyToOne(optional = false)
    private Exercise exercise;

    private int reps;

    /** Gewicht in kg */
    private double weight;

    protected WorkoutSet() {
    }

    public WorkoutSet(Exercise exercise, int reps, double weight) {
        this.exercise = exercise;
        this.reps = reps;
        this.weight = weight;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Workout getWorkout() { return workout; }
    public void setWorkout(Workout workout) { this.workout = workout; }
    public Exercise getExercise() { return exercise; }
    public void setExercise(Exercise exercise) { this.exercise = exercise; }
    public int getReps() { return reps; }
    public void setReps(int reps) { this.reps = reps; }
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
}
