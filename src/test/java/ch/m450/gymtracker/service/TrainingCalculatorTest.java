package ch.m450.gymtracker.service;

import ch.m450.gymtracker.model.WorkoutSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TrainingCalculator (Logik)")
class TrainingCalculatorTest {

    private final TrainingCalculator calculator = new TrainingCalculator();

    private WorkoutSet set(int reps, double weight) {
        return new WorkoutSet(null, reps, weight);
    }

    @Nested
    @DisplayName("validateSet")
    class ValidateSet {

        @Test
        @DisplayName("akzeptiert gültige Randwerte für Wiederholungen (1 und 100)")
        void acceptsRepBoundaries() {
            calculator.validateSet(1, 50);
            calculator.validateSet(TrainingCalculator.MAX_REPS, 50);
        }

        @Test
        @DisplayName("akzeptiert gültige Randwerte für Gewicht (0 und 500)")
        void acceptsWeightBoundaries() {
            calculator.validateSet(10, 0);
            calculator.validateSet(10, TrainingCalculator.MAX_WEIGHT);
        }

        @Test
        @DisplayName("wirft bei 0 Wiederholungen")
        void rejectsZeroReps() {
            assertThatThrownBy(() -> calculator.validateSet(0, 50))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Wiederholungen");
        }

        @Test
        @DisplayName("wirft bei mehr als 100 Wiederholungen")
        void rejectsTooManyReps() {
            assertThatThrownBy(() -> calculator.validateSet(TrainingCalculator.MAX_REPS + 1, 50))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Wiederholungen");
        }

        @Test
        @DisplayName("wirft bei negativem Gewicht")
        void rejectsNegativeWeight() {
            assertThatThrownBy(() -> calculator.validateSet(10, -0.01))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Gewicht");
        }

        @Test
        @DisplayName("wirft bei Gewicht über 500 kg")
        void rejectsTooHeavyWeight() {
            assertThatThrownBy(() -> calculator.validateSet(10, TrainingCalculator.MAX_WEIGHT + 0.01))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Gewicht");
        }
    }

    @Nested
    @DisplayName("oneRepMax")
    class OneRepMax {

        @Test
        @DisplayName("gibt bei genau 1 Wiederholung das Gewicht selbst zurück")
        void returnsWeightForSingleRep() {
            assertThat(calculator.oneRepMax(1, 120)).isEqualTo(120.0);
        }

        @Test
        @DisplayName("berechnet 1RM nach Epley und rundet auf 2 Stellen")
        void computesEpleyFormula() {
            assertThat(calculator.oneRepMax(5, 100)).isEqualTo(116.67);
        }

        @Test
        @DisplayName("berechnet 1RM auch für glatte Werte korrekt")
        void computesExactFormula() {
            assertThat(calculator.oneRepMax(3, 100)).isEqualTo(110.0);
        }

        @Test
        @DisplayName("validiert die Eingabe (ungültige Wiederholungen werfen)")
        void validatesInput() {
            assertThatThrownBy(() -> calculator.oneRepMax(0, 100))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("volume")
    class Volume {

        @Test
        @DisplayName("gibt 0 zurück bei null")
        void returnsZeroForNull() {
            assertThat(calculator.volume(null)).isZero();
        }

        @Test
        @DisplayName("gibt 0 zurück bei leerer Liste")
        void returnsZeroForEmpty() {
            assertThat(calculator.volume(Collections.emptyList())).isZero();
        }

        @Test
        @DisplayName("summiert Wiederholungen × Gewicht über alle Sätze")
        void sumsRepsTimesWeight() {
            List<WorkoutSet> sets = List.of(set(10, 50), set(5, 20));
            assertThat(calculator.volume(sets)).isEqualTo(600.0);
        }
    }

    @Nested
    @DisplayName("strengthLevel")
    class StrengthLevel {

        @Test
        @DisplayName("wirft bei Körpergewicht 0")
        void rejectsZeroBodyWeight() {
            assertThatThrownBy(() -> calculator.strengthLevel(100, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Körpergewicht");
        }

        @Test
        @DisplayName("wirft bei negativem Körpergewicht")
        void rejectsNegativeBodyWeight() {
            assertThatThrownBy(() -> calculator.strengthLevel(100, -70))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("stuft Verhältnis < 1.0 als ANFAENGER ein")
        void classifiesBeginner() {
            assertThat(calculator.strengthLevel(50, 100)).isEqualTo("ANFAENGER");
        }

        @Test
        @DisplayName("stuft Verhältnis genau 1.0 als FORTGESCHRITTEN ein (untere Grenze)")
        void classifiesAdvancedAtLowerBound() {
            assertThat(calculator.strengthLevel(100, 100)).isEqualTo("FORTGESCHRITTEN");
        }

        @Test
        @DisplayName("stuft Verhältnis knapp unter 1.5 als FORTGESCHRITTEN ein")
        void classifiesAdvancedBelowUpperBound() {
            assertThat(calculator.strengthLevel(149, 100)).isEqualTo("FORTGESCHRITTEN");
        }

        @Test
        @DisplayName("stuft Verhältnis genau 1.5 als PROFI ein (obere Grenze)")
        void classifiesProAtUpperBound() {
            assertThat(calculator.strengthLevel(150, 100)).isEqualTo("PROFI");
        }
    }
}
