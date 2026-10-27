
package com.stockflow.security;

import com.stockflow.config.SecurityConfig;
import com.stockflow.controller.AuthController;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;

import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class SecurityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // TEST 1: Un usuario sin iniciar sesión no puede acceder.
    @Test
    void anonymousUserCannotAccessProtectedEndpoint()
            throws Exception {

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    // TEST 2: ADMIN puede consultar su información.
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanAccessProtectedEndpoint()
            throws Exception {

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    // TEST 3: EMPLOYEE puede consultar su información.
    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void employeeCanAccessProtectedEndpoint()
            throws Exception {

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("employee"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    // TEST 4: EMPLOYEE no puede crear productos.
    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void employeeCannotCreateProducts()
            throws Exception {

        mockMvc.perform(
                post("/api/products")
                        .contentType("application/json")
                        .content("""
                            {
                              "name": "Producto de prueba",
                              "price": 20.00,
                              "stock": 10
                            }
                            """)
        ).andExpect(status().isForbidden());
    }

    // TEST 5: El navegador puede realizar preflight CORS.
    @Test
    void browserCorsPreflightIsAllowed()
            throws Exception {

        mockMvc.perform(
                        options("/api/auth/me")
                                .header("Origin", "http://localhost:4200")
                                .header("Access-Control-Request-Method", "GET")
                                .header("Access-Control-Request-Headers",
                                        "authorization")
                )
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Access-Control-Allow-Origin",
                        "http://localhost:4200"
                ));
    }
}
