package com.fishing.brazil.dto.request.login;

import lombok.Data;

@Data
public class VerifyRequestDTO {
    private String email;
    private String code;
}