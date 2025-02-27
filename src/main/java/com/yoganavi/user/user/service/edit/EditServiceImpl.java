package com.yoganavi.user.user.service.edit;

import com.yoganavi.user.common.entity.Users;
import com.yoganavi.user.common.repository.UserRepository;
import com.yoganavi.user.user.dto.edit.UpdateDto;
import com.yoganavi.user.user.service.email.EmailService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EditServiceImpl implements EditService {

    private static final String PURPOSE = "비밀번호 변경";

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String sendCredentialToken(String email) {
        int valCase = checkUser(email);
        if (valCase == 1) {
            return emailService.sendVerificationEmail(email, PURPOSE);
        } else if (valCase == 0) {
            return "존재하지 않는 회원입니다.";
        } else {
            return "가입할 수 없습니다.";
        }
    }

    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public int checkUser(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            return 1;
        }
        if (email.matches("deleted_\\d+@yoganavi\\.com")) {
            return 2;
        }
        return 0;
    }

    @Override
    public boolean validateToken(String email, String token) {
        return emailService.validateToken(email, token, PURPOSE);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Users setNewCredential(UpdateDto updateDto) {
        log.info("비밀번호 변경 시작: 이메일 {}", updateDto.getEmail());

        try {
            if (!emailService.isVerified(updateDto.getEmail(), PURPOSE)) {
                throw new IllegalStateException("이메일 인증이 완료되지 않았습니다.");
            }

            String hashPwd = passwordEncoder.encode(updateDto.getPassword());
            Users user = userRepository.findByEmail(updateDto.getEmail())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));
            user.setPwd(hashPwd);
            user.setIsDeleted(false);
            Users saveMember = userRepository.save(user);

            emailService.clearVerificationStatus(updateDto.getEmail(), PURPOSE);

            log.info("비밀번호 변경 완료: 사용자 ID {}", saveMember.getUserId());
            return saveMember;

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("이메일 유효성 검증 실패: {}", e.getMessage());
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("데이터 무결성 위반: {}", e.getMessage());
            throw new IllegalStateException("회원 정보 저장 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("비밀번호 변경 중 예기치 않은 오류 발생", e);
            throw new RuntimeException("비밀번호 변경 중 오류가 발생했습니다.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkCredential(Long userId, String password) {
        log.info("비밀번호 확인 시작: id {}", userId);

        try {
            Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));
            return passwordEncoder.matches(password, user.getPwd());
        } catch (Exception e) {
            log.error("비밀번호 확인 중 예기치 않은 오류 발생", e);
            throw new RuntimeException("비밀번호 확인 중 오류가 발생했습니다.");
        }
    }

    @Override
    public UpdateDto createUpdateResponse(Users user) {
        UpdateDto responseDto = new UpdateDto();
        boolean isTeacher = user.getRole().equals("TEACHER");

        responseDto.setImageUrl(user.getProfileImageUrl());
        responseDto.setImageUrlSmall(user.getProfileImageUrlSmall());
        responseDto.setNickname(user.getNickname());
        responseDto.setTeacher(isTeacher);

        if (isTeacher) {
            Set<String> myTags = getUserHashtags(user.getUserId());
            responseDto.setHashTags(new ArrayList<>(myTags));
            responseDto.setContent(user.getContent());
        }

        return responseDto;
    }

    @Override
    public Users updateUser(UpdateDto updateDto, Long userId) {
        log.info("사용자 정보 업데이트 시작: 사용자 ID {}", userId);

        try {
            Users user = userRepository.findByEmail(updateDto.getEmail())
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

            updateUserFields(user, updateDto, userId);

            Users updatedUser = userRepository.save(user);
            log.info("사용자 정보 업데이트 완료: 사용자 ID {}", userId);
            return updatedUser;
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("입력값 유효성 검증 실패: {}", e.getMessage());
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("데이터 무결성 위반: {}", e.getMessage());
            throw new IllegalStateException("회원 정보 저장 중 오류가 발생했습니다.");
        } catch (Exception e) {
            log.error("회원 정보 변경 중 예기치 않은 오류 발생", e);
            throw new RuntimeException("회원 정보 변경 중 오류가 발생했습니다.");
        }
    }

    private void updateUserFields(Users user, UpdateDto updateDto, Long userId) {
        if (updateDto.getNickname() != null && !updateDto.getNickname().isEmpty()) {
            log.debug("사용자 {}의 닉네임 변경: {}", userId, updateDto.getNickname());
            user.setNickname(updateDto.getNickname());
        }

        updateProfileImages(user, updateDto, userId);

        if (updateDto.getPassword() != null && !updateDto.getPassword().isEmpty()) {
            log.debug("사용자 {}의 비밀번호 수정", userId);
            user.setPwd(passwordEncoder.encode(updateDto.getPassword()));
        }

        if (updateDto.getHashTags() != null && !updateDto.getHashTags().isEmpty()) {
            log.debug("사용자 {}의 해시태그 수정: {}", userId, updateDto.getHashTags());
            // updateUserHashtags(userId, Set.copyOf(updateDto.getHashTags()));
        }

        if (updateDto.getContent() != null && !updateDto.getContent().isEmpty()) {
            log.debug("사용자 {}의 소개 내용 수정: {}", userId, updateDto.getContent());
            user.setContent(updateDto.getContent());
        }
    }

    private void updateProfileImages(Users user, UpdateDto updateDto, Long userId) {
        if (updateDto.getImageUrl() != null && !updateDto.getImageUrl().isEmpty()) {
            updateProfileImage(user, updateDto.getImageUrl(), user.getProfileImageUrl(), userId,
                "프로필");
            user.setProfileImageUrl(updateDto.getImageUrl());
        }

        if (updateDto.getImageUrlSmall() != null && !updateDto.getImageUrlSmall().isEmpty()) {
            updateProfileImage(user, updateDto.getImageUrlSmall(), user.getProfileImageUrlSmall(),
                userId, "소형 프로필");
            user.setProfileImageUrlSmall(updateDto.getImageUrlSmall());
        }
    }

    private void updateProfileImage(Users user, String newImageUrl, String oldImageUrl, Long userId,
        String imageType) {
        if (oldImageUrl != null && !oldImageUrl.equals(newImageUrl)) {
            log.debug("사용자 {}의 {} 이미지 삭제: {}", userId, imageType, oldImageUrl);
            try {
                // s3Service.deleteFile(oldImageUrl);
            } catch (Exception e) {
                log.error("사용자 {}의 {} 이미지 삭제 불가: {}", userId, imageType, oldImageUrl, e);
                throw new RuntimeException("이전 " + imageType + " 이미지 삭제 불가", e);
            }
        }
        log.debug("사용자 {}의 {} 이미지를 변경: {}", userId, imageType, newImageUrl);
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Set<String> getUserHashtags(Long userId) {
        Optional<Users> users = userRepository.findById(userId);
//        if (users.isPresent()) {
//            Users user = users.get();
//            return user.getHashtags().stream()
//                .map(Hashtag::getName)
//                .collect(Collectors.toSet());
//        }
        return Collections.emptySet();
    }

}

