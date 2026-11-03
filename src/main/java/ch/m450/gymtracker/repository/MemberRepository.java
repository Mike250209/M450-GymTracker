package ch.m450.gymtracker.repository;

import ch.m450.gymtracker.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
