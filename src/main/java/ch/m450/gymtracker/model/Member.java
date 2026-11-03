package ch.m450.gymtracker.model;

import jakarta.persistence.*;

@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    /** Körpergewicht in kg */
    private double bodyWeight;

    protected Member() {
    }

    public Member(String name, double bodyWeight) {
        this.name = name;
        this.bodyWeight = bodyWeight;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getBodyWeight() { return bodyWeight; }
    public void setBodyWeight(double bodyWeight) { this.bodyWeight = bodyWeight; }
}
