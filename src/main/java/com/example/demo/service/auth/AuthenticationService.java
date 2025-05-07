package com.example.demo.service.auth;

import com.example.demo.dto.auth.AuthenticationRequestDTO;
import com.example.demo.dto.auth.IntrospectRequest;
import com.example.demo.dto.auth.IntrospectResponse;
import com.example.demo.response.AuthenticationResponse;
import com.nimbusds.jose.JOSEException;

import java.text.ParseException;

public interface AuthenticationService {
        AuthenticationResponse Authenticate(AuthenticationRequestDTO authenticationRequestDTO);
        IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException;
        IntrospectResponse introspectRefreshToken(IntrospectRequest request) throws JOSEException, ParseException;

}
