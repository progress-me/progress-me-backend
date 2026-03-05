-- ============================================================
-- Seed script: Import Nazar's progress data (Dec 2025 – Feb 2026)
-- User: c22b1348-6b1e-4077-8c1d-0c3165956e76
-- Run: psql -d progress_analyzer -f seed_nazar_data.sql
-- Idempotent: uses ON CONFLICT DO NOTHING
-- ============================================================

BEGIN;

-- ======================== CONSTANTS ========================

-- User
-- c22b1348-6b1e-4077-8c1d-0c3165956e76 (already exists)

-- Categories
-- cat_career    = ea7736ac-04f3-4180-b0b6-a1be276eb7ab
-- cat_health    = 0510a50f-83ab-4979-b552-f39080f24700
-- cat_learning  = 6d37f60c-f044-445d-aef0-926f68ad53f7
-- cat_growth    = 95114efa-6fb1-4f94-9cd5-f6ac163943d1
-- cat_projects  = 8b206bd4-9fd3-4b35-b309-24eb4dffc680

-- Values
-- val_productivity     = 5e6debfa-ccb6-4cc4-af84-7b7c4721893f
-- val_work_life_bal    = ec86159a-4501-4f1b-8b66-554982e183c6
-- val_personal_brand   = 6f41ed1c-2b3d-45d9-9f30-31cea8e0b870
-- val_exercise         = 3e36b257-fc43-4553-9fd8-da7dc07fa627
-- val_rest             = ec45499d-d4b2-4355-9efe-7be6be23f9da
-- val_mental_health    = a21c0d83-c207-42ed-8c42-9b8361c346cc
-- val_system_design    = 4df5a7b9-77ee-4bf8-bba9-b839e1874ca7
-- val_ai_ml            = 6a0d85fe-8df2-4804-848d-44b3763ff81f
-- val_algorithms       = 1e331660-c1e1-4052-8256-9b82aed6063f
-- val_cloud            = 18e2c533-7da8-4374-9b20-82f3382598d2
-- val_self_reflection  = 471bc567-6d18-4cf4-8e0d-91927599d7c7
-- val_social           = 1c918f1f-6ff7-409b-be0e-2bf1db61cbe5
-- val_improvement      = 2ddd6ece-6d3a-4a1f-98f7-38824ef2d61f
-- val_entrepreneurship = f7b031ed-2820-4cae-8591-e98e8deca363
-- val_creativity       = 51307161-e86c-4479-8332-a7e8617a1ade

-- Goals
-- goal_salary       = 0efe3837-44fe-497c-810e-27791fcf7930
-- goal_ai_ml        = 544039d3-548a-4e7b-8930-97c6c3086691
-- goal_sys_design   = d0f6e640-981e-4bc2-bf5c-ce00e44d2dec
-- goal_progress_app = a423677c-0e0d-4099-949f-f32668520eb1
-- goal_certificates = 30a1892b-af38-4bd4-a6a8-5dda3600603d
-- goal_english      = 52047c38-962a-423d-bb80-831a494cb188
-- goal_fitness      = 1c583ca5-a44b-425b-af78-b5733f28fbbd

-- ======================== PERSONA ========================

INSERT INTO personas (id, user_id, display_name, age, bio, strengths, weaknesses, interests, context)
VALUES (
  '91ecd726-63b7-44a3-8936-a7b86dfe25c9',
  'c22b1348-6b1e-4077-8c1d-0c3165956e76',
  'Nazar',
  22,
  'My life is a project that may change my life as an engineer! I am focused on becoming a strong developer who can confidently use modern technologies, build products, and grow as a leader.',
  'Love trying new hobbies, very creative, flexible, high productivity when focused, good at learning new things quickly',
  'Easy to distract, may do too much work, sometimes forgets small details in pursuit of understanding the bigger picture',
  'Technologies, classic books, coffee, AI/ML, system design, personal growth',
  'Male, 22 years old, has a partner. Currently working as a developer earning 12000 PLN, targeting 18000 PLN by mid-2026. Attends therapy regularly. Participates in System Design course, IT community events. Building personal brand through Telegram channel. Personality analysis suggests leadership path (team lead) rather than pure senior developer track.'
)
ON CONFLICT (user_id) DO NOTHING;

-- ======================== AI SETTINGS ========================

INSERT INTO ai_settings (id, user_id, tone, strictness, focus_areas, ignore_areas, custom_instructions, language)
VALUES (
  'c8fd6a76-7b22-4af8-a54f-c1239c724ad0',
  'c22b1348-6b1e-4077-8c1d-0c3165956e76',
  'balanced',
  6,
  '["career_growth", "productivity", "learning", "personal_development", "side_projects"]'::jsonb,
  '[]'::jsonb,
  'Focus on patterns in productivity, energy levels, and career progression. Track progress toward salary goal (12k→18k PLN). Monitor work-life balance and burnout risk. Consider therapy insights when analyzing mental health patterns.',
  'en'
)
ON CONFLICT (user_id) DO NOTHING;

-- ======================== CATEGORIES ========================

INSERT INTO categories (id, user_id, name, description, color, icon, sort_order) VALUES
  ('ea7736ac-04f3-4180-b0b6-a1be276eb7ab', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'Career & Work', 'Professional development, work productivity, and salary growth', '#2563EB', 'briefcase', 0),
  ('0510a50f-83ab-4979-b552-f39080f24700', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'Health & Wellness', 'Physical fitness, rest, and mental health', '#10B981', 'heart', 1),
  ('6d37f60c-f044-445d-aef0-926f68ad53f7', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'Learning & Education', 'System design, AI/ML, algorithms, and cloud technologies', '#8B5CF6', 'book-open', 2),
  ('95114efa-6fb1-4f94-9cd5-f6ac163943d1', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'Personal Growth', 'Self-reflection, relationships, and continuous improvement', '#F59E0B', 'sparkles', 3),
  ('8b206bd4-9fd3-4b35-b309-24eb4dffc680', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'Side Projects', 'Personal projects and entrepreneurial ventures', '#EF4444', 'rocket', 4)
