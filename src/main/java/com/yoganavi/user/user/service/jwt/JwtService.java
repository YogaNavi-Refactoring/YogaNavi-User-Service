package com.yoganavi.user.user.service.jwt;

public interface JwtService {

    String reIssueRefreshToken(String accessToken, String refreshToken);
}
