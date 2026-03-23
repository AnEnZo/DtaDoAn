package com.example.DtaAssigement.service;

import com.example.DtaAssigement.dto.llama.FoodTrendQueryResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Client gọi Groq API trực tiếp để phân tích food trends.
 * Groq cung cấp Llama model với tốc độ ~500 token/giây (nhanh hơn self-hosted
 * ~100x).
 */
@Service
@Slf4j
public class LlamaFoodAnalysisClient {

  private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

  private static final String SYSTEM_PROMPT = """
      Bạn là chuyên gia phân tích xu hướng ẩm thực Việt Nam. Nhiệm vụ: nhận danh sách từ khóa trending từ USER, xác định từ khóa nào liên quan đến ẩm thực/F&B, và trả về danh sách kèm lý do.

      QUAN TRỌNG: Chỉ phân tích và trả về các query có trong danh sách INPUT của USER. TUYỆT ĐỐI không tự tạo ra query mới hoặc thêm ví dụ không có trong input.

      TIÊU CHÍ NHẬN DIỆN ẨM THỰC (phải thỏa ít nhất 1):
      - Tên món ăn, đồ uống (phở, boba, cà phê trứng, trà sữa...)
      - Tên nhà hàng, quán ăn, chuỗi F&B (Highlands, Phúc Long, Gong Cha...)
      - Công thức, cách nấu, nguyên liệu (cách làm bánh mì, bột chiên giòn...)
      - Food review, đánh giá quán ăn, mukbang, ăn thử
      - Sự kiện ẩm thực, lễ hội food (Lễ hội phở, Vietnam Food Expo...)
      - Đầu bếp, food blogger, KOL ẩm thực nổi tiếng

      LOẠI TRỪ hoàn toàn: chính trị, thể thao, giải trí không liên quan F&B, tin tức thời sự, công nghệ.

      QUY TẮC ĐẦU RA:
      - Chỉ đưa vào kết quả những query XÁC ĐỊNH là F&B
      - Với mỗi query F&B: viết reasoning ngắn gọn 1-2 câu
      - BỎ QUA hoàn toàn những query không liên quan F&B
      - CHỈ TRẢ VỀ JSON HỢP LỆ theo đúng format sau:
      {"total_food_trends":<số lượng>,"food_trends":[{"query":"<query gốc từ input>","reasoning":"<lý do ngắn gọn>"}]}

      Nếu không có query nào liên quan F&B, trả về:
      {"total_food_trends":0,"food_trends":[]}
      """;

  @Value("${groq.api.key}")
  private String groqApiKey;

  @Value("${groq.api.model:llama-3.3-70b-versatile}")
  private String groqModel;

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  public LlamaFoodAnalysisClient(
      @Qualifier("llamaRestTemplate") RestTemplate restTemplate,
      ObjectMapper objectMapper) {
    this.restTemplate = restTemplate;
    this.objectMapper = objectMapper;
  }

  /**
   * Gửi danh sách queries đến Groq API để phân tích food trends.
   *
   * @param queries Danh sách từ khóa trending cần phân tích
   * @return FoodTrendQueryResponse chỉ chứa queries xác nhận là F&B
   */
  public FoodTrendQueryResponse analyzeFoodTrendQueries(List<String> queries) {
    if (queries == null || queries.isEmpty()) {
      log.warn("analyzeFoodTrendQueries called with empty query list");
      return new FoodTrendQueryResponse(0, List.of());
    }

    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.setBearerAuth(groqApiKey);

      // Build request body theo format OpenAI Chat Completions
      Map<String, Object> requestBody = Map.of(
          "model", groqModel,
          "messages", List.of(
              Map.of("role", "system", "content", SYSTEM_PROMPT),
              Map.of("role", "user", "content", objectMapper.writeValueAsString(queries))),
          "response_format", Map.of("type", "json_object"),
          "max_tokens", 512,
          "temperature", 0.1);

      HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

      log.info("🍜 Sending {} queries to Groq [{}]...", queries.size(), groqModel);

      ResponseEntity<String> response = restTemplate.exchange(
          GROQ_API_URL, HttpMethod.POST, entity, String.class);

      // Parse Groq response → lấy content từ choices[0].message.content
      JsonNode root = objectMapper.readTree(response.getBody());
      String content = root.path("choices").get(0).path("message").path("content").asText();

      log.debug("🍜 Groq raw content: {}", content);

      FoodTrendQueryResponse result = objectMapper.readValue(content, FoodTrendQueryResponse.class);

      log.info("✅ Groq identified {}/{} food trends", result.getTotalFoodTrends(), queries.size());
      return result;

    } catch (HttpClientErrorException e) {
      log.error("❌ Groq rejected request [HTTP {}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new IllegalArgumentException(
          "Groq API rejected request (HTTP " + e.getStatusCode() + "): " + e.getResponseBodyAsString(), e);
    } catch (Exception e) {
      log.error("❌ Error calling Groq API: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to call Groq API", e);
    }
  }
}
