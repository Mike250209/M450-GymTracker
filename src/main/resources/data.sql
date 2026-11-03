INSERT INTO member (name, body_weight) VALUES ('Mike', 75.0), ('Julian', 80.0);

INSERT INTO exercise (name, muscle_group) VALUES
  ('Bankdrücken', 'BRUST'),
  ('Kniebeuge', 'BEINE'),
  ('Kreuzheben', 'RUECKEN');

INSERT INTO workout (date, member_id) VALUES
  ('2026-10-20', 1), ('2026-10-23', 1), ('2026-10-21', 2);

INSERT INTO workout_set (workout_id, exercise_id, reps, weight) VALUES
  (1, 1, 8, 60.0), (1, 1, 6, 65.0), (1, 2, 10, 80.0),
  (2, 3, 5, 100.0), (2, 1, 8, 62.5),
  (3, 2, 8, 90.0), (3, 3, 5, 110.0);
