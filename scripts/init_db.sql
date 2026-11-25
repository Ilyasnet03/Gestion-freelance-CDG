-- ====================================================================
-- PROJET DE STAGE : Plateforme de gestion des freelances - CDG BEP
-- Auteur : Ilyas BENLOUALI
-- Script d'initialisation de la base de données PostgreSQL 15
-- ====================================================================

DROP TABLE IF EXISTS paiement CASCADE;
DROP TABLE IF EXISTS pv_reception CASCADE;
DROP TABLE IF EXISTS ligne_timesheet CASCADE;
DROP TABLE IF EXISTS timesheet CASCADE;
DROP TABLE IF EXISTS tache CASCADE;
DROP TABLE IF EXISTS affectation CASCADE;
DROP TABLE IF EXISTS alerte CASCADE;
DROP TABLE IF EXISTS projet CASCADE;
DROP TABLE IF EXISTS type_projet CASCADE;
DROP TABLE IF EXISTS freelance CASCADE;
DROP TABLE IF EXISTS utilisateur CASCADE;
DROP TABLE IF EXISTS parametre CASCADE;

-- 1. Table Utilisateur
CREATE TABLE utilisateur (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    mot_de_passe VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL CHECK (role IN ('ADMIN', 'CHEF_PROJET', 'FREELANCE')),
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

-- 2. Table Freelance
CREATE TABLE freelance (
    id BIGSERIAL PRIMARY KEY,
    utilisateur_id BIGINT NOT NULL UNIQUE REFERENCES utilisateur(id) ON DELETE CASCADE,
    tjm NUMERIC(10,2) NOT NULL CHECK (tjm > 0),
    specialite VARCHAR(100),
    statut VARCHAR(30) NOT NULL DEFAULT 'ACTIF',
    seuil_heures_jour NUMERIC(4,2) DEFAULT 8.00 CHECK (seuil_heures_jour > 0 AND seuil_heures_jour <= 10)
);

-- 3. Table Type de Projet
CREATE TABLE type_projet (
    id BIGSERIAL PRIMARY KEY,
    libelle VARCHAR(100) NOT NULL UNIQUE
);

-- 4. Table Projet
CREATE TABLE projet (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    nom VARCHAR(150) NOT NULL,
    description TEXT,
    budget NUMERIC(12,2) NOT NULL CHECK (budget > 0),
    heures_prevues NUMERIC(10,2) CHECK (heures_prevues > 0),
    nombre_taches_prevues INTEGER CHECK (nombre_taches_prevues > 0),
    date_debut DATE NOT NULL,
    date_fin_prevue DATE NOT NULL,
    statut VARCHAR(30) NOT NULL DEFAULT 'PLANIFIE' CHECK (statut IN ('PLANIFIE', 'EN_COURS', 'SUSPENDU', 'TERMINE')),
    type_projet_id BIGINT NOT NULL REFERENCES type_projet(id),
    chef_projet_id BIGINT NOT NULL REFERENCES utilisateur(id)
);

-- 5. Table Affectation
CREATE TABLE affectation (
    id BIGSERIAL PRIMARY KEY,
    projet_id BIGINT NOT NULL REFERENCES projet(id) ON DELETE CASCADE,
    freelance_id BIGINT NOT NULL REFERENCES freelance(id) ON DELETE CASCADE,
    tjm_applique NUMERIC(10,2) NOT NULL CHECK (tjm_applique > 0),
    date_debut DATE NOT NULL,
    date_fin DATE,
    CONSTRAINT uq_affectation_projet_freelance UNIQUE (projet_id, freelance_id)
);

-- 6. Table Tâche
CREATE TABLE tache (
    id BIGSERIAL PRIMARY KEY,
    projet_id BIGINT NOT NULL REFERENCES projet(id) ON DELETE CASCADE,
    affectation_id BIGINT REFERENCES affectation(id) ON DELETE SET NULL,
    titre VARCHAR(200) NOT NULL,
    charge_estimee NUMERIC(6,1) NOT NULL DEFAULT 0 CHECK (charge_estimee >= 0),
    heures_prevues NUMERIC(10,2) CHECK (heures_prevues > 0),
    avancement INT NOT NULL DEFAULT 0 CHECK (avancement BETWEEN 0 AND 100),
    statut VARCHAR(30) NOT NULL DEFAULT 'A_FAIRE' CHECK (statut IN ('A_FAIRE', 'EN_COURS', 'TERMINEE')),
    date_debut DATE,
    date_fin DATE
);

-- 7. Table Timesheet
CREATE TABLE timesheet (
    id BIGSERIAL PRIMARY KEY,
    freelance_id BIGINT NOT NULL REFERENCES freelance(id) ON DELETE CASCADE,
    semaine_debut DATE NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'BROUILLON' CHECK (statut IN ('BROUILLON', 'SOUMISE', 'VALIDEE', 'REJETEE')),
    motif_rejet TEXT,
    date_soumission TIMESTAMP,
    date_decision TIMESTAMP,
    valide_par BIGINT REFERENCES utilisateur(id),
    CONSTRAINT uq_timesheet_semaine UNIQUE (freelance_id, semaine_debut)
);

-- 8. Table Ligne de Timesheet
CREATE TABLE ligne_timesheet (
    id BIGSERIAL PRIMARY KEY,
    timesheet_id BIGINT NOT NULL REFERENCES timesheet(id) ON DELETE CASCADE,
    tache_id BIGINT NOT NULL REFERENCES tache(id) ON DELETE RESTRICT,
    date_saisie DATE NOT NULL,
    heures NUMERIC(4,2) NOT NULL CHECK (heures > 0 AND heures <= 24),
    CONSTRAINT uq_ligne UNIQUE (timesheet_id, tache_id, date_saisie)
);

-- 9. Table PV de Réception
CREATE TABLE pv_reception (
    id BIGSERIAL PRIMARY KEY,
    timesheet_id BIGINT NOT NULL UNIQUE REFERENCES timesheet(id) ON DELETE CASCADE,
    numero VARCHAR(50) NOT NULL UNIQUE,
    total_heures NUMERIC(6,2) NOT NULL CHECK (total_heures >= 0),
    montant NUMERIC(12,2) NOT NULL CHECK (montant >= 0),
    chemin_pdf VARCHAR(255),
    date_generation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 10. Table Paiement
CREATE TABLE paiement (
    id BIGSERIAL PRIMARY KEY,
    pv_id BIGINT NOT NULL REFERENCES pv_reception(id) ON DELETE CASCADE,
    statut VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE' CHECK (statut IN ('EN_ATTENTE', 'PAYE')),
    date_paiement DATE,
    reference VARCHAR(100)
);

-- 11. Table Alerte (générée par FastAPI)
CREATE TABLE alerte (
    id BIGSERIAL PRIMARY KEY,
    projet_id BIGINT NOT NULL REFERENCES projet(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL CHECK (type IN ('DEPASSEMENT_BUDGET', 'RETARD_PROJET', 'TIMESHEET_EN_RETARD', 'ANOMALIE_SAISIE', 'SURCHARGE_FREELANCE')),
    niveau VARCHAR(20) NOT NULL CHECK (niveau IN ('INFO', 'ATTENTION', 'CRITIQUE')),
    message TEXT NOT NULL,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    traitee BOOLEAN NOT NULL DEFAULT FALSE
);

-- 12. Table Paramètres
CREATE TABLE parametre (
    cle VARCHAR(100) PRIMARY KEY,
    valeur VARCHAR(255) NOT NULL
);

-- Index de performance (section 2.3 et 5.1 du cahier des charges)
CREATE INDEX idx_alerte_projet_traitee ON alerte(projet_id, traitee);
CREATE INDEX idx_timesheet_freelance ON timesheet(freelance_id);
CREATE INDEX idx_timesheet_semaine ON timesheet(semaine_debut);
CREATE INDEX idx_ligne_timesheet_date ON ligne_timesheet(date_saisie);
CREATE INDEX idx_tache_projet ON tache(projet_id);
CREATE INDEX idx_affectation_projet ON affectation(projet_id);
CREATE INDEX idx_affectation_freelance ON affectation(freelance_id);

-- ====================================================================
-- Données initiales par défaut (Admin, Types de projet, Paramètres)
-- Mot de passe haché par défaut pour tous : 'password123'
-- Hash BCrypt ($2a$10$e8w8GkGf3lO43X3iJkLgDu6WjN4h/p0n74rQkYpZ9qg4U4M2rW5z2)
-- ====================================================================

-- Paramètres système
INSERT INTO parametre (cle, valeur) VALUES 
('TJM_DEFAUT', '4500'),
('SEUIL_BUDGET_ATTENTION', '80'),
('SEUIL_BUDGET_CRITIQUE', '100'),
('SEUIL_RETARD_ATTENTION', '15'),
('SEUIL_RETARD_CRITIQUE', '30'),
('DELAI_VALIDATION_JOURS', '3'),
('MAX_HEURES_JOUR_INFO', '10'),
('MAX_HEURES_SEMAINE_INFO', '48');

-- Types de projets CDG
INSERT INTO type_projet (libelle) VALUES 
('Digital Factory - Web / Mobile'),
('Système d''Information Épargne'),
('Modernisation Prévoyance'),
('Infrastructure Cloud & Data'),
('Cybersécurité & Conformité');

-- Utilisateurs initiaux (mot de passe: password123)
-- Hash BCrypt officiel pour 'password123': $2a$10$wT0XkQ8ZJb8U5jNvyh59e.08Edfn6sU2c9Nvy7E3p5fGkX3vL39eG
INSERT INTO utilisateur (nom, prenom, email, mot_de_passe, role, actif) VALUES 
('BENLOUALI', 'Ilyas', 'admin@cdg.ma', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', TRUE),
('EL ALAMI', 'Karim', 'cdp.alami@cdg.ma', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CHEF_PROJET', TRUE),
('BENNANI', 'Sofia', 'cdp.bennani@cdg.ma', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CHEF_PROJET', TRUE),
('TAZI', 'Mehdi', 'mehdi.tazi@freelance.cdg.ma', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'FREELANCE', TRUE),
('CHRAIBI', 'Youssef', 'youssef.chraibi@freelance.cdg.ma', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'FREELANCE', TRUE),
('IDRISSI', 'Fatima', 'fatima.idrissi@freelance.cdg.ma', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'FREELANCE', TRUE);

-- Profils Freelance
INSERT INTO freelance (utilisateur_id, tjm, specialite, statut) VALUES 
(4, 5000.00, 'Développeur Fullstack Angular / Spring Boot', 'ACTIF'),
(5, 4500.00, 'Ingénieur Data & Python FastAPI', 'ACTIF'),
(6, 4800.00, 'Expert DevOps & Cloud Docker', 'ACTIF');
