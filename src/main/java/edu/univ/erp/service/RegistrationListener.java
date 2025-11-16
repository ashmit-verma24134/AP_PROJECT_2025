package edu.univ.erp.service;

/**
 * Simple listener interface for registration (enroll/drop) changes.
 */
public interface RegistrationListener {
    /**
     * Called when a registration (enroll/drop) change occurs in the system.
     */
    void onRegistrationChanged();
}
