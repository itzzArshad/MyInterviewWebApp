-- Lessons can now be authored in the app (paste → parse → save) and are mirrored to a GitHub repo.

ALTER TABLE concept ADD COLUMN source_md VARCHAR(1000000);            -- exactly what was pasted/imported (lesson + quiz block)
ALTER TABLE concept ADD COLUMN content_source VARCHAR(10) NOT NULL DEFAULT 'SEED';  -- SEED (bundled file) | USER (written in the app)
ALTER TABLE concept ADD COLUMN github_path VARCHAR(500);
ALTER TABLE concept ADD COLUMN github_sha VARCHAR(64);
ALTER TABLE concept ADD COLUMN github_synced_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE concept ADD COLUMN github_error VARCHAR(2000);