ON CONFLICT (user_id, name) DO NOTHING;

-- ======================== VALUES ========================

INSERT INTO user_values (id, user_id, category_id, name, description, sort_order) VALUES
  -- Career & Work
  ('5e6debfa-ccb6-4cc4-af84-7b7c4721893f', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'ea7736ac-04f3-4180-b0b6-a1be276eb7ab', 'Productivity', 'Staying focused and efficient in work', 0),
  ('ec86159a-4501-4f1b-8b66-554982e183c6', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'ea7736ac-04f3-4180-b0b6-a1be276eb7ab', 'Work-Life Balance', 'Maintaining healthy boundaries between work and personal life', 1),
  ('6f41ed1c-2b3d-45d9-9f30-31cea8e0b870', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'ea7736ac-04f3-4180-b0b6-a1be276eb7ab', 'Personal Branding', 'Building reputation in IT community through content and networking', 2),
  -- Health & Wellness
  ('3e36b257-fc43-4553-9fd8-da7dc07fa627', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', '0510a50f-83ab-4979-b552-f39080f24700', 'Physical Exercise', 'Gym, walking, home exercises', 0),
  ('ec45499d-d4b2-4355-9efe-7be6be23f9da', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', '0510a50f-83ab-4979-b552-f39080f24700', 'Rest & Recovery', 'Learning to rest effectively — podcasts, radio dramas, walks', 1),
  ('a21c0d83-c207-42ed-8c42-9b8361c346cc', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', '0510a50f-83ab-4979-b552-f39080f24700', 'Mental Health', 'Therapy sessions, stress management, self-awareness', 2),
  -- Learning & Education
  ('4df5a7b9-77ee-4bf8-bba9-b839e1874ca7', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', '6d37f60c-f044-445d-aef0-926f68ad53f7', 'System Design', 'Studying system design patterns, attending course', 0),
  ('6a0d85fe-8df2-4804-848d-44b3763ff81f', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', '6d37f60c-f044-445d-aef0-926f68ad53f7', 'AI & Machine Learning', 'Understanding AI agents, RAG systems, embeddings, Vertex AI', 1),
  ('1e331660-c1e1-4052-8256-9b82aed6063f', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', '6d37f60c-f044-445d-aef0-926f68ad53f7', 'Algorithms', 'Sorting, graphs, intervals — algorithm challenges', 2),
  ('18e2c533-7da8-4374-9b20-82f3382598d2', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', '6d37f60c-f044-445d-aef0-926f68ad53f7', 'Cloud Technologies', 'AWS (ECS, EC2, Lambda, S3, IAM), GCP (Vertex AI)', 3),
  -- Personal Growth
  ('471bc567-6d18-4cf4-8e0d-91927599d7c7', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', '95114efa-6fb1-4f94-9cd5-f6ac163943d1', 'Self-Reflection', 'Regular introspection, journaling, understanding personality patterns', 0),
  ('1c918f1f-6ff7-409b-be0e-2bf1db61cbe5', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', '95114efa-6fb1-4f94-9cd5-f6ac163943d1', 'Social Connections', 'Friends, IT community, Telegram channel, meetups', 1),
  ('2ddd6ece-6d3a-4a1f-98f7-38824ef2d61f', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', '95114efa-6fb1-4f94-9cd5-f6ac163943d1', 'Continuous Improvement', 'Always learning, iterating on processes, refining routines', 2),
  -- Side Projects
  ('f7b031ed-2820-4cae-8591-e98e8deca363', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', '8b206bd4-9fd3-4b35-b309-24eb4dffc680', 'Entrepreneurship', 'Building products, exploring revenue streams, market research', 0),
  ('51307161-e86c-4479-8332-a7e8617a1ade', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', '8b206bd4-9fd3-4b35-b309-24eb4dffc680', 'Creativity', 'Creative solutions, portfolio building, content creation', 1)
ON CONFLICT (user_id, category_id, name) DO NOTHING;

-- ======================== GOALS ========================

-- 1. Main SMART Goal: Salary increase
INSERT INTO goals (id, user_id, title, description, status, start_date, target_date, template_type, template_data, is_default, created_at)
VALUES (
  '0efe3837-44fe-497c-810e-27791fcf7930',
  'c22b1348-6b1e-4077-8c1d-0c3165956e76',
  'Increase salary from 12000 to 18000 PLN',
  'Main career goal: grow from 12000 PLN to 18000 PLN through skill development, certifications, and job progression. Intermediate target: 14000 PLN by February.',
  'active',
  '2025-12-01',
  '2026-06-01',
  'smart',
  '{
    "specific": "Become a developer who can confidently use algorithms, patterns, Hibernate, AWS, GCP. Master Java, Python, and frontend technologies. Increase salary from 12000 PLN to 18000 PLN.",
    "measurable": "Read/understood books, completed deadlines, finished courses, overall improvements, salary milestones (14k, 18k).",
    "achievable": "Tasks filtered by current level. Realistic deadlines. Include resting. Can delete or edit tasks as needed.",
    "relevant": "A developer must be useful — choose technologies that help others solve problems right now.",
    "time_bound": "December 1, 2025 to June 1, 2026"
  }'::jsonb,
  false,
  '2025-12-01T00:00:00+01:00'
)
ON CONFLICT DO NOTHING;

-- 2. Learn AI/ML
INSERT INTO goals (id, user_id, title, description, status, start_date, target_date, is_default, created_at)
VALUES (
  '544039d3-548a-4e7b-8930-97c6c3086691',
  'c22b1348-6b1e-4077-8c1d-0c3165956e76',
  'Learn AI/ML fundamentals',
  'Start AI/ML course, understand agents and RAG systems, work with embeddings and Vertex AI.',
  'active',
  '2026-01-01',
  '2026-06-01',
  false,
  '2026-01-01T00:00:00+01:00'
)
ON CONFLICT DO NOTHING;

-- 3. Master System Design
INSERT INTO goals (id, user_id, title, description, status, start_date, target_date, is_default, created_at)
VALUES (
  'd0f6e640-981e-4bc2-bf5c-ce00e44d2dec',
  'c22b1348-6b1e-4077-8c1d-0c3165956e76',
  'Master System Design',
  'Complete System Design course, attend weekly lessons, do homework, study networking, API design, core concepts.',
  'active',
  '2026-01-01',
  '2026-06-01',
  false,
  '2026-01-01T00:00:00+01:00'
)
ON CONFLICT DO NOTHING;

-- 4. Build Progress Analyzer MVP
INSERT INTO goals (id, user_id, title, description, status, start_date, target_date, is_default, created_at)
VALUES (
  'a423677c-0e0d-4099-949f-f32668520eb1',
  'c22b1348-6b1e-4077-8c1d-0c3165956e76',
  'Build Progress Analyzer MVP',
  'Create the progress-analyzer application to track personal growth, analyze patterns with AI.',
  'active',
  '2025-12-15',
  '2026-03-31',
  false,
  '2025-12-15T00:00:00+01:00'
)
ON CONFLICT DO NOTHING;

-- 5. Obtain Certificates
INSERT INTO goals (id, user_id, title, description, status, start_date, target_date, is_default, created_at)
VALUES (
  '30a1892b-af38-4bd4-a6a8-5dda3600603d',
  'c22b1348-6b1e-4077-8c1d-0c3165956e76',
  'Obtain professional certificates',
  'Prepare for and obtain technical certificates. Preparation in April, exams in May.',
  'active',
  '2026-04-01',
  '2026-05-31',
  false,
  '2025-12-01T00:00:00+01:00'
)
ON CONFLICT DO NOTHING;

-- 6. Improve English
INSERT INTO goals (id, user_id, title, description, status, start_date, target_date, is_default, created_at)
VALUES (
  '52047c38-962a-423d-bb80-831a494cb188',
  'c22b1348-6b1e-4077-8c1d-0c3165956e76',
  'Improve English speaking level',
  'Attend weekly English classes, practice speaking and professional communication.',
  'active',
  '2026-01-01',
  '2026-06-01',
  false,
  '2026-01-01T00:00:00+01:00'
)
ON CONFLICT DO NOTHING;

-- 7. Build fitness routine
INSERT INTO goals (id, user_id, title, description, status, start_date, target_date, is_default, created_at)
VALUES (
  '1c583ca5-a44b-425b-af78-b5733f28fbbd',
  'c22b1348-6b1e-4077-8c1d-0c3165956e76',
  'Build consistent fitness routine',
  'Go to gym regularly, walk daily (especially 3PM energy boost walks), do home exercises.',
  'active',
  '2026-01-01',
  '2026-06-01',
  false,
  '2026-01-01T00:00:00+01:00'
)
ON CONFLICT DO NOTHING;

-- ======================== GOAL_CATEGORIES ========================

INSERT INTO goal_categories (goal_id, category_id) VALUES
  -- Salary goal → Career
  ('0efe3837-44fe-497c-810e-27791fcf7930', 'ea7736ac-04f3-4180-b0b6-a1be276eb7ab'),
  -- AI/ML → Learning
  ('544039d3-548a-4e7b-8930-97c6c3086691', '6d37f60c-f044-445d-aef0-926f68ad53f7'),
  -- System Design → Learning
  ('d0f6e640-981e-4bc2-bf5c-ce00e44d2dec', '6d37f60c-f044-445d-aef0-926f68ad53f7'),
  -- Progress App → Side Projects
  ('a423677c-0e0d-4099-949f-f32668520eb1', '8b206bd4-9fd3-4b35-b309-24eb4dffc680'),
  -- Certificates → Learning, Career
  ('30a1892b-af38-4bd4-a6a8-5dda3600603d', '6d37f60c-f044-445d-aef0-926f68ad53f7'),
  ('30a1892b-af38-4bd4-a6a8-5dda3600603d', 'ea7736ac-04f3-4180-b0b6-a1be276eb7ab'),
  -- English → Learning
  ('52047c38-962a-423d-bb80-831a494cb188', '6d37f60c-f044-445d-aef0-926f68ad53f7'),
  -- Fitness → Health
  ('1c583ca5-a44b-425b-af78-b5733f28fbbd', '0510a50f-83ab-4979-b552-f39080f24700')
ON CONFLICT DO NOTHING;

-- ======================== LOG ENTRIES ========================
-- Type: journal (reflective diary entries)
-- Mood/energy inferred from text sentiment (1-10 scale, NULL if unclear)

-- ---- DECEMBER 2025 ----

-- Dec Week 1 (whole week summary)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('dd819f78-c8b5-4b97-8a56-92413ae1b1dd', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'This week I created a plan and learned how to use it effectively. I also started using new task-tracking tools, such as ClickUp, which will help me track all tasks and plan upcoming weeks. Additionally, I offered to help a friend with his Telegram bot, and now I have a small new project that I can improve and develop.

I struggled with a new project and asked my teammates how they manage multiple projects at the same time. What I''m going to try next: switching context once an hour, getting used to frequent context switching, learning more about the project itself. I also started bringing back some of my old skills, and I''m very proud of that.',
7, 7, '2025-12-07T20:00:00+01:00', '2025-12-07T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Dec Week 2 (whole week)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('74a47fa7-efb6-4717-900e-21dca25d0ac5', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'This week I''m implementing new features for the bot, and it''s going really well. I''m very happy with how productive I''ve been. I also started listening to Pomodoro Lo-fi Girl, which helps me stay focused and be extremely productive at work.',
9, 8, '2025-12-14T20:00:00+01:00', '2025-12-14T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Dec Week 3 (whole week — conference)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('984120c3-06e5-4474-bb83-a20ecbf15398', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'This week I''m on IT conference and doing a lot of notes which will help me in future to be a good developer! I learned a lot and I think I should be a product-minded engineer which can do anything and not be scared about problems, just do work confidently and not be scared asking for help! Also use AI as a tool and not a competitor!

I worked a lot for my name in IT community, that is also good for becoming a great engineer! I''ve created my small project which can analyze my progress and help me with building my way.',
8, 8, '2025-12-21T20:00:00+01:00', '2025-12-21T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Dec Week 4 — Monday
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('81254e18-9393-4f79-8a5b-c09494c3d85a', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'This week started awesome! After therapy session I understood that to be more productive I need to focus on path to goal. It''s like to get water I need to just grab water, but in case if it''s someone''s water I need to find out how to reach for water. In first case flow looks like "me → goal", but in case of obstacles it looks like "me → problem → goal", so to become better in resolving those problems I would need to check how I manage those obstacles and learn how to live with those patterns.

Also, I noticed that I work a lot and my level of productivity and happiness is on very high level!',
9, 8, '2025-12-22T20:00:00+01:00', '2025-12-22T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Dec Week 4 — Tuesday
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('74f08840-77fc-4836-b723-4ace492ea5ed', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Today is lazy day, I''m doing practically nothing, it''s because my progress analyzer told that I don''t mention any resting. I''ve been playing a lot today and I''m happy about that! Also, it would be a long weekend, I''d do something interesting on those days!

The biggest problem this week is that I haven''t planned anything in ClickUp, maybe because this week is a bit chaotic. I''ll fill it asap!',
7, 4, '2025-12-23T20:00:00+01:00', '2025-12-23T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Dec Week 4 — Friday
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('85a9186b-0c2a-48e2-aa0f-3be6bb1af7da', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'I''ve filled plan for this week in ClickUp and finished plans in that week. Because of Christmas holidays, my productivity is lower this week, but I started doing fascinating things — I''ve created a portfolio page on which I can post my resume on developer page, share this page with recruiters, and build my personal brand!',
7, 5, '2025-12-26T20:00:00+01:00', '2025-12-26T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Dec Week 5 — Start (therapy + rest discovery)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('7c7a3981-9c29-4e91-904e-e03e0ab6574d', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'This week started with my therapy session and it was great! We focused on resting and I started trying new types of resting. I know that because of my sitting job my eyes are hurting me a lot, so I needed to find something that can reduce this pain.

I started listening to radio dramas/podcasts, so my eyes are resting, while my head can unfocus from ideas and job by listening narration. When I listen to those things I noticed that I fall in some sort of dream state in which I listen and dream in the same time. After 10-20 minutes I feel fully rested (WOW).

I also reflect on how my parents are resting — they only use sleep to rest. Maybe that''s why my head is thinking that sleep is the best way to rest.',
8, 6, '2025-12-29T20:00:00+01:00', '2025-12-29T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Dec Week 5 — New Year's Eve
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('0123790b-7960-4430-b9e7-f2811fe585e9', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'New year''s eve is interesting time, because of guests in my home I cannot fully focus on tasks, but my head is always thinking about something new.',
6, 5, '2025-12-31T20:00:00+01:00', '2025-12-31T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Dec Week 5 — Planning for January
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('c264c819-0f80-4221-8785-230d2eb98bc8', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Planning for January. On overall plan, this month I should focus on working a lot and find out new solutions how to be more productive, but also stay human. Approximate plan: focus on startup with analyzer, generate new solutions for continuous progression.',
7, 6, '2026-01-02T20:00:00+01:00', '2026-01-02T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- December Conclusion
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('ab778999-6306-4ecb-9816-3a6991c7d390', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'DECEMBER CONCLUSION: This month I''ve learned how to build a system and stick to the plan. Also, I found a new way how to rest and will try to improve it in the future! Applied to the new small project to learn new things. Started small startup with progress analyzer. Managed my work-life routine. I feel that this month was very productive and created really cool base for future months!',
8, 7, '2025-12-31T23:00:00+01:00', '2025-12-31T23:00:00+01:00')
ON CONFLICT DO NOTHING;

-- ---- JANUARY 2026 ----

-- Jan Week 1 — Thursday (Jan 1)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('aaec5e04-84fb-4c62-8a8b-4afff7004daf', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'I prepared approximate plan on how I should progress this month.',
6, 5, '2026-01-01T20:00:00+01:00', '2026-01-01T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Jan Week 1 — Sunday (Jan 4)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('594cac1f-590d-4b07-8e58-3e7a7764a4fa', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'I had a wonderful time with my friends, we spoke a lot about productivity and plans for this year. By the end of this year we''ll change significantly and will be on another level! I and Yura will create Telegram channel in which we''ll share our thoughts about our lifestyle and way of thinking.

Also from 12th of January will start my programming program on which I''ll learn new things in a very complex matter! Will need to learn system design. Also, I will create a vision of what I want to achieve by the end of this year. I''m happy to be that productive and gain so much knowledge!',
8, 7, '2026-01-04T20:00:00+01:00', '2026-01-04T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Jan Week 2 — Monday (Jan 5)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('f8fead1c-69f6-42f0-878b-880a5ec67e09', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Scope done: made a call about AI/ML qualification — found a free course, will join it soon. Added tasks for ClickUp. Started system design meetups.',
7, 7, '2026-01-05T20:00:00+01:00', '2026-01-05T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Jan Week 2 — Tuesday (Jan 6)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('d4fa2e18-cd36-460e-8c57-20d8282c9108', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Completed localization for coffee bot. Attended organizational meetup for IT community. Helped my friend with organizing Google spreadsheets.',
7, 7, '2026-01-06T20:00:00+01:00', '2026-01-06T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Jan Week 2 — Wednesday (Jan 7)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('1c5e82eb-75c7-47c4-862c-d28cf349b754', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Fixed issue in coffee bot. Learned more about AI agents and RAG systems. I feel exhausted for today, tomorrow should be easy and resting day.',
6, 3, '2026-01-07T20:00:00+01:00', '2026-01-07T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Jan Week 2 — Thursday (Jan 8)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('950cd7d0-69cf-4b73-9899-93fa887836ee', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Today is quite easy day! But I reflect on year achievements and plan and understood that I want to earn 18000 PLN brutto by the end of this year. So, my head started to work a little bit different: plan for Feb right now is changed to increase salary from 12000 to 14000 by finishing tasks on work and learning new skills.',
7, 5, '2026-01-08T20:00:00+01:00', '2026-01-08T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Jan Week 2 — Friday (Jan 9)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('25cc1b97-f283-4281-a4cb-57d2aa5abcf9', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Today also was an easy day, because I tried to avoid learning about System design (for now why I avoid learning tasks). Also, I''ve created a Telegram channel on which I and my friend are sharing thoughts and try working on personal growth to build our own brand.',
6, 5, '2026-01-09T20:00:00+01:00', '2026-01-09T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Jan Week 2 — Saturday (Jan 10)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('27a8e2a9-7920-4194-b60b-598a66f4e8c5', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Today I learned two topics from System Design course. I learned a lot and found out why I cannot start new project — all because of lack of understanding how system design for my ideas should look like. I also learned a lot about core concepts and what I need to renew in my memory to move to the next level.

Overall learned: what is system design, how system design is affecting development, where do I need to focus. On next session I''ll learn about core concepts, patterns and delivery frameworks.

Also, I used another way for resting — I''ve just watched in the window, which calmed me down really quickly.',
8, 7, '2026-01-10T20:00:00+01:00', '2026-01-10T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Jan Week 2 — Sunday (Jan 11)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('21a4187c-96ea-42ed-a2ef-afad48a3a9ac', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Today I wanted to be productive, but I need to overwork a bit in evening because of deadline for Tuesday. I walked and relaxed a lot today, so I fulfilled my social balance for today.',
6, 5, '2026-01-11T20:00:00+01:00', '2026-01-11T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Jan Week 3 — Monday (Jan 12)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('5fafaf1a-c17d-4fda-94ab-5a6a68905550', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'I''ve been preparing to the system design meeting.',
6, 6, '2026-01-12T20:00:00+01:00', '2026-01-12T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Jan Week 3 — Tuesday (Jan 13)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('8ce09e17-cf80-41e5-9166-972cce535d96', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Unfortunately I felt very sick so used a lot of my resources for preparation and attending on System Design meeting. On meeting, I''ve learned how System design affects programmers life and how to improve systems we develop.',
4, 2, '2026-01-13T20:00:00+01:00', '2026-01-13T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Jan Week 3 — Wednesday (Jan 14)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('0bd88ff2-2fc6-46f0-8d1b-830a3531c3cf', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Worked a bit, made a plan for the week. Problem is that I cannot focus today on the plan for a week, I''ll fix this issue a little bit later.',
5, 4, '2026-01-14T20:00:00+01:00', '2026-01-14T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Jan Week 3 — Thursday (Jan 15)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('e29902dc-dcdc-49df-9461-5a59f4b09430', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Today I''m planning productive work. Finished one algorithm. Finished 1/3 Networking essentials. Attended English classes.',
7, 6, '2026-01-15T20:00:00+01:00', '2026-01-15T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Jan Week 4 — Monday (Jan 19)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('a285ff46-9314-458e-b63b-dc73914cc174', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Today I started with video about how a real programmer should behave and solve problems. With that knowledge I changed my expectations and start doing real job. By 4 PM I finished with ancient task about upgrading Gradle to the latest. Made a template for upgrading versions.',
8, 8, '2026-01-19T20:00:00+01:00', '2026-01-19T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Jan Week 4 — Tuesday (Jan 20)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('ecdb1390-20f2-4897-8266-cae34588a864', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Today I finished with upgrading to Java 25 and finished successfully! Also finished with updating old versions in our platform. As well gave for review my code reviewer assistant to my teammates. Very promising week has started.

In evening unfortunately my system design lesson was skipped by mentor so I found some time to reflect on week and improve my progress.',
8, 7, '2026-01-20T20:00:00+01:00', '2026-01-20T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Jan Week 4 — Saturday (Jan 24)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('5ca5e385-114e-4349-8356-7af8a01bb8d5', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'I worked a lot like on previous days. I''ve created a request to increase my salary from 12000 → 14000 PLN.',
8, 7, '2026-01-24T20:00:00+01:00', '2026-01-24T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Jan Week 5 — Monday (Jan 26)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('a83e6122-a261-49b7-bad6-19fa98f73ba2', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Today I''m planning to attend on all meetings and do a little bit related to my work. I will use pomodoro with bigger gaps for today. I feel that this week would be less productive than previous, so I''ll try not to burn out with that. Started monitoring my progress in physical journal.',
6, 5, '2026-01-26T20:00:00+01:00', '2026-01-26T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Jan Week 5 — Tuesday (Jan 27)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('9271f6cd-1211-48ac-a962-89363e04f540', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'I worked a little bit, researched on how to start small projects that can gain some money, so I''ll be more work independent and may reach my goal faster. Instead of gym I walk a lot and try to do exercises at home. Participated on System Design lesson, it was very useful for me.',
7, 6, '2026-01-27T20:00:00+01:00', '2026-01-27T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Jan Week 5 — Wednesday (Jan 28)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('9ff71b57-874f-4b06-b375-d4549f6825a5', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Today I bought Claude Code and learned how it works. Have started to learn basics and tried to build first demo. Improved my-prof life embeddings generation.',
8, 7, '2026-01-28T20:00:00+01:00', '2026-01-28T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Jan Week 5 — Thursday (Jan 29)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('bec2abe5-4aad-4a20-8859-29d5d7526ea4', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Have created iOS app, researched how market works. Created first agent.',
8, 7, '2026-01-29T20:00:00+01:00', '2026-01-29T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Jan Week 5 — Friday (Jan 30)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('92bd719b-5bc2-4ec1-977e-602bfd60953d', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Learned new algorithm, Intervals, and created video for team how it works. Created plan for prosto-learn platform using Claude.',
8, 7, '2026-01-30T20:00:00+01:00', '2026-01-30T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Jan Week 5 — Saturday (Jan 31)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('e6a1b2c3-d4e5-4f60-a1b2-c3d4e5f60a1b', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Started implementing prosto-learn using Claude. I have finished 3 days plan. Added conclusions for January. The minus is my free time has shortened a little bit.',
7, 6, '2026-01-31T20:00:00+01:00', '2026-01-31T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Jan Week 5 — Sunday (Feb 1)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('f7a2b3c4-d5e6-4f70-a2b3-c4d5e6f70a2b', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Today I worked on prosto-learn, prepared main endpoints for MVP, so I can start creating frontend part. I''m planning to create first MVP version before 08.02, so I can share it with people from Threads to hear their thoughts. Also I''m planning to work more about understanding my personality, to understand who I am exactly.',
8, 7, '2026-02-01T20:00:00+01:00', '2026-02-01T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- January Conclusion
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('a1b2c3d4-e5f6-4a70-b1c2-d3e4f5a6b7c8', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'JANUARY CONCLUSION: This month was intense and insightful. I''ve learned a lot of AI-related skills, grew few new skills related to networking and databases. Have found out that my level of energy is bigger when I walk at 3PM every day. I asked for salary review from 12000 to 14000.

I started to be focused at improvements of my personal progress and become focused on gaining more money from current possibilities. I learn, I work, and my productivity is stable and high!',
8, 7, '2026-01-31T23:00:00+01:00', '2026-01-31T23:00:00+01:00')
ON CONFLICT DO NOTHING;

-- ---- FEBRUARY 2026 ----

-- Feb Week 1 — Monday (Feb 2)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('b2c3d4e5-f6a7-4b80-c2d3-e4f5a6b7c8d9', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Today I started with plan for frontend implementation. Relaxed other part of a day.',
6, 5, '2026-02-02T20:00:00+01:00', '2026-02-02T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Feb Week 1 — Tuesday (Feb 3)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('c3d4e5f6-a7b8-4c90-d3e4-f5a6b7c8d9e0', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Been on therapy, have spoken about my energy levels and how to improve quality of my productivity time. So, all my stress now is because of my stress for future as an interpreter. Have attended on a System Design session.',
6, 5, '2026-02-03T20:00:00+01:00', '2026-02-03T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Feb Week 1 — Wednesday (Feb 4)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('d4e5f6a7-b8c9-4da0-e4f5-a6b7c8d9e0f1', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Today I''ve created research based on my personality type and all of my days I create analysis. My analysis told me that I need to start focusing on becoming team lead, not a senior developer. My productivity strictly depends on people and their reactions, so I need to start focusing on sharing information with people and start building my support system, so I may fill my inner needs.

I need to review my project a lot and fulfill my gaps, because people with personality like I have are forgetting small details in purpose of understanding bigger picture.',
7, 6, '2026-02-04T20:00:00+01:00', '2026-02-04T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Feb Week 1 — Rest of week (Feb 7)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('e5f6a7b8-c9d0-4eb0-f5a6-b7c8d9e0f1a2', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'I''ve been analyzing my research and creating new neurons in my head.',
6, 5, '2026-02-07T20:00:00+01:00', '2026-02-07T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Feb Week 2 (whole week overview, Feb 15)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('f6a7b8c9-d0e1-4fc0-a6b7-c8d9e0f1a2b3', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'This week I''ve worked a lot about networking and found out that my own code cannot be used in my working computer. I''ve cleaned my code from working PC and started from scratch on the new one. Also, I''m looking for Mac mini to have great progress on a normal machine.',
5, 5, '2026-02-15T20:00:00+01:00', '2026-02-15T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Feb Week 3 — Monday (Feb 16)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('a7b8c9d0-e1f2-4ad0-b7c8-d9e0f1a2b3c4', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'I tried to work, but I was too tired to do anything.',
4, 2, '2026-02-16T20:00:00+01:00', '2026-02-16T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Feb Week 3 — Tuesday (Feb 17)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('b8c9d0e1-f2a3-4be0-c8d9-e0f1a2b3c4d5', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'I''ve been working on switch from the device and learned about System Design. Have attended to the group meeting.',
6, 5, '2026-02-17T20:00:00+01:00', '2026-02-17T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Feb Week 3 — Wednesday (Feb 18)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('c9d0e1f2-a3b4-4cf0-d9e0-f1a2b3c4d5e6', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'Today I bought a Mac mini and am waiting for the delivery. Started working on new Linux setup while waiting for the PC.',
7, 6, '2026-02-18T20:00:00+01:00', '2026-02-18T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- Feb Week 4 (summary + conclusions, Feb 28)
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('d0e1f2a3-b4c5-4d00-e0f1-a2b3c4d5e6f7', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'All week I''ve been implementing new project MVP and attended on all meetings. Also reflected on my progress and started going to the gym once again.',
7, 7, '2026-02-28T20:00:00+01:00', '2026-02-28T20:00:00+01:00')
ON CONFLICT DO NOTHING;

-- February Conclusion
INSERT INTO log_entries (id, user_id, type, content, mood, energy, logged_at, created_at) VALUES
('e1f2a3b4-c5d6-4e10-f1a2-b3c4d5e6f7a8', 'c22b1348-6b1e-4077-8c1d-0c3165956e76', 'journal',
'FEBRUARY CONCLUSION: This month was quite productive. I have implemented basic prosto-learn platform and started creating progress-analyzer application. Also I''ve been creating more tools for myself so now I want to clear them a little bit and start from the beginning.',
7, 6, '2026-02-28T23:00:00+01:00', '2026-02-28T23:00:00+01:00')
ON CONFLICT DO NOTHING;

-- ======================== ENTRY_GOALS ========================
-- Link entries to relevant goals based on content

INSERT INTO entry_goals (entry_id, goal_id) VALUES
  -- Dec entries → Progress App
  ('984120c3-06e5-4474-bb83-a20ecbf15398', 'a423677c-0e0d-4099-949f-f32668520eb1'),  -- conference, created analyzer
  -- Jan entries → various goals
  ('f8fead1c-69f6-42f0-878b-880a5ec67e09', '544039d3-548a-4e7b-8930-97c6c3086691'),  -- AI/ML call
  ('f8fead1c-69f6-42f0-878b-880a5ec67e09', 'd0f6e640-981e-4bc2-bf5c-ce00e44d2dec'),  -- sys design meetups
  ('1c5e82eb-75c7-47c4-862c-d28cf349b754', '544039d3-548a-4e7b-8930-97c6c3086691'),  -- AI agents, RAG
  ('950cd7d0-69cf-4b73-9899-93fa887836ee', '0efe3837-44fe-497c-810e-27791fcf7930'),  -- salary reflection
  ('27a8e2a9-7920-4194-b60b-598a66f4e8c5', 'd0f6e640-981e-4bc2-bf5c-ce00e44d2dec'),  -- sys design topics
  ('5fafaf1a-c17d-4fda-94ab-5a6a68905550', 'd0f6e640-981e-4bc2-bf5c-ce00e44d2dec'),  -- sys design prep
  ('8ce09e17-cf80-41e5-9166-972cce535d96', 'd0f6e640-981e-4bc2-bf5c-ce00e44d2dec'),  -- sys design meeting
  ('e29902dc-dcdc-49df-9461-5a59f4b09430', '52047c38-962a-423d-bb80-831a494cb188'),  -- english class
  ('a285ff46-9314-458e-b63b-dc73914cc174', '0efe3837-44fe-497c-810e-27791fcf7930'),  -- productive work → salary
  ('ecdb1390-20f2-4897-8266-cae34588a864', '0efe3837-44fe-497c-810e-27791fcf7930'),  -- Java upgrade → salary
  ('5ca5e385-114e-4349-8356-7af8a01bb8d5', '0efe3837-44fe-497c-810e-27791fcf7930'),  -- salary request
  ('9271f6cd-1211-48ac-a962-89363e04f540', 'd0f6e640-981e-4bc2-bf5c-ce00e44d2dec'),  -- sys design lesson
  ('9271f6cd-1211-48ac-a962-89363e04f540', '1c583ca5-a44b-425b-af78-b5733f28fbbd'),  -- walking/exercise
  ('9ff71b57-874f-4b06-b375-d4549f6825a5', '544039d3-548a-4e7b-8930-97c6c3086691'),  -- Claude Code, AI
  ('bec2abe5-4aad-4a20-8859-29d5d7526ea4', '544039d3-548a-4e7b-8930-97c6c3086691'),  -- iOS app, agent
  ('92bd719b-5bc2-4ec1-977e-602bfd60953d', 'a423677c-0e0d-4099-949f-f32668520eb1'),  -- prosto-learn plan
  ('e6a1b2c3-d4e5-4f60-a1b2-c3d4e5f60a1b', 'a423677c-0e0d-4099-949f-f32668520eb1'),  -- prosto-learn impl
  ('f7a2b3c4-d5e6-4f70-a2b3-c4d5e6f70a2b', 'a423677c-0e0d-4099-949f-f32668520eb1'),  -- prosto-learn MVP
  -- Feb entries
  ('c3d4e5f6-a7b8-4c90-d3e4-f5a6b7c8d9e0', 'd0f6e640-981e-4bc2-bf5c-ce00e44d2dec'),  -- sys design session
  ('b8c9d0e1-f2a3-4be0-c8d9-e0f1a2b3c4d5', 'd0f6e640-981e-4bc2-bf5c-ce00e44d2dec'),  -- sys design learning
  ('d0e1f2a3-b4c5-4d00-e0f1-a2b3c4d5e6f7', 'a423677c-0e0d-4099-949f-f32668520eb1'),  -- MVP implementation
  ('d0e1f2a3-b4c5-4d00-e0f1-a2b3c4d5e6f7', '1c583ca5-a44b-425b-af78-b5733f28fbbd')   -- gym
ON CONFLICT DO NOTHING;

-- ======================== ENTRY_TAGS ========================
-- Tag entries with relevant values

INSERT INTO entry_tags (entry_id, value_id) VALUES
  -- Dec Week 1 → Productivity, Creativity
  ('dd819f78-c8b5-4b97-8a56-92413ae1b1dd', '5e6debfa-ccb6-4cc4-af84-7b7c4721893f'),
  ('dd819f78-c8b5-4b97-8a56-92413ae1b1dd', '51307161-e86c-4479-8332-a7e8617a1ade'),
  -- Dec Week 2 → Productivity
  ('74a47fa7-efb6-4717-900e-21dca25d0ac5', '5e6debfa-ccb6-4cc4-af84-7b7c4721893f'),
  -- Dec Week 3 → Personal Branding, Creativity, Entrepreneurship
  ('984120c3-06e5-4474-bb83-a20ecbf15398', '6f41ed1c-2b3d-45d9-9f30-31cea8e0b870'),
  ('984120c3-06e5-4474-bb83-a20ecbf15398', '51307161-e86c-4479-8332-a7e8617a1ade'),
  ('984120c3-06e5-4474-bb83-a20ecbf15398', 'f7b031ed-2820-4cae-8591-e98e8deca363'),
  -- Dec W4 Monday → Mental Health, Self-Reflection, Productivity
  ('81254e18-9393-4f79-8a5b-c09494c3d85a', 'a21c0d83-c207-42ed-8c42-9b8361c346cc'),
  ('81254e18-9393-4f79-8a5b-c09494c3d85a', '471bc567-6d18-4cf4-8e0d-91927599d7c7'),
  ('81254e18-9393-4f79-8a5b-c09494c3d85a', '5e6debfa-ccb6-4cc4-af84-7b7c4721893f'),
  -- Dec W4 Tuesday → Rest
  ('74f08840-77fc-4836-b723-4ace492ea5ed', 'ec45499d-d4b2-4355-9efe-7be6be23f9da'),
  -- Dec W4 Friday → Personal Branding
  ('85a9186b-0c2a-48e2-aa0f-3be6bb1af7da', '6f41ed1c-2b3d-45d9-9f30-31cea8e0b870'),
  -- Dec W5 therapy → Mental Health, Rest
  ('7c7a3981-9c29-4e91-904e-e03e0ab6574d', 'a21c0d83-c207-42ed-8c42-9b8361c346cc'),
  ('7c7a3981-9c29-4e91-904e-e03e0ab6574d', 'ec45499d-d4b2-4355-9efe-7be6be23f9da'),
  -- Jan W2 Saturday → System Design, Self-Reflection, Rest
  ('27a8e2a9-7920-4194-b60b-598a66f4e8c5', '4df5a7b9-77ee-4bf8-bba9-b839e1874ca7'),
  ('27a8e2a9-7920-4194-b60b-598a66f4e8c5', '471bc567-6d18-4cf4-8e0d-91927599d7c7'),
  ('27a8e2a9-7920-4194-b60b-598a66f4e8c5', 'ec45499d-d4b2-4355-9efe-7be6be23f9da'),
  -- Jan W2 Wednesday → AI/ML
  ('1c5e82eb-75c7-47c4-862c-d28cf349b754', '6a0d85fe-8df2-4804-848d-44b3763ff81f'),
  -- Jan W2 Thursday → Salary reflection → Improvement
  ('950cd7d0-69cf-4b73-9899-93fa887836ee', '2ddd6ece-6d3a-4a1f-98f7-38824ef2d61f'),
  -- Jan W2 Friday → Social, Personal Branding
  ('25cc1b97-f283-4281-a4cb-57d2aa5abcf9', '1c918f1f-6ff7-409b-be0e-2bf1db61cbe5'),
  ('25cc1b97-f283-4281-a4cb-57d2aa5abcf9', '6f41ed1c-2b3d-45d9-9f30-31cea8e0b870'),
  -- Jan W3 Tuesday → System Design (sick day)
  ('8ce09e17-cf80-41e5-9166-972cce535d96', '4df5a7b9-77ee-4bf8-bba9-b839e1874ca7'),
  -- Jan W3 Thursday → Algorithms
  ('e29902dc-dcdc-49df-9461-5a59f4b09430', '1e331660-c1e1-4052-8256-9b82aed6063f'),
  -- Jan W4 Monday → Productivity, Improvement
  ('a285ff46-9314-458e-b63b-dc73914cc174', '5e6debfa-ccb6-4cc4-af84-7b7c4721893f'),
  ('a285ff46-9314-458e-b63b-dc73914cc174', '2ddd6ece-6d3a-4a1f-98f7-38824ef2d61f'),
  -- Jan W4 Tuesday → Productivity
  ('ecdb1390-20f2-4897-8266-cae34588a864', '5e6debfa-ccb6-4cc4-af84-7b7c4721893f'),
  -- Jan W4 Saturday → Salary
  ('5ca5e385-114e-4349-8356-7af8a01bb8d5', '2ddd6ece-6d3a-4a1f-98f7-38824ef2d61f'),
  -- Jan W5 Tuesday → Entrepreneurship, Exercise
  ('9271f6cd-1211-48ac-a962-89363e04f540', 'f7b031ed-2820-4cae-8591-e98e8deca363'),
  ('9271f6cd-1211-48ac-a962-89363e04f540', '3e36b257-fc43-4553-9fd8-da7dc07fa627'),
  -- Jan W5 Wednesday → AI/ML, Creativity
  ('9ff71b57-874f-4b06-b375-d4549f6825a5', '6a0d85fe-8df2-4804-848d-44b3763ff81f'),
  ('9ff71b57-874f-4b06-b375-d4549f6825a5', '51307161-e86c-4479-8332-a7e8617a1ade'),
  -- Jan W5 Thursday → Entrepreneurship, AI/ML
  ('bec2abe5-4aad-4a20-8859-29d5d7526ea4', 'f7b031ed-2820-4cae-8591-e98e8deca363'),
  ('bec2abe5-4aad-4a20-8859-29d5d7526ea4', '6a0d85fe-8df2-4804-848d-44b3763ff81f'),
  -- Jan W5 Friday → Algorithms, Creativity
  ('92bd719b-5bc2-4ec1-977e-602bfd60953d', '1e331660-c1e1-4052-8256-9b82aed6063f'),
  ('92bd719b-5bc2-4ec1-977e-602bfd60953d', '51307161-e86c-4479-8332-a7e8617a1ade'),
  -- Jan W5 Saturday → Entrepreneurship
  ('e6a1b2c3-d4e5-4f60-a1b2-c3d4e5f60a1b', 'f7b031ed-2820-4cae-8591-e98e8deca363'),
  -- Jan W5 Sunday → Entrepreneurship, Self-Reflection
  ('f7a2b3c4-d5e6-4f70-a2b3-c4d5e6f70a2b', 'f7b031ed-2820-4cae-8591-e98e8deca363'),
  ('f7a2b3c4-d5e6-4f70-a2b3-c4d5e6f70a2b', '471bc567-6d18-4cf4-8e0d-91927599d7c7'),
  -- Feb W1 Tuesday → Mental Health, System Design
  ('c3d4e5f6-a7b8-4c90-d3e4-f5a6b7c8d9e0', 'a21c0d83-c207-42ed-8c42-9b8361c346cc'),
  ('c3d4e5f6-a7b8-4c90-d3e4-f5a6b7c8d9e0', '4df5a7b9-77ee-4bf8-bba9-b839e1874ca7'),
  -- Feb W1 Wednesday → Self-Reflection
  ('d4e5f6a7-b8c9-4da0-e4f5-a6b7c8d9e0f1', '471bc567-6d18-4cf4-8e0d-91927599d7c7'),
  ('d4e5f6a7-b8c9-4da0-e4f5-a6b7c8d9e0f1', '2ddd6ece-6d3a-4a1f-98f7-38824ef2d61f'),
  -- Feb W3 Monday → Rest (tired)
  ('a7b8c9d0-e1f2-4ad0-b7c8-d9e0f1a2b3c4', 'ec45499d-d4b2-4355-9efe-7be6be23f9da'),
  -- Feb W4 → Exercise, Entrepreneurship
  ('d0e1f2a3-b4c5-4d00-e0f1-a2b3c4d5e6f7', '3e36b257-fc43-4553-9fd8-da7dc07fa627'),
  ('d0e1f2a3-b4c5-4d00-e0f1-a2b3c4d5e6f7', 'f7b031ed-2820-4cae-8591-e98e8deca363')
ON CONFLICT DO NOTHING;

COMMIT;
