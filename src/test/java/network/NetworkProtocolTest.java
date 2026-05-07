package network;
import utils.JsonUtil;


import entity.Ban;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NetworkProtocolTest {

    @Test
    public void testRequestSerialization() {
        // 1. Prepare data
        Ban testBan = new Ban();
        testBan.setMaBan("B01");
        testBan.setViTri("Tang 1");
        
        Request request = new Request(CommandType.UPDATE_TABLE_STATUS);
        request.setData(testBan);
        request.setToken("test-token-123");
        request.setParam("reason", "Customer arrived");

        // 2. Serialize to JSON
        String json = JsonUtil.toJson(request);
        System.out.println("Serialized Request JSON: " + json);
        
        assertNotNull(json);
        assertTrue(json.contains("UPDATE_TABLE_STATUS"));
        assertTrue(json.contains("B01"));
        assertTrue(json.contains("test-token-123"));

        // 3. Deserialize back to Request
        Request deserialized = JsonUtil.fromJson(json, Request.class);
        
        assertEquals(CommandType.UPDATE_TABLE_STATUS, deserialized.getAction());
        assertEquals("test-token-123", deserialized.getToken());
        assertEquals("Customer arrived", deserialized.getParam("reason"));
        
        // Note: data is Object, Gson might deserialize it as a LinkedTreeMap if not specified
        // For unit test, we can convert it back to Ban
        String dataJson = JsonUtil.toJson(deserialized.getData());
        Ban deserializedBan = JsonUtil.fromJson(dataJson, Ban.class);
        
        assertEquals("B01", deserializedBan.getMaBan());
    }

    @Test
    public void testResponseSerialization() {
        // 1. Prepare data
        Response response = Response.ok("Task Completed");
        response.setStatusCode(200);

        // 2. Serialize
        String json = JsonUtil.toJson(response);
        System.out.println("Serialized Response JSON: " + json);

        // 3. Deserialize
        Response deserialized = JsonUtil.fromJson(json, Response.class);
        
        assertEquals(200, deserialized.getStatusCode());
        assertEquals("Success", deserialized.getMessage());
        assertEquals("Task Completed", deserialized.getData());
    }

    @Test
    public void testLocalDateTimeSerialization() {
        LocalDateTime now = LocalDateTime.of(2025, 5, 5, 10, 30);
        String json = JsonUtil.toJson(now);
        
        // The format depends on LocalDateTimeAdapter implementation
        // Let's just check if it can be deserialized back
        LocalDateTime deserialized = JsonUtil.fromJson(json, LocalDateTime.class);
        
        assertEquals(now, deserialized);
    }
    
    @Test
    public void testListSerialization() {
        List<String> items = List.of("Apple", "Banana", "Orange");
        String json = JsonUtil.toJson(items);
        
        List<String> deserialized = JsonUtil.fromJsonList(json, String.class);
        
        assertEquals(3, deserialized.size());
        assertEquals("Apple", deserialized.get(0));
    }
}
