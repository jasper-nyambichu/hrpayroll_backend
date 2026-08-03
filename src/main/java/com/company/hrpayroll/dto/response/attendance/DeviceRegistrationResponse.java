// dto/response/attendance/DeviceRegistrationResponse.java
package com.company.hrpayroll.dto.response.attendance;

// plaintextToken is shown exactly once, at registration time — it is never
// retrievable again, only the hash is stored.
public record DeviceRegistrationResponse(
        Long id,
        String deviceSerial,
        String plaintextToken
) {}