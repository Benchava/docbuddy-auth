package com.docbuddy.auth.controller.request;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    private String userName;

    private String password;
}
