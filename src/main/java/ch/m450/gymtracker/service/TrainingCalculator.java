package ch.m450.gymtracker.service;

import ch.m450.gymtracker.model.WorkoutSet;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reine Berechnungslogik ohne Abhängigkeiten.
 * → Baustein 2: Unit-Test für Logik
 */
@Component
public class TrainingCalculator {

    public static final int MAX_REPS = 100;
    public static final double MAX_WEIGHT = 500.0;

    /**
     * Prüft, ob ein Satz gültig ist.
     * Gültig: 1–100 Wiederholungen, Gewicht 0–500 kg (0 = Körpergewicht-Übung).
     */
    public void validateSet(int reps, double weight) {
        if (reps < 1 || reps > MAX_REPS) {
            throw new IllegalArgumentException("Wiederholungen müssen zwischen 1 und " + MAX_REPS + " liegen");
        }
        if (weight < 0 || weight > MAX_WEIGHT) {
            throw new IllegalArgumentException("Gewicht muss zwischen 0 und " + MAX_WEIGHT + " kg liegen");
        }
    }

    /**
     * Geschätztes 1-Rep-Max nach Epley: weight * (1 + reps / 30).
     * Bei genau 1 Wiederholung ist das 1RM das Gewicht selbst.
     */
    public double oneRepMax(int reps, double weight) {
        validateSet(reps, weight);
        if (reps == 1) {
            return weight;
        }
        return round(weight * (1 + reps / 30.0));
    }

    /** Trainingsvolumen = Summe aus Wiederholungen × Gewicht aller Sätze. */
    public double volume(List<WorkoutSet> sets) {
        if (sets == null || sets.isEmpty()) {
            return 0;
        }
        double total = 0;
        for (WorkoutSet set : sets) {
            total += set.getReps() * set.getWeight();
        }
        return round(total);
    }

    /**
     * Relative Stärke = 1RM / Körpergewicht.
     * Einstufung: < 1.0 ANFAENGER, < 1.5 FORTGESCHRITTEN, sonst PROFI.
     */
    public String strengthLevel(double oneRepMax, double bodyWeight) {
        if (bodyWeight <= 0) {
            throw new IllegalArgumentException("Körpergewicht muss grösser als 0 sein");
        }
        double ratio = oneRepMax / bodyWeight;
        if (ratio < 1.0) {
            return "ANFAENGER";
        }
        if (ratio < 1.5) {
            return "FORTGESCHRITTEN";
        }
        return "PROFI";
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
