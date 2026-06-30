-- Script d'initialisation des bases de données PostgreSQL pour YowYob
-- Ce script crée toutes les bases de données nécessaires aux microservices

-- Création de la base de données pour le service d'authentification
CREATE DATABASE yowyob_auth;

-- Création de la base de données pour le service de listings
CREATE DATABASE yowyob_listings;

-- Création de la base de données pour le service utilisateur
CREATE DATABASE yowyob_users;

-- Affichage des bases de données créées
\l
