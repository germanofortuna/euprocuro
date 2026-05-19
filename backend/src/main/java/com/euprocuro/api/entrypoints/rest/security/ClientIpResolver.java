package com.euprocuro.api.entrypoints.rest.security;

import java.net.InetAddress;
import java.net.UnknownHostException;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.validator.routines.InetAddressValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ClientIpResolver {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String[] SINGLE_VALUE_IP_HEADERS = {
            "CF-Connecting-IP",
            "True-Client-IP",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
    };

    private final InetAddressValidator validator = InetAddressValidator.getInstance();
    private final String trustedProxyRanges;

    public ClientIpResolver(@Value("${application.security.trusted-proxies:}") String trustedProxyRanges) {
        this.trustedProxyRanges = trustedProxyRanges == null ? "" : trustedProxyRanges;
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddr = normalize(request.getRemoteAddr());

        if (!isValidIp(remoteAddr)) {
            return null;
        }

        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

        String forwardedFor = resolveForwardedFor(request.getHeader(X_FORWARDED_FOR));
        if (forwardedFor != null) {
            return forwardedFor;
        }

        for (String header : SINGLE_VALUE_IP_HEADERS) {
            String headerIp = resolveSingleValueHeader(request.getHeader(header));
            if (headerIp != null) {
                return headerIp;
            }
        }

        return remoteAddr;
    }

    private String resolveForwardedFor(String headerValue) {
        if (!StringUtils.hasText(headerValue) || "unknown".equalsIgnoreCase(headerValue.trim())) {
            return null;
        }

        String[] ips = headerValue.split(",");

        for (int index = ips.length - 1; index >= 0; index--) {
            String candidate = normalize(ips[index]);

            if (isValidIp(candidate) && !isTrustedProxy(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private String resolveSingleValueHeader(String headerValue) {
        if (!StringUtils.hasText(headerValue) || "unknown".equalsIgnoreCase(headerValue.trim())) {
            return null;
        }

        String candidate = normalize(headerValue.split(",")[0]);
        if (isValidIp(candidate)) {
            return candidate;
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

    private boolean isTrustedProxy(String ip) {
        if (!StringUtils.hasText(trustedProxyRanges)) {
            return false;
        }

        for (String range : trustedProxyRanges.split(",")) {
            String candidate = range.trim();
            if (!StringUtils.hasText(candidate)) {
                continue;
            }

            if (candidate.contains("/")) {
                if (isInCidrRange(ip, candidate)) {
                    return true;
                }
            } else if (candidate.equals(ip)) {
                return true;
            }
        }

        return false;
    }

    private boolean isInCidrRange(String ip, String cidr) {
        String[] parts = cidr.split("/", 2);
        if (parts.length != 2 || !isValidIp(parts[0])) {
            return false;
        }

        try {
            byte[] address = InetAddress.getByName(ip).getAddress();
            byte[] network = InetAddress.getByName(parts[0]).getAddress();
            if (address.length != network.length) {
                return false;
            }

            int prefixLength = Integer.parseInt(parts[1]);
            if (prefixLength < 0 || prefixLength > address.length * Byte.SIZE) {
                return false;
            }

            int fullBytes = prefixLength / Byte.SIZE;
            int remainingBits = prefixLength % Byte.SIZE;

            for (int i = 0; i < fullBytes; i++) {
                if (address[i] != network[i]) {
                    return false;
                }
            }

            if (remainingBits == 0) {
                return true;
            }

            int mask = 0xFF << (Byte.SIZE - remainingBits);
            return (address[fullBytes] & mask) == (network[fullBytes] & mask);
        } catch (NumberFormatException | UnknownHostException ex) {
            return false;
        }
    }
}
