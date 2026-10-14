package com.medical.inventory.controller;

import com.medical.inventory.dto.InventoryResponse;
import com.medical.inventory.dto.PagedResponse;
import com.medical.inventory.entity.InventoryStatus;
import com.medical.inventory.security.JwtAuthFilter;
import com.medical.inventory.security.JwtTokenProvider;
import com.medical.inventory.security.UserDetailsServiceImpl;
import com.medical.inventory.service.InventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc(addFilters = false) // Bypass security filters for web slice tests
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryService inventoryService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    @WithMockUser
    @DisplayName("GET /api/inventory/{id} should return inventory item details")
    void testGetItemById_Success() throws Exception {
        InventoryResponse item = new InventoryResponse(
                1L, "MED-001", "Paracetamol 500mg", "BAT-001", 100,
                new BigDecimal("12.50"), LocalDate.of(2026, 12, 31),
                InventoryStatus.ACTIVE, Instant.now(), Instant.now()
        );

        when(inventoryService.getItemById(1L)).thenReturn(item);

        mockMvc.perform(get("/api/inventory/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.entryCode").value("MED-001"))
                .andExpect(jsonPath("$.data.itemName").value("Paracetamol 500mg"))
                .andExpect(jsonPath("$.data.quantity").value(100));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/inventory/search should return paginated search results")
    void testSearch_Success() throws Exception {
        InventoryResponse item = new InventoryResponse(
                1L, "MED-001", "Paracetamol 500mg", "BAT-001", 100,
                new BigDecimal("12.50"), LocalDate.of(2026, 12, 31),
                InventoryStatus.ACTIVE, Instant.now(), Instant.now()
        );

        PagedResponse<InventoryResponse> pagedResponse = PagedResponse.from(
                new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1)
        );

        when(inventoryService.search(any(), any(), any(), any())).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/inventory/search")
                        .param("name", "Paracetamol")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].entryCode").value("MED-001"));
    }
}
