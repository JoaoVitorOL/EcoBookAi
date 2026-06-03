package com.ecobook;

import com.ecobook.model.Material;
import com.ecobook.model.Solicitacao;
import com.ecobook.model.Usuario;
import com.ecobook.model.enums.Disciplina;
import com.ecobook.model.enums.EstadoConservacao;
import com.ecobook.model.enums.NivelEnsino;
import com.ecobook.model.enums.Role;
import com.ecobook.model.enums.SistemaEnsino;
import com.ecobook.model.enums.StatusMaterial;
import com.ecobook.model.enums.StatusSolicitacao;
import com.ecobook.repository.MaterialRepository;
import com.ecobook.repository.SolicitacaoRepository;
import com.ecobook.repository.UsuarioRepository;
import com.ecobook.scheduler.ReservationExpiryJob;
import com.ecobook.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class E2ETests extends BaseIntegrationTest {

    private static final String SEEDED_PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOHiPpclJ3DgA6hfa/vw/jemjL5V6cO9e";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private SolicitacaoRepository solicitacaoRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ReservationExpiryJob reservationExpiryJob;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("E2E 01 - Happy path should cover register, profile, upload, search, request, approve and complete")
    void shouldRunHappyPathFlow() throws Exception {
        RegisteredUser donor = register("e2e-happy-donor@example.com", "SenhaSegura123", "Doador Happy");
        RegisteredUser student = register("e2e-happy-student@example.com", "SenhaSegura123", "Estudante Happy");

        completeProfile(donor.token(), donor.nome(), "+5511991234501", "Florianopolis", "Centro", true);
        completeProfile(student.token(), student.nome(), "+5511991234502", "Florianopolis", "Trindade", true);

        String uploadId = previewUploadIdFor(donor.token(), validPng("happy"));
        String materialId = createMaterial(donor.token(), uploadId, "Colecao Happy", "MATEMATICA", "FUNDAMENTAL", 7, "ANGLO");

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + student.token())
                        .param("query", "happy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.results[0].id").value(materialId));

        String requestId = createRequest(student.token(), materialId);

        mockMvc.perform(patch("/v1/solicitacoes/{id}/aprovar", requestId)
                        .header("Authorization", "Bearer " + donor.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APROVADA"));

        mockMvc.perform(patch("/v1/solicitacoes/{id}/concluir", requestId)
                        .header("Authorization", "Bearer " + donor.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONCLUIDA"));
    }

    @Test
    @DisplayName("E2E 02 - Approval race guard should leave only one approved request")
    void shouldGuardApprovalRace() throws Exception {
        Usuario donor = createCompleteUser("e2e-race-donor@example.com", "Doador", true);
        Usuario firstStudent = createCompleteUser("e2e-race-student-1@example.com", "Primeiro", true);
        Usuario secondStudent = createCompleteUser("e2e-race-student-2@example.com", "Segundo", true);
        Material material = createMaterialEntity(donor, "Colecao corrida", NivelEnsino.FUNDAMENTAL, 8, SistemaEnsino.ANGLO);

        String firstRequestId = createRequest(tokenFor(firstStudent), material.getId().toString());
        String secondRequestId = createRequest(tokenFor(secondStudent), material.getId().toString());

        mockMvc.perform(patch("/v1/solicitacoes/{id}/aprovar", firstRequestId)
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APROVADA"));

        mockMvc.perform(patch("/v1/solicitacoes/{id}/aprovar", secondRequestId)
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("E2E 03 - Expiry should reopen a reserved material and allow a new request")
    void shouldExpireApprovedReservationAndAllowNewRequest() throws Exception {
        Usuario donor = createCompleteUser("e2e-expiry-donor@example.com", "Doador", true);
        Usuario firstStudent = createCompleteUser("e2e-expiry-student-1@example.com", "Primeiro", true);
        Usuario secondStudent = createCompleteUser("e2e-expiry-student-2@example.com", "Segundo", true);
        Material material = createMaterialEntity(donor, "Colecao expirada E2E", NivelEnsino.FUNDAMENTAL, 7, SistemaEnsino.ANGLO);
        material.setStatus(StatusMaterial.RESERVADO);
        materialRepository.saveAndFlush(material);

        Solicitacao approved = Solicitacao.builder()
                .material(material)
                .estudante(firstStudent)
                .status(StatusSolicitacao.APROVADA)
                .aprovadoEm(LocalDateTime.now().minusDays(20))
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        approved.populateContatoDoador(donor);
        solicitacaoRepository.saveAndFlush(approved);

        reservationExpiryJob.expireReservations();

        mockMvc.perform(post("/v1/materiais/{id}/solicitacoes", material.getId())
                        .header("Authorization", "Bearer " + tokenFor(secondStudent)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDENTE"));
    }

    @Test
    @DisplayName("E2E 04 - Donor decline flow should surface RECUSADA to the student")
    void shouldRunDeclineFlow() throws Exception {
        Usuario donor = createCompleteUser("e2e-decline-donor@example.com", "Doador", true);
        Usuario student = createCompleteUser("e2e-decline-student@example.com", "Estudante", true);
        Material material = createMaterialEntity(donor, "Colecao recusada", NivelEnsino.FUNDAMENTAL, 6, SistemaEnsino.ANGLO);
        String requestId = createRequest(tokenFor(student), material.getId().toString());

        mockMvc.perform(patch("/v1/solicitacoes/{id}/recusar", requestId)
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RECUSADA"));

        mockMvc.perform(get("/v1/solicitacoes/minhas")
                        .header("Authorization", "Bearer " + tokenFor(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("RECUSADA"));
    }

    @Test
    @DisplayName("E2E 05 - Student cancellation should keep the material available")
    void shouldKeepMaterialAvailableAfterPendingCancellation() throws Exception {
        Usuario donor = createCompleteUser("e2e-cancel-donor@example.com", "Doador", true);
        Usuario student = createCompleteUser("e2e-cancel-student@example.com", "Estudante", true);
        Material material = createMaterialEntity(donor, "Colecao cancelavel", NivelEnsino.FUNDAMENTAL, 6, SistemaEnsino.ANGLO);
        String requestId = createRequest(tokenFor(student), material.getId().toString());

        mockMvc.perform(patch("/v1/solicitacoes/{id}/cancelar", requestId)
                        .header("Authorization", "Bearer " + tokenFor(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELADA"));

        assertThat(materialRepository.findById(material.getId()))
                .hasValueSatisfying(saved -> assertThat(saved.getStatus()).isEqualTo(StatusMaterial.DISPONIVEL));
    }

    @Test
    @DisplayName("E2E 06 - Incomplete profile should be blocked from protected material creation")
    void shouldBlockIncompleteProfileFromCreatingMaterial() throws Exception {
        Usuario incomplete = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("e2e-incomplete@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Incomplete User")
                .perfilCompleto(false)
                .role(Role.USER)
                .build());

        mockMvc.perform(post("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(incomplete))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "upload_id": "missing-preview",
                                  "titulo": "Bloqueado",
                                  "descricao": "Descricao suficiente para disparar o gate de perfil.",
                                  "disciplina": "MATEMATICA",
                                  "nivel_ensino": "FUNDAMENTAL",
                                  "ano": 7,
                                  "sistema_ensino": "ANGLO",
                                  "estado_conservacao": "BOM"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("E2E 07 - AI consent gate should return a manual fallback preview")
    void shouldReturnManualFallbackWhenAiConsentIsDisabled() throws Exception {
        Usuario donor = createCompleteUser("e2e-no-ai@example.com", "Doador", false);

        mockMvc.perform(multipart("/v1/materiais/preview")
                        .file(validPng("manual-fallback"))
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status_ia").value("FAILURE"))
                .andExpect(jsonPath("$.data.error_details.missing_fields[0]").value("consentimento_ia"));
    }

    @Test
    @DisplayName("E2E 08 - Invalid material enums should return HTTP 400")
    void shouldRejectInvalidMaterialEnum() throws Exception {
        Usuario donor = createCompleteUser("e2e-invalid-enum@example.com", "Doador", true);
        String uploadId = previewUploadIdFor(tokenFor(donor), validPng("invalid-enum"));

        mockMvc.perform(post("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(donor))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "upload_id": "%s",
                                  "titulo": "Colecao invalida",
                                  "descricao": "Descricao valida para enum invalido.",
                                  "disciplina": "INEXISTENTE",
                                  "nivel_ensino": "FUNDAMENTAL",
                                  "ano": 7,
                                  "sistema_ensino": "ANGLO",
                                  "estado_conservacao": "BOM",
                                  "necessidade_academica": "TEXTBOOKS"
                                }
                                """.formatted(uploadId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field_errors.disciplina").value("Valor invalido para disciplina"));
    }

    @Test
    @DisplayName("E2E 09 - Requesting a non-existent material should return HTTP 404")
    void shouldRejectMissingMaterialRequest() throws Exception {
        Usuario student = createCompleteUser("e2e-missing-material@example.com", "Estudante", true);

        mockMvc.perform(post("/v1/materiais/{id}/solicitacoes", UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenFor(student)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Material nao encontrado"));
    }

    @Test
    @DisplayName("E2E 10 - Donor should not be able to request the own material")
    void shouldRejectSelfRequest() throws Exception {
        Usuario donor = createCompleteUser("e2e-self-request@example.com", "Doador", true);
        Material material = createMaterialEntity(donor, "Colecao propria", NivelEnsino.FUNDAMENTAL, 7, SistemaEnsino.ANGLO);

        mockMvc.perform(post("/v1/materiais/{id}/solicitacoes", material.getId())
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Nao e possivel solicitar o proprio material"));
    }

    @Test
    @DisplayName("E2E 11 - Same student should not request the same material twice")
    void shouldRejectDuplicateActiveRequest() throws Exception {
        Usuario donor = createCompleteUser("e2e-duplicate-donor@example.com", "Doador", true);
        Usuario student = createCompleteUser("e2e-duplicate-student@example.com", "Estudante", true);
        Material material = createMaterialEntity(donor, "Colecao unica", NivelEnsino.FUNDAMENTAL, 7, SistemaEnsino.ANGLO);

        createRequest(tokenFor(student), material.getId().toString());

        mockMvc.perform(post("/v1/materiais/{id}/solicitacoes", material.getId())
                        .header("Authorization", "Bearer " + tokenFor(student)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Voce ja possui uma solicitacao ativa para este material"));
    }

    @Test
    @DisplayName("E2E 12 - Expired JWT should return HTTP 401")
    void shouldRejectExpiredJwt() throws Exception {
        Usuario user = createCompleteUser("e2e-expired-token@example.com", "Expirado", true);

        mockMvc.perform(get("/v1/usuarios/me")
                        .header("Authorization", "Bearer " + expiredTokenFor(user)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("E2E 13 - Two preview uploads should receive distinct upload ids")
    void shouldGenerateDistinctUploadIds() throws Exception {
        Usuario firstUser = createCompleteUser("e2e-upload-1@example.com", "Primeiro", true);
        Usuario secondUser = createCompleteUser("e2e-upload-2@example.com", "Segundo", true);

        String firstUploadId = previewUploadIdFor(tokenFor(firstUser), validPng("upload-1"));
        String secondUploadId = previewUploadIdFor(tokenFor(secondUser), validPng("upload-2"));

        assertThat(firstUploadId).isNotEqualTo(secondUploadId);
    }

    @Test
    @DisplayName("E2E 14 - GIF and WebP should be rejected during preview")
    void shouldRejectGifAndWebp() throws Exception {
        Usuario donor = createCompleteUser("e2e-image-formats@example.com", "Doador", true);

                mockMvc.perform(multipart("/v1/materiais/preview")
                        .file(new MockMultipartFile("file", "animado.gif", "image/gif", "GIF89a".getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field_errors.image").value("Formato não suportado. Escolha uma imagem em JPG ou PNG."));

                mockMvc.perform(multipart("/v1/materiais/preview")
                        .file(new MockMultipartFile("file", "capa.webp", "image/webp", "RIFF1234WEBPVP8 ".getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + tokenFor(donor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field_errors.image").value("Formato não suportado. Escolha uma imagem em JPG ou PNG."));
    }

    @Test
    @DisplayName("E2E 15 - WhatsApp validation should accept E.164 and reject formatted input")
    void shouldValidateWhatsappFormats() throws Exception {
        Usuario validUser = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("e2e-valid-whatsapp@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Valid WhatsApp")
                .perfilCompleto(false)
                .role(Role.USER)
                .build());
        Usuario invalidUser = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("e2e-invalid-whatsapp-format@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Invalid WhatsApp")
                .perfilCompleto(false)
                .role(Role.USER)
                .build());

        mockMvc.perform(putProfileRequest(tokenFor(validUser), """
                {
                  "nome": "Valid WhatsApp",
                  "whatsapp": "+5511999999999",
                  "cpf": "52998224725",
                  "cidade": "Criciuma",
                  "bairro": "Centro"
                }
                """))
                .andExpect(status().isOk());

        mockMvc.perform(putProfileRequest(tokenFor(invalidUser), """
                {
                  "nome": "Invalid WhatsApp",
                  "whatsapp": "+55 (11) 99999-9999",
                  "cpf": "52998224725",
                  "cidade": "Criciuma",
                  "bairro": "Centro"
                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field_errors.whatsapp").value("Use o formato E.164 (+55XXXXXXXXXXX)"));
    }

    @Test
    @DisplayName("E2E 16 - Geographic normalization should make accented city filters match stored ASCII cities")
    void shouldMatchAccentedCityFilters() throws Exception {
        Usuario requester = createCompleteUser("e2e-geo-requester@example.com", "Busca", true);
        Usuario donor = createCompleteUser("e2e-geo-donor@example.com", "Doador", true, "SAO PAULO", "Centro");
        createMaterialEntity(donor, "Colecao paulista", NivelEnsino.FUNDAMENTAL, 7, SistemaEnsino.ANGLO);

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(requester))
                        .param("cidade", "S\u00E3o Paulo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.results[0].cidade").value("SAO PAULO"));
    }

    @Test
    @DisplayName("E2E 17 - SUPERIOR searches should ignore the year filter and still return higher-ed materials")
    void shouldIgnoreYearForSuperiorSearches() throws Exception {
        Usuario requester = createCompleteUser("e2e-superior-requester@example.com", "Busca", true);
        Usuario donor = createCompleteUser("e2e-superior-donor@example.com", "Doador", true);

        materialRepository.saveAndFlush(Material.builder()
                .doador(donor)
                .titulo("Calculo Superior")
                .descricao("Descricao de calculo superior")
                .disciplina(Disciplina.MATEMATICA)
                .nivelEnsino(NivelEnsino.SUPERIOR)
                .ano(null)
                .sistemaEnsino(SistemaEnsino.OUTRO)
                .estadoConservacao(EstadoConservacao.BOM)
                .status(StatusMaterial.DISPONIVEL)
                .cidade(donor.getCidade())
                .bairro(donor.getBairro())
                .dataPublicacao(2023)
                .build());

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(requester))
                        .param("nivel_ensino", "SUPERIOR")
                        .param("ano", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.results[0].nivel_ensino").value("SUPERIOR"));
    }

    @Test
    @DisplayName("E2E 18 - Exact system matching should exclude different systems")
    void shouldRequireExactTeachingSystem() throws Exception {
        Usuario requester = createCompleteUser("e2e-system-requester@example.com", "Busca", true);
        Usuario donor = createCompleteUser("e2e-system-donor@example.com", "Doador", true);

        createMaterialEntity(donor, "Anglo Exato E2E", NivelEnsino.FUNDAMENTAL, 7, SistemaEnsino.ANGLO);
        createMaterialEntity(donor, "Objetivo Excluido E2E", NivelEnsino.FUNDAMENTAL, 7, SistemaEnsino.OBJETIVO);

        mockMvc.perform(get("/v1/materiais")
                        .header("Authorization", "Bearer " + tokenFor(requester))
                        .param("sistema_ensino", "ANGLO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.results[0].titulo").value("Anglo Exato E2E"));
    }

    @Test
    @DisplayName("E2E 19 - Approved-student account deletion should reopen the material")
    void shouldReopenReservedMaterialOnApprovedStudentDeletion() throws Exception {
        Usuario donor = createCompleteUser("e2e-delete-donor@example.com", "Doador", true, "FLORIANOPOLIS", "Centro", "delete-pass-123");
        Usuario student = createCompleteUser("e2e-delete-student@example.com", "Estudante", true, "FLORIANOPOLIS", "Trindade", "delete-pass-123");
        Material material = createMaterialEntity(donor, "Colecao delete E2E", NivelEnsino.FUNDAMENTAL, 8, SistemaEnsino.ANGLO);
        material.setStatus(StatusMaterial.RESERVADO);
        materialRepository.saveAndFlush(material);

        Solicitacao request = Solicitacao.builder()
                .material(material)
                .estudante(student)
                .status(StatusSolicitacao.APROVADA)
                .aprovadoEm(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(13))
                .build();
        request.populateContatoDoador(donor);
        request = solicitacaoRepository.saveAndFlush(request);

        mockMvc.perform(post("/v1/usuarios/delete")
                        .header("Authorization", "Bearer " + tokenFor(student))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "delete-pass-123",
                                  "reason": "Cenario E2E"
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(materialRepository.findById(material.getId()))
                .hasValueSatisfying(saved -> assertThat(saved.getStatus()).isEqualTo(StatusMaterial.DISPONIVEL));
        assertThat(solicitacaoRepository.findById(request.getId()))
                .hasValueSatisfying(saved -> assertThat(saved.getStatus()).isEqualTo(StatusSolicitacao.CANCELADA));
    }

    @Test
    @DisplayName("E2E 20 - Email changes should invalidate the previous session")
    void shouldInvalidateOldSessionAfterEmailChange() throws Exception {
        Usuario user = usuarioRepository.saveAndFlush(Usuario.builder()
                .email("e2e-email-change@example.com")
                .passwordHash(SEEDED_PASSWORD_HASH)
                .nome("Email Change User")
                .perfilCompleto(false)
                .role(Role.USER)
                .build());
        String oldToken = tokenFor(user);

        mockMvc.perform(putProfileRequest(oldToken, """
                {
                  "email": "novo-email-e2e@example.com",
                  "nome": "Email Change User",
                  "whatsapp": "+5511991234567",
                  "cpf": "52998224725",
                  "cidade": "Florianopolis",
                  "bairro": "Centro"
                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("novo-email-e2e@example.com"));

        mockMvc.perform(get("/v1/usuarios/me")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
    }

    private RegisteredUser register(String email, String password, String nome) throws Exception {
        String body = mockMvc.perform(post("/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "nome": "%s"
                                }
                                """.formatted(email, password, nome)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(body).path("data");
        return new RegisteredUser(
                email,
                password,
                nome,
                json.path("id").asText(),
                json.path("token").asText()
        );
    }

    private void completeProfile(String token,
                                 String nome,
                                 String whatsapp,
                                 String cidade,
                                 String bairro,
                                 boolean consentimentoIa) throws Exception {
        mockMvc.perform(putProfileRequest(token, """
                {
                  "nome": "%s",
                  "whatsapp": "%s",
                  "cpf": "52998224725",
                  "cidade": "%s",
                  "bairro": "%s",
                  "consentimento_ia": %s
                }
                """.formatted(nome, whatsapp, cidade, bairro, consentimentoIa)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.perfil_completo").value(true));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder putProfileRequest(String token,
                                                                                                        String body) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/v1/usuarios/me")
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON)
                .content(body);
    }

    private String previewUploadIdFor(String token, MockMultipartFile file) throws Exception {
        String body = mockMvc.perform(multipart("/v1/materiais/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).path("data").path("upload_id").asText();
    }

    private String createMaterial(String token,
                                  String uploadId,
                                  String titulo,
                                  String disciplina,
                                  String nivelEnsino,
                                  Integer ano,
                                  String sistemaEnsino) throws Exception {
        String body = mockMvc.perform(post("/v1/materiais")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "upload_id": "%s",
                                  "titulo": "%s",
                                  "descricao": "Descricao automatizada suficiente para o cenario E2E.",
                                  "disciplina": "%s",
                                  "nivel_ensino": "%s",
                                  "ano": %s,
                                  "sistema_ensino": "%s",
                                  "estado_conservacao": "BOM",
                                  "necessidade_academica": "TEXTBOOKS",
                                  "data_publicacao": 2023
                                }
                                """.formatted(uploadId, titulo, disciplina, nivelEnsino, ano, sistemaEnsino)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(body).path("data").path("id").asText();
    }

    private String createRequest(String token, String materialId) throws Exception {
        String body = mockMvc.perform(post("/v1/materiais/{id}/solicitacoes", materialId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(body).path("data").path("id").asText();
    }

    private Usuario createCompleteUser(String email, String nome, boolean consentimentoIa) {
        return createCompleteUser(email, nome, consentimentoIa, "FLORIANOPOLIS", "CENTRO");
    }

    private Usuario createCompleteUser(String email,
                                       String nome,
                                       boolean consentimentoIa,
                                       String cidade,
                                       String bairro) {
        return createCompleteUser(email, nome, consentimentoIa, cidade, bairro, null);
    }

    private Usuario createCompleteUser(String email,
                                       String nome,
                                       boolean consentimentoIa,
                                       String cidade,
                                       String bairro,
                                       String rawPassword) {
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .email(email)
                .passwordHash(rawPassword == null ? SEEDED_PASSWORD_HASH : passwordEncoder.encode(rawPassword))
                .nome(nome)
                .whatsapp("+5511991234567")
                .cpf("52998224725")
                .cidade(cidade)
                .bairro(bairro)
                .perfilCompleto(true)
                .consentimentoIa(consentimentoIa)
                .role(Role.USER)
                .build());
    }

    private Material createMaterialEntity(Usuario donor,
                                          String title,
                                          NivelEnsino nivelEnsino,
                                          Integer ano,
                                          SistemaEnsino sistemaEnsino) {
        return materialRepository.saveAndFlush(Material.builder()
                .doador(donor)
                .titulo(title)
                .descricao("Descricao detalhada para " + title)
                .disciplina(Disciplina.MATEMATICA)
                .nivelEnsino(nivelEnsino)
                .ano(ano)
                .sistemaEnsino(sistemaEnsino)
                .estadoConservacao(EstadoConservacao.BOM)
                .status(StatusMaterial.DISPONIVEL)
                .cidade(donor.getCidade())
                .bairro(donor.getBairro())
                .dataPublicacao(2022)
                .build());
    }

    private String tokenFor(Usuario usuario) {
        return jwtTokenProvider.generateToken(
                usuario.getEmail(),
                usuario.getRole().name(),
                usuario.isPerfilCompleto(),
                usuario.getId().toString()
        );
    }

    private String expiredTokenFor(Usuario usuario) {
        String secret = (String) ReflectionTestUtils.getField(jwtTokenProvider, "jwtSecret");
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Date issuedAt = new Date(System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000L);
        Date expiredAt = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L);

        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("role", usuario.getRole().name())
                .claim("perfilCompleto", usuario.isPerfilCompleto())
                .claim("userId", usuario.getId().toString())
                .issuedAt(issuedAt)
                .expiration(expiredAt)
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    private MockMultipartFile validPng(String name) throws Exception {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        int color = switch (name.hashCode() & 3) {
            case 0 -> 0x00695C;
            case 1 -> 0x00796B;
            case 2 -> 0x00897B;
            default -> 0x009688;
        };
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                image.setRGB(x, y, color);
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return new MockMultipartFile("file", name + ".png", IMAGE_PNG_VALUE, outputStream.toByteArray());
    }

    private record RegisteredUser(
            String email,
            String password,
            String nome,
            String userId,
            String token
    ) {
    }
}
