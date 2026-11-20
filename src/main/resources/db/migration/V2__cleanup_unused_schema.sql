ALTER TABLE course_spots DROP CONSTRAINT uk_course_spots_order;

ALTER TABLE feedbacks ADD COLUMN content_text text;
UPDATE feedbacks SET content_text = convert_from(lo_get(content), 'UTF8');
SELECT lo_unlink(content) FROM feedbacks;
ALTER TABLE feedbacks DROP COLUMN content;
ALTER TABLE feedbacks RENAME COLUMN content_text TO content;
ALTER TABLE feedbacks ALTER COLUMN content SET NOT NULL;

DROP TABLE sample;

DROP TABLE refresh_token;

ALTER TABLE pozings DROP COLUMN thumbnail_url;

ALTER TABLE tourist_spots
    DROP COLUMN area_code,
    DROP COLUMN sigungu_code;
