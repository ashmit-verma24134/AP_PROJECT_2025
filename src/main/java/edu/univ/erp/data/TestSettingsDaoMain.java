package edu.univ.erp.data;

import edu.univ.erp.util.DBConnection;

import java.sql.Connection;

/**
 * Simple smoke-test runner to check SettingsDaoImpl read + write.
 * Place this under src/main/java so you can run it quickly from the IDE.
 */
public class TestSettingsDaoMain {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getErpConnection()) {
            SettingsDaoImpl dao = new SettingsDaoImpl(conn);

            System.out.println("Initial maintenance: " + dao.isMaintenanceOn());

            System.out.println("Turning maintenance ON...");
            boolean setOn = dao.setMaintenance(true);
            System.out.println("setMaintenance returned: " + setOn);
            System.out.println("Now maintenance: " + dao.isMaintenanceOn());

            System.out.println("Turning maintenance OFF...");
            boolean setOff = dao.setMaintenance(false);
            System.out.println("setMaintenance returned: " + setOff);
            System.out.println("Now maintenance: " + dao.isMaintenanceOn());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
