package com.yoganavi.user.service.jwt;

public interface JwtService {

    String reIssueRefreshToken(String accessToken, String refreshToken);
}
