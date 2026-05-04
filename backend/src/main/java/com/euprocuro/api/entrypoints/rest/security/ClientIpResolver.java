package com.euprocuro.api.entrypoints.rest.security;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ClientIpResolver {

    private static final String[] IP_HEADERS = {
            "CF-Connecting-IP",
            "True-Client-IP",
            "X-Real-IP",
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
    };

    private final InetAddressValidator validator = InetAddressValidator.getInstance();

    public String resolve(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String value = request.getHeader(header);

            if (!StringUtils.hasText(value) || "unknown".equalsIgnoreCase(value.trim())) {
                continue;
            }

            String[] ips = value.split(",");

            for (String ip : ips) {
                String candidate = normalize(ip);

                if (isValidIp(candidate)) {
                    return candidate;
                }
            }
        }

        String remoteAddr = normalize(request.getRemoteAddr());

        if (isValidIp(remoteAddr)) {
            return remoteAddr;
        }

        return null;
    }

    private String normalize(String ip) {
        if (ip == null) {
            return null;
        }

        return ip.trim()
                .replace("\"", "")
                .replace("'", "");
    }

    private boolean isValidIp(String ip) {
        return StringUtils.hasText(ip)
                && !"unknown".equalsIgnoreCase(ip)
                && validator.isValid(ip);
    }
}