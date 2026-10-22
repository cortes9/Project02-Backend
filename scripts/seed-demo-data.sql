BEGIN;

-- All identifiers in the -13000 range are reserved for fictional demo data.
-- Explicit negative identity values avoid collisions without changing sequences.

INSERT INTO major (major_id, major_name)
VALUES
    (-13001, 'Demo Computer Science'),
    (-13002, 'Demo Mathematics'),
    (-13003, 'Demo Biology'),
    (-13004, 'Demo Chemistry'),
    (-13005, 'Demo Psychology')
ON CONFLICT (major_id) DO UPDATE
SET major_name = EXCLUDED.major_name;

INSERT INTO course (course_id, course_code, course_name)
VALUES
    (-13101, 'DEMO-CS101', 'Demo Introduction to Programming'),
    (-13102, 'DEMO-CS220', 'Demo Data Structures'),
    (-13103, 'DEMO-MATH120', 'Demo Calculus I'),
    (-13104, 'DEMO-MATH210', 'Demo Discrete Mathematics'),
    (-13105, 'DEMO-BIO110', 'Demo General Biology'),
    (-13106, 'DEMO-BIO230', 'Demo Genetics'),
    (-13107, 'DEMO-CHEM101', 'Demo General Chemistry'),
    (-13108, 'DEMO-CHEM220', 'Demo Organic Chemistry'),
    (-13109, 'DEMO-PSY101', 'Demo Introduction to Psychology'),
    (-13110, 'DEMO-PSY240', 'Demo Cognitive Psychology')
ON CONFLICT (course_id) DO UPDATE
SET course_code = EXCLUDED.course_code,
    course_name = EXCLUDED.course_name;

INSERT INTO building (building_id, building_name)
VALUES
    (-13201, 'Demo North Library'),
    (-13202, 'Demo Science Hall'),
    (-13203, 'Demo Learning Commons'),
    (-13204, 'Demo Engineering Center'),
    (-13205, 'Demo Student Union')
ON CONFLICT (building_id) DO UPDATE
SET building_name = EXCLUDED.building_name;

INSERT INTO "USER" (
    user_id,
    email,
    is_admin,
    major_id,
    name,
    oauth_subject,
    student_year
)
OVERRIDING SYSTEM VALUE
VALUES
    (-13301, 'demo.admin@example.invalid', TRUE, -13001, 'Demo Avery Admin', 'demo-oauth-subject-01', 'SENIOR'),
    (-13302, 'demo.alex@example.invalid', FALSE, -13001, 'Demo Alex Rivera', 'demo-oauth-subject-02', 'JUNIOR'),
    (-13303, 'demo.blair@example.invalid', FALSE, -13002, 'Demo Blair Chen', 'demo-oauth-subject-03', 'SOPHOMORE'),
    (-13304, 'demo.casey@example.invalid', FALSE, -13003, 'Demo Casey Morgan', 'demo-oauth-subject-04', 'FIRST_YEAR'),
    (-13305, 'demo.drew@example.invalid', FALSE, -13004, 'Demo Drew Patel', 'demo-oauth-subject-05', 'SENIOR'),
    (-13306, 'demo.ellis@example.invalid', FALSE, -13005, 'Demo Ellis Johnson', 'demo-oauth-subject-06', 'JUNIOR'),
    (-13307, 'demo.frankie@example.invalid', FALSE, -13001, 'Demo Frankie Kim', 'demo-oauth-subject-07', 'SOPHOMORE'),
    (-13308, 'demo.gray@example.invalid', FALSE, -13002, 'Demo Gray Williams', 'demo-oauth-subject-08', 'FIRST_YEAR'),
    (-13309, 'demo.hayden@example.invalid', FALSE, -13003, 'Demo Hayden Smith', 'demo-oauth-subject-09', 'SENIOR'),
    (-13310, 'demo.indigo@example.invalid', FALSE, -13004, 'Demo Indigo Davis', 'demo-oauth-subject-10', 'JUNIOR')
ON CONFLICT (user_id) DO UPDATE
SET email = EXCLUDED.email,
    is_admin = EXCLUDED.is_admin,
    major_id = EXCLUDED.major_id,
    name = EXCLUDED.name,
    oauth_subject = EXCLUDED.oauth_subject,
    student_year = EXCLUDED.student_year;

