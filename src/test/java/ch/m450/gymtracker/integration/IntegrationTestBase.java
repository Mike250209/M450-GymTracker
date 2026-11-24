package ch.m450.gymtracker.integration;

import ch.m450.gymtracker.model.Exercise;
import ch.m450.gymtracker.model.Member;
import ch.m450.gymtracker.repository.ExerciseRepository;
import ch.m450.gymtracker.repository.MemberRepository;
import ch.m450.gymtracker.repository.WorkoutRepository;
import ch.m450.gymtracker.service.WorkoutService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Basis für alle Integrationstests (LB2).
 * Startet EINE echte PostgreSQL-Datenbank in einem Docker-Container (Testcontainers)
 * als Singleton, der über alle Testklassen geteilt und erst beim Beenden der JVM
 * abgeräumt wird. So wird gegen eine echte DB getestet, nicht gegen Mocks oder eine
 * In-Memory-DB.
 *
 * @author Julian Schiller
 */
@SpringBootTest
public abstract class IntegrationTestBase {

    /**
     * Lokal (ohne vorgegebene Datenbank) wird PostgreSQL über Testcontainers gestartet.
     * In der CI steht über {@code SPRING_DATASOURCE_URL} bereits ein PostgreSQL-Service-
     * Container bereit; dann wird dieser genutzt und kein eigener Container gestartet.
     */
    static final boolean USE_TESTCONTAINERS = System.getenv("SPRING_DATASOURCE_URL") == null;

    static final PostgreSQLContainer<?> POSTGRES =
            USE_TESTCONTAINERS ? new PostgreSQLContainer<>("postgres:16-alpine") : null;

    static {
        if (USE_TESTCONTAINERS) {
            POSTGRES.start();
        }
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        if (USE_TESTCONTAINERS) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        }
        // sonst: Spring liest spring.datasource.* aus den Umgebungsvariablen (CI-Service)
    }

    @Autowired
    protected WorkoutService workoutService;
    @Autowired
    protected WorkoutRepository workoutRepository;
    @Autowired
    protected MemberRepository memberRepository;
    @Autowired
    protected ExerciseRepository exerciseRepository;

    @PersistenceContext
    protected EntityManager em;

    /** Vor jedem Test wird die DB geleert, damit jeder Test auf einem definierten Zustand startet. */
    @BeforeEach
    void resetDatabase() {
        workoutRepository.deleteAll();
        exerciseRepository.deleteAll();
        memberRepository.deleteAll();
    }

    protected Member newMember(String name, double bodyWeight) {
        return memberRepository.save(new Member(name, bodyWeight));
    }

    protected Exercise newExercise(String name, String muscleGroup) {
        return exerciseRepository.save(new Exercise(name, muscleGroup));
    }

    protected long countSets() {
        return em.createQuery("select count(s) from WorkoutSet s", Long.class).getSingleResult();
    }
}
