package im.turms.interaction.common.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一响应模板
 */
public class ResponseTemplate {
    
    public static ResponseEntity<Object> ok(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "Success");
        response.put("data", data);
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
    
    public static ResponseEntity<Object> error(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 500);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    public static ResponseEntity<Object> error(int code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.valueOf(code)).body(response);
    }
    
    public static ResponseEntity<Object> badRequest(String message) {
        return error(400, message);
    }
    
    public static ResponseEntity<Object> notFound(String message) {
        return error(404, message);
    }
    
    public static ResponseEntity<Object> forbidden(String message) {
        return error(403, message);
    }
}