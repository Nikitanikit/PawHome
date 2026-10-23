-- ============================================================
-- PawHome Pet Adoption Platform - Relational Database Schema
-- Engine: H2 (file-based). Portable to MySQL by swapping the
-- datasource in application.properties and AUTO_INCREMENT syntax.
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name       VARCHAR(150) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    phone           VARCHAR(30)  NOT NULL,
    address         VARCHAR(300) NOT NULL,
    password_hash   VARCHAR(200) NOT NULL,
    created_at      VARCHAR(30)  NOT NULL
);

CREATE TABLE IF NOT EXISTS admins (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(80) NOT NULL UNIQUE,
    password_hash   VARCHAR(200) NOT NULL,
    full_name       VARCHAR(150) NOT NULL,
    created_at      VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS pets (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                     VARCHAR(100) NOT NULL,
    species                  VARCHAR(50)  NOT NULL,
    breed                    VARCHAR(100) NOT NULL,
    age_years                DOUBLE       NOT NULL,
    gender                   VARCHAR(10)  NOT NULL CHECK (gender IN ('Male','Female')),
    color                    VARCHAR(60)  NOT NULL,
    location                 VARCHAR(120) NOT NULL,
    vaccination_status       VARCHAR(60)  NOT NULL,
    health_status            VARCHAR(60)  NOT NULL,
    description              VARCHAR(2000) NOT NULL,
    personality              VARCHAR(500) NOT NULL,
    adoption_requirements     VARCHAR(500) NOT NULL,
    adoption_fee             DOUBLE       NOT NULL DEFAULT 0,
    image_url                VARCHAR(500) NOT NULL,
    status                   VARCHAR(20)  NOT NULL DEFAULT 'available'
                             CHECK (status IN ('available','pending','adopted')),
    created_at               VARCHAR(30)  NOT NULL
);

CREATE TABLE IF NOT EXISTS favorites (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    pet_id      BIGINT NOT NULL,
    created_at  VARCHAR(30) NOT NULL,
    UNIQUE (user_id, pet_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (pet_id)  REFERENCES pets(id)  ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS cart_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    pet_id      BIGINT NOT NULL,
    added_at    VARCHAR(30) NOT NULL,
    UNIQUE (user_id, pet_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (pet_id)  REFERENCES pets(id)  ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS payments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    pet_id          BIGINT NOT NULL,
    amount          DOUBLE NOT NULL,
    payment_method  VARCHAR(30) NOT NULL,
    transaction_id  VARCHAR(50) NOT NULL UNIQUE,
    status          VARCHAR(20) NOT NULL DEFAULT 'success' CHECK (status IN ('success','failed')),
    created_at      VARCHAR(30) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (pet_id)  REFERENCES pets(id)  ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS adoption_applications (
    id                           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                      BIGINT NOT NULL,
    pet_id                       BIGINT NOT NULL,
    payment_id                  BIGINT,
    full_name                   VARCHAR(150) NOT NULL,
    email                        VARCHAR(150) NOT NULL,
    phone                        VARCHAR(30)  NOT NULL,
    address                      VARCHAR(300) NOT NULL,
    housing_type                 VARCHAR(60)  NOT NULL,
    previous_pet_experience       VARCHAR(1000) NOT NULL,
    reason_for_adoption           VARCHAR(1000) NOT NULL,
    family_members                INT NOT NULL,
    employment_info                VARCHAR(300) NOT NULL,
    agreement                    BOOLEAN NOT NULL DEFAULT FALSE,
    status                       VARCHAR(20) NOT NULL DEFAULT 'pending'
                                  CHECK (status IN ('pending','approved','rejected')),
    admin_note                   VARCHAR(500),
    applied_at                   VARCHAR(30) NOT NULL,
    reviewed_at                  VARCHAR(30),
    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
    FOREIGN KEY (pet_id)     REFERENCES pets(id)     ON DELETE CASCADE,
    FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_pets_status ON pets(status);
CREATE INDEX IF NOT EXISTS idx_pets_species ON pets(species);
CREATE INDEX IF NOT EXISTS idx_apps_status ON adoption_applications(status);
CREATE INDEX IF NOT EXISTS idx_apps_pet ON adoption_applications(pet_id);
