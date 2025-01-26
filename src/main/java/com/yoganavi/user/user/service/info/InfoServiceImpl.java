package com.yoganavi.user.user.service.info;

import com.yoganavi.user.common.entity.Users;
import com.yoganavi.user.common.repository.UserRepository;
import com.yoganavi.user.user.dto.edit.UpdateDto;
import java.util.ArrayList;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class InfoServiceImpl implements InfoService {

    private final UserRepository userRepository;


    @Override
    @Transactional(readOnly = true)
    public Set<String> getUserHashtags(Long userId) {
        try {
            log.debug("사용자 {}의 해시태그 조회 시작", userId);
            return Set.of();  // 해시태그 조회 로직 필요
        } catch (Exception e) {
            log.error("사용자 {}의 해시태그 조회 중 오류 발생", userId, e);
            throw new RuntimeException("해시태그 조회 중 오류 발생.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UpdateDto getUserInfo(Long userId, String role) {
        log.info("사용자 정보 조회 시작: ID {}, 역할 {}", userId, role);

        try {
            // 입력값 검증 추가

            Users user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없음: ID {}", userId);
                    return new RuntimeException("사용자 찾을 수 없음.");
                });

            UpdateDto responseDto = createUserInfoDto(user, role);

            log.info("사용자 정보 조회 완료: ID {}", userId);
            return responseDto;

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 입력값: {}", e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("사용자 정보 조회 중 오류 발생: ID {}", userId, e);
            throw e;
        } catch (Exception e) {
            log.error("예기치 않은 오류 발생: ID {}", userId, e);
            throw new RuntimeException("사용자 정보 조회 중 오류 발생.", e);
        }
    }

    private UpdateDto createUserInfoDto(Users user, String role) {
        try {
            UpdateDto responseDto = new UpdateDto();
            boolean isTeacher = role.equals("TEACHER");

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
        } catch (Exception e) {
            log.error("사용자 정보 DTO 생성 중 오류 발생: ID {}", user.getUserId(), e);
            throw new RuntimeException("사용자 정보 변환 중 오류 발생.", e);
        }
    }
}