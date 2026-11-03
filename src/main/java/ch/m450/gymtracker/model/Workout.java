package ch.m450.gymtracker.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Workout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;

    @ManyToOne(optional = false)
    private Member member;

    @OneToMany(mappedBy = "workout", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkoutSet> sets = new ArrayList<>();

    protected Workout() {
    }

    public Workout(Member member, LocalDate date) {
        this.member = member;
        this.date = date;
    }

    public void addSet(WorkoutSet set) {
        set.setWorkout(this);
        sets.add(set);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public List<WorkoutSet> getSets() { return sets; }
}
