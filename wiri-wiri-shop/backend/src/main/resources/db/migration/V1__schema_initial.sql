-- Wiri-Wiri Shop : schéma initial (PostgreSQL)
-- Les UUID sont générés côté serveur (application) pour éviter la prédictibilité des IDs.

CREATE TABLE boutiques (
    id                   UUID          PRIMARY KEY,
    nom_vendeur          VARCHAR(100)  NOT NULL,
    telephone            VARCHAR(20)   NOT NULL,
    actif                BOOLEAN       NOT NULL DEFAULT TRUE,
    date_creation        TIMESTAMP     NOT NULL,
    -- Ajouts : traçabilité du consentement CDP et lien de paiement Wave du vendeur
    date_consentement    TIMESTAMP,
    lien_wave            VARCHAR(512),
    CONSTRAINT uk_boutiques_telephone UNIQUE (telephone)
);

CREATE TABLE produits (
    id             UUID          PRIMARY KEY,
    boutique_id    UUID          NOT NULL,
    nom_produit    VARCHAR(150)  NOT NULL,
    prix           INTEGER       NOT NULL,
    taille         VARCHAR(20),
    image_url      VARCHAR(512)  NOT NULL,
    audio_url      VARCHAR(512),
    actif          BOOLEAN       NOT NULL DEFAULT TRUE,
    date_creation  TIMESTAMP     NOT NULL,
    CONSTRAINT fk_produits_boutique FOREIGN KEY (boutique_id)
        REFERENCES boutiques (id) ON DELETE CASCADE,
    CONSTRAINT ck_produits_prix CHECK (prix > 0)
);

CREATE INDEX idx_produits_boutique_actif ON produits (boutique_id, actif);

CREATE TABLE commandes (
    id                  UUID          PRIMARY KEY,
    produit_id          UUID          NOT NULL,
    -- Chiffré AES-256-GCM (Base64) : la colonne est plus large que les 20 caractères du numéro en clair.
    telephone_client    VARCHAR(255)  NOT NULL,
    quartier_livraison  VARCHAR(100)  NOT NULL,
    date_commande       TIMESTAMP     NOT NULL,
    CONSTRAINT fk_commandes_produit FOREIGN KEY (produit_id)
        REFERENCES produits (id) ON DELETE CASCADE
);

CREATE INDEX idx_commandes_produit ON commandes (produit_id);

-- Codes OTP : seul un condensat HMAC-SHA256 est stocké, jamais le code en clair.
CREATE TABLE codes_otp (
    telephone        VARCHAR(20)  PRIMARY KEY,
    code_hash        VARCHAR(64)  NOT NULL,
    date_expiration  TIMESTAMP    NOT NULL,
    tentatives       INTEGER      NOT NULL DEFAULT 0
);
