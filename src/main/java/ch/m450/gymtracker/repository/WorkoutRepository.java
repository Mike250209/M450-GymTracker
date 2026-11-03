package ch.m450.gymtracker.repository;

import ch.m450.gymtracker.model.Workout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface WorkoutRepository extends JpaRepository<Workout, Long> {

    List<Workout> findByMemberId(Long memberId);

    boolean existsByMemberIdAndDate(Long memberId, LocalDate date);
}
