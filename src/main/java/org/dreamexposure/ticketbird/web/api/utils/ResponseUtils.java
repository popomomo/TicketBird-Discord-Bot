package org.dreamexposure.ticketbird.web.api.utils;

public class ResponseUtils {
    public static String getJsonResponseMessage(String message) {
        return "{\"Message\": \"" + message + "\"";
    }
}