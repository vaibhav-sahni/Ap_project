package edu.univ.erp.api.common;

import edu.univ.erp.api.ClientRequest;

/**
 * Common API for maintenance mode checking that can be used by all dashboards
 */
public class CommonAPI {

    /**
     * Check if maintenance mode is currently active
     *
     * @return true if maintenance mode is ON, false otherwise
     * @throws Exception if there's an error communicating with the server
     */
    public boolean checkMaintenanceMode() throws Exception {
        String response = ClientRequest.send("CHECK_MAINTENANCE");
        if (response.startsWith("SUCCESS:")) {
            String payload = response.substring("SUCCESS:".length()).trim();
            // Server returns SUCCESS:ON or SUCCESS:OFF
            return "ON".equalsIgnoreCase(payload) || "TRUE".equalsIgnoreCase(payload);
        }
        if (response.startsWith("ERROR:")) {
            throw new Exception(response.substring("ERROR:".length()));
        }
        // Fallback: assume maintenance mode is off if response is unclear
        return false;
    }
}
