package ch.m450.gymtracker.repository;

import ch.m450.gymtracker.model.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
}
