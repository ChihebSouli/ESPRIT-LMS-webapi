package com.esprit.lms.users.entity;

/**
 * LMS-specific roles mapped from auth MS JWT claims.
 * Auth MS roles: ETUDIANT, ENSEIGNANT, ADMIN, GESTION, etc.
 * LMS maps these to our simplified 4-role system.
 */
public enum LmsRole {
    STUDENT,
    TEACHER,
    LIBRARIAN,
    ADMIN
}
