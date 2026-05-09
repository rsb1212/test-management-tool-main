package com.testmgmt.service;

import com.testmgmt.entity.Defect;
import com.testmgmt.exception.GlobalExceptionHandler.JiraIntegrationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JiraService {

    @Value("${app.jira.base-url}")
    private String jiraBaseUrl;

    @Value("${app.jira.username}")
    private String jiraUsername;

    @Value("${app.jira.api-token}")
    private String jiraApiToken;

    @Value("${app.jira.project-key}")
    private String jiraProjectKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public record JiraIssueResult(String issueKey, String issueUrl) {}

    public JiraIssueResult createIssue(Defect defect) {
        try {
            HttpHeaders headers = buildHeaders();

            Map<String, Object> body = Map.of(
                "fields", Map.of(
                    "project",     Map.of("key", jiraProjectKey),
                    "summary",     "[" + defect.getCode() + "] " + defect.getTitle(),
                    "description", Map.of(
                        "type",    "doc",
                        "version", 1,
                        "content", java.util.List.of(Map.of(
                            "type",    "paragraph",
                            "content", java.util.List.of(Map.of(
                                "type", "text",
                                "text", defect.getDescription() != null ? defect.getDescription() : ""
                            ))
                        ))
                    ),
                    "issuetype",   Map.of("name", "Bug"),
                    "priority",    Map.of("name", mapPriority(defect.getPriority())),
                    "labels",      java.util.List.of("test-mgmt", defect.getSeverity().name().toLowerCase())
                )
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                jiraBaseUrl + "/rest/api/3/issue",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String issueKey = (String) response.getBody().get("key");
                String issueUrl = jiraBaseUrl + "/browse/" + issueKey;
                log.info("Jira issue created: {} for defect {}", issueKey, defect.getCode());
                return new JiraIssueResult(issueKey, issueUrl);
            }

            throw new JiraIntegrationException("Jira returned unexpected status: " + response.getStatusCode());

        } catch (JiraIntegrationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Jira integration failed", e);
            throw new JiraIntegrationException("Jira unreachable: " + e.getMessage());
        }
    }

    private HttpHeaders buildHeaders() {
        String credentials = jiraUsername + ":" + jiraApiToken;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encoded);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String mapPriority(Defect.DefectPriority priority) {
        return switch (priority) {
            case P1 -> "Highest";
            case P2 -> "High";
            case P3 -> "Medium";
            case P4 -> "Low";
        };
    }
}
