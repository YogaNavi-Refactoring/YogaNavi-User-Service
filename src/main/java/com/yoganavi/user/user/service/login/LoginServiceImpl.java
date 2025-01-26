package com.yoganavi.user.user.service.login;

import com.yoganavi.user.common.entity.Users;
import com.yoganavi.user.common.repository.UserRepository;
import com.yoganavi.user.common.util.JwtUtil;
import com.yoganavi.user.user.dto.login.LoginRequestDto;
import com.yoganavi.user.user.dto.login.LoginResponseDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
@Transactional
public class LoginServiceImpl implements LoginService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LoginResponseDto login(LoginRequestDto loginRequestDto) {
        String email = loginRequestDto.getEmail();
        String password = loginRequestDto.getPassword();

        // 사용자 정보 조회
        Users user = userRepository.findByEmail(email)
            .orElseThrow(() -> {
                log.warn("존재하지 않는 사용자로 로그인 시도: {}", email);
                return new BadCredentialsException("사용자가 존재하지 않습니다.");
            });

        // 삭제된 계정 체크
        if (user.getIsDeleted()) {
            log.warn("삭제된 계정으로 로그인 시도: {}", email);
            throw new BadCredentialsException("계정이 삭제되었습니다.");
        }

        // 탈퇴 진행 중인 사용자 확인 및 처리
        if (user.getDeletedAt() != null) {
            if (passwordEncoder.matches(password, user.getPwd())) {
                if (!recoverAccount(user)) {
                    throw new RuntimeException("사용자 계정 복구 실패");
                }
                log.info("사용자 {} 계정 복구됨.", email);
            } else {
                log.warn("탈퇴 진행 중인 계정 비밀번호 불일치: {}", email);
                throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
            }
        }

        // 비밀번호 검증
        if (!passwordEncoder.matches(password, user.getPwd())) {
            log.warn("비밀번호 불일치: {}", email);
            throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
        }

        // 인증 객체 생성 및 설정
        List<GrantedAuthority> authorities = getGrantedAuthorities(user.getRole());
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(email, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // JWT 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(
            user.getEmail(),
            user.getRole()
        );
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        log.info("사용자 로그인 성공: {}", email);

        return LoginResponseDto.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .message("로그인 성공")
            .build();
    }

    /**
     * 사용자 계정 복구
     *
     * @param user 복구할 사용자 엔티티
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Override
    public boolean recoverAccount(Users user) {

        if (user == null) {
            return false;
        }

        Optional<Users> recoveredUsers = userRepository.findById(user.getUserId());
        if (recoveredUsers.isPresent()) {
            log.info("계정 복구 시작: 사용자 ID {}", user.getUserId());
            Users recoveredUser = recoveredUsers.get();
            recoveredUser.setDeletedAt(null);
            recoveredUser.setIsDeleted(false);
            userRepository.save(recoveredUser);
            log.info("사용자 계정 복구 성공. 사용자 ID: {}, 이메일: {}", recoveredUser.getUserId(),
                recoveredUser.getEmail());
            return true;
        } else {
            return false;
        }

    }

    /**
     * 사용자 권한 목록 생성
     *
     * @param role 사용자 역할
     * @return 사용자 권한
     */
    private List<GrantedAuthority> getGrantedAuthorities(String role) {
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
        return grantedAuthorities;
    }

}