INSERT INTO study_session (
    session_id,
    building_id,
    capacity,
    course_id,
    creator_id,
    delivery_mode,
    description,
    start_time,
    end_time,
    status
)
OVERRIDING SYSTEM VALUE
VALUES
    (-13401, -13201, 4, -13101, -13301, 'IN_PERSON',
        'Demo programming practice and introductions.',
        ((CURRENT_DATE + 2) + TIME '09:00') AT TIME ZONE 'UTC',
        ((CURRENT_DATE + 2) + TIME '10:30') AT TIME ZONE 'UTC', 'OPEN'),
    (-13402, NULL, 2, -13102, -13302, 'REMOTE',
        'Demo remote data structures review.',
        ((CURRENT_DATE + 2) + TIME '11:00') AT TIME ZONE 'UTC',
        ((CURRENT_DATE + 2) + TIME '12:30') AT TIME ZONE 'UTC', 'FULL'),
    (-13403, NULL, 5, -13103, -13303, 'ASYNC',
        'Demo asynchronous calculus problem exchange.',
        ((CURRENT_DATE + 3) + TIME '08:00') AT TIME ZONE 'UTC',
        ((CURRENT_DATE + 3) + TIME '20:00') AT TIME ZONE 'UTC', 'OPEN'),
    (-13404, -13202, 6, -13104, -13304, 'IN_PERSON',
        'Demo discrete mathematics workshop.',
        ((CURRENT_DATE + 3) + TIME '10:00') AT TIME ZONE 'UTC',
        ((CURRENT_DATE + 3) + TIME '12:00') AT TIME ZONE 'UTC', 'OPEN'),
    (-13405, -13203, 1, -13105, -13305, 'IN_PERSON',
        'Demo focused biology quiz preparation.',
        ((CURRENT_DATE + 4) + TIME '13:00') AT TIME ZONE 'UTC',
        ((CURRENT_DATE + 4) + TIME '14:00') AT TIME ZONE 'UTC', 'FULL'),
    (-13406, NULL, 4, -13101, -13306, 'REMOTE',
        'Demo conflict-check candidate overlapping nearby sessions.',
        ((CURRENT_DATE + 2) + TIME '10:00') AT TIME ZONE 'UTC',
        ((CURRENT_DATE + 2) + TIME '11:00') AT TIME ZONE 'UTC', 'OPEN'),
    (-13407, -13204, 5, -13106, -13307, 'IN_PERSON',
        'Demo genetics discussion following the morning session.',
        ((CURRENT_DATE + 2) + TIME '10:30') AT TIME ZONE 'UTC',
        ((CURRENT_DATE + 2) + TIME '11:30') AT TIME ZONE 'UTC', 'OPEN'),
    (-13408, -13205, 4, -13107, -13308, 'IN_PERSON',
        'Demo chemistry fundamentals review.',
        ((CURRENT_DATE + 5) + TIME '09:30') AT TIME ZONE 'UTC',
        ((CURRENT_DATE + 5) + TIME '11:00') AT TIME ZONE 'UTC', 'OPEN'),
    (-13409, NULL, 3, -13108, -13309, 'REMOTE',
        'Demo full organic chemistry exam review.',
        ((CURRENT_DATE + 6) + TIME '16:00') AT TIME ZONE 'UTC',
        ((CURRENT_DATE + 6) + TIME '18:00') AT TIME ZONE 'UTC', 'FULL'),
    (-13410, -13201, 5, -13109, -13310, 'IN_PERSON',
        'Demo psychology reading discussion.',
        ((CURRENT_DATE + 7) + TIME '12:00') AT TIME ZONE 'UTC',
        ((CURRENT_DATE + 7) + TIME '13:30') AT TIME ZONE 'UTC', 'OPEN'),
    (-13411, NULL, 8, -13110, -13301, 'ASYNC',
        'Demo asynchronous cognitive psychology study board.',
        ((CURRENT_DATE + 8) + TIME '08:00') AT TIME ZONE 'UTC',
        ((CURRENT_DATE + 9) + TIME '20:00') AT TIME ZONE 'UTC', 'OPEN'),
    (-13412, -13202, 3, -13102, -13302, 'IN_PERSON',
        'Demo algorithm tracing practice.',
        ((CURRENT_DATE + 10) + TIME '14:00') AT TIME ZONE 'UTC',
        ((CURRENT_DATE + 10) + TIME '15:30') AT TIME ZONE 'UTC', 'OPEN'),
    (-13413, NULL, 6, -13104, -13303, 'REMOTE',
        'Demo remote proofs and logic review.',
        ((CURRENT_DATE + 12) + TIME '17:00') AT TIME ZONE 'UTC',
        ((CURRENT_DATE + 12) + TIME '18:30') AT TIME ZONE 'UTC', 'OPEN'),
    (-13414, -13204, 2, -13106, -13304, 'IN_PERSON',
        'Demo full genetics lab preparation.',
        ((CURRENT_DATE + 14) + TIME '10:00') AT TIME ZONE 'UTC',
        ((CURRENT_DATE + 14) + TIME '12:00') AT TIME ZONE 'UTC', 'FULL'),
    (-13415, -13203, 4, -13110, -13305, 'IN_PERSON',
        'Demo cognitive psychology final review.',
        ((CURRENT_DATE + 16) + TIME '15:00') AT TIME ZONE 'UTC',
        ((CURRENT_DATE + 16) + TIME '16:30') AT TIME ZONE 'UTC', 'OPEN')
ON CONFLICT (session_id) DO UPDATE
SET building_id = EXCLUDED.building_id,
    capacity = EXCLUDED.capacity,
    course_id = EXCLUDED.course_id,
    creator_id = EXCLUDED.creator_id,
    delivery_mode = EXCLUDED.delivery_mode,
    description = EXCLUDED.description,
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    status = EXCLUDED.status;

INSERT INTO session_major (major_id, session_id)
VALUES
    (-13001, -13401), (-13002, -13401),
    (-13001, -13402),
    (-13003, -13403), (-13004, -13403),
    (-13002, -13404), (-13005, -13404),
    (-13003, -13405),
    (-13001, -13406), (-13004, -13406),
    (-13002, -13407),
    (-13005, -13408),
    (-13001, -13409), (-13002, -13409),
    (-13003, -13410),
    (-13004, -13411), (-13005, -13411),
    (-13001, -13412),
    (-13002, -13413), (-13003, -13413),
    (-13004, -13414),
    (-13005, -13415), (-13001, -13415)
ON CONFLICT (major_id, session_id) DO NOTHING;

INSERT INTO session_member (session_id, user_id)
VALUES
    (-13401, -13302),
    (-13402, -13303), (-13402, -13304),
    (-13404, -13304), (-13404, -13305),
    (-13405, -13306),
    (-13407, -13302),
    (-13408, -13305),
    (-13409, -13301), (-13409, -13307), (-13409, -13308),
    (-13410, -13301),
    (-13412, -13307),
    (-13414, -13309), (-13414, -13310),
    (-13415, -13303)
ON CONFLICT (user_id, session_id) DO NOTHING;

COMMIT;
