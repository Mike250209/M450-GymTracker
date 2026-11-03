# GymTracker – M450 LB1

Minimaler Spring-Boot-Gym-Tracker als Testbasis für Modul 450.

## Voraussetzungen
- JDK 21
- Maven (oder IntelliJ mit Maven-Support)
- PostgreSQL lokal installiert (ohne Docker)

## Datenbank einrichten (einmalig, in psql oder pgAdmin)
```sql
CREATE USER gymtracker WITH PASSWORD 'gymtracker';
CREATE DATABASE gymtracker OWNER gymtracker;
```

## Starten
```
mvn spring-boot:run
```
Tabellen werden beim Start erzeugt, Testdaten kommen aus `data.sql`.

## Wo was hingehört
| Baustein | Klasse |
|---|---|
| 1 – Unit-Test mit Mocking | `service/WorkoutService` (Repositories + Calculator mocken) |
| 2 – Unit-Test für Logik | `service/TrainingCalculator` |
| 3 – Mutation-Testing | PIT, schon im `pom.xml` konfiguriert |
| 4 – TDD | neue Methode, z. B. Durchschnittsvolumen pro Workout eines Mitglieds |

Tests kommen nach `src/test/java/ch/m450/gymtracker/...`.

## Befehle
```
mvn test                                                   # Unit-Tests
mvn test-compile org.pitest:pitest-maven:mutationCoverage  # Mutation-Report
```
Der PIT-Report liegt danach unter `target/pit-reports/index.html`.
Das Build schlägt fehl, solange nicht 100 % der Mutanten getötet sind.

## Automatisierung
`.github/workflows/tests.yml` führt bei jedem Push Tests und PIT aus (3 Punkte beim Automatisierungsgrad).

## REST-Endpoints
Alle Beispiel-Requests liegen in `requests.http` (in IntelliJ direkt mit dem grünen Pfeil ausführbar).

| Methode | Pfad | Zweck |
|---|---|---|
| GET | `/members` | alle Mitglieder |
| GET | `/exercises` | alle Übungen |
| GET | `/members/{id}/workouts` | Workouts eines Mitglieds |
| GET | `/members/{id}/volume` | Gesamtvolumen |
| POST | `/workouts` | Workout starten |
| POST | `/workouts/{id}/sets` | Satz hinzufügen |
| DELETE | `/workouts/{id}` | Workout löschen |
| GET | `/calculator/one-rep-max?reps=&weight=` | 1RM berechnen |
| GET | `/calculator/strength-level?oneRepMax=&bodyWeight=` | Stärke einstufen |
