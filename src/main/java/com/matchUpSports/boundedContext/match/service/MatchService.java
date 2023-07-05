package com.matchUpSports.boundedContext.match.service;

import com.matchUpSports.base.rsData.RsData;
import com.matchUpSports.boundedContext.field.entity.Field;
import com.matchUpSports.boundedContext.field.repository.FieldRepository;
import com.matchUpSports.boundedContext.match.entity.Match;
import com.matchUpSports.boundedContext.match.entity.MatchMember;
import com.matchUpSports.boundedContext.match.matchFormDto.MatchForm;
import com.matchUpSports.boundedContext.match.repository.MatchMemberRepository;
import com.matchUpSports.boundedContext.match.repository.MatchRepository;
import com.matchUpSports.boundedContext.member.entity.Member;
import com.matchUpSports.boundedContext.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService {
    private final MatchRepository matchRepository;
    private final MatchMemberRepository matchMemberRepository;
    private final FieldRepository fieldRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public RsData<Match> createMatch(MatchForm matchForm, long loggedInMemberId) {

        // 로그인 한 회원 조회
        Member loggedInMember = getLoggedInMember(loggedInMemberId);
        if (loggedInMember == null) {
            return RsData.of("F-1", "멤버를 찾을 수 없습니다.");
        }

        // 이미 등록한 동일한 조건의 매치 확인
        if (isDuplicateMatch(matchForm, loggedInMember)) {
            return RsData.of("F-3", "이미 동일한 조건의 매치를 등록했습니다.");
        }

        // 입력한 정보 중 누락된 값 확인
        if (hasMissingValues(matchForm)) {
            return RsData.of("F-2", "입력한 정보 중 누락된 값이 있습니다.");
        }

        // 동일한 조건의 매치가 존재하고 인원이 2명 미만인 경우, 해당 매치에 멤버 추가
        // 동일한 조건의 매치가 모두 2명이 참가하고 있거나 존재하지 않을 경우, 새로운 매치 생성
        MatchAndSubStadium availableMatchAndSubStadium = findAvailableMatchAndSubStadium(matchForm);
        if (availableMatchAndSubStadium.subStadium == -1) {
            return RsData.of("F-1", "모든 구장이 가득 찼습니다.");
        }

        Match newOrExistingMatch;
        if (availableMatchAndSubStadium.match != null) {
            newOrExistingMatch = availableMatchAndSubStadium.match;
            newOrExistingMatch.setParticipantCount(newOrExistingMatch.getParticipantCount() + 1);
        } else {
            newOrExistingMatch = createAndSaveMatch(matchForm, availableMatchAndSubStadium.subStadium);
        }

        // 매치에 멤버 추가 및 저장
        addMatchMember(loggedInMember, newOrExistingMatch);

        return RsData.successOf(newOrExistingMatch);
    }

    private static class MatchAndSubStadium {
        Match match;
        int subStadium;

        MatchAndSubStadium(Match match, int subStadium) {
            this.match = match;
            this.subStadium = subStadium;
        }
    }

    private MatchAndSubStadium findAvailableMatchAndSubStadium(MatchForm matchForm) {
        List<Match> matches = matchRepository.findByStadiumAndMatchDateAndUsageTime(
                matchForm.getStadium(),
                matchForm.getMatchDate(),
                matchForm.getUsageTime()
        );

        Field selectedField = fieldRepository.findByFieldName(matchForm.getStadium());
        int maxSubStadiumCount = selectedField.getCourtCount();

        int[] subStadiumMembers = new int[maxSubStadiumCount + 1];

        for (Match existingMatch : matches) {
            int currentSubStadium = existingMatch.getSubStadiumCount();
            int currentParticipantCount = existingMatch.getParticipantCount();
            subStadiumMembers[currentSubStadium] = currentParticipantCount;
        }

        for (int i = 1; i <= maxSubStadiumCount; i++) {
            if (subStadiumMembers[i] < 2) {
                for (Match existingMatch : matches) {
                    if (existingMatch.getSubStadiumCount() == i) {
                        return new MatchAndSubStadium(existingMatch, i);
                    }
                }
                return new MatchAndSubStadium(null, i);
            }
        }

        return new MatchAndSubStadium(null, -1);
    }

    private Member getLoggedInMember(long memberId) {
        return memberRepository.findById(memberId).orElse(null);
    }

    private boolean isDuplicateMatch(MatchForm matchForm, Member loggedInMember) {
        List<MatchMember> matchMembers = matchMemberRepository.findByMember(loggedInMember);

        for (MatchMember matchMember : matchMembers) {
            Match existingMatch = matchMember.getMatch();
            if (existingMatch.getStadium().equals(matchForm.getStadium()) &&
                    existingMatch.getMatchDate().equals(matchForm.getMatchDate()) &&
                    existingMatch.getUsageTime().equals(matchForm.getUsageTime())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMissingValues(MatchForm matchForm) {
        return Objects.isNull(matchForm.getStadium()) || Objects.isNull(matchForm.getMatchDate()) || Objects.isNull(matchForm.getUsageTime());
    }

    private int findAvailableSubStadium(MatchForm matchForm) {
        List<Match> matches = matchRepository.findByStadiumAndMatchDateAndUsageTime(
                matchForm.getStadium(),
                matchForm.getMatchDate(),
                matchForm.getUsageTime()
        );

        Field selectedField = fieldRepository.findByFieldName(matchForm.getStadium());
        int maxSubStadiumCount = selectedField.getCourtCount();

        int[] subStadiumMembers = new int[maxSubStadiumCount + 1];

        for (Match existingMatch : matches) {
            int currentSubStadium = existingMatch.getSubStadiumCount();
            int currentParticipantCount = existingMatch.getParticipantCount();
            subStadiumMembers[currentSubStadium] = currentParticipantCount;
        }

        for (int i = 1; i <= maxSubStadiumCount; i++) {
            if (subStadiumMembers[i] < 2) {
                return i;
            }
        }
        return -1;
    }

    private Match createAndSaveMatch(MatchForm matchForm, int availableSubStadium) {
        Match newMatch = matchForm.toEntity();
        newMatch.setParticipantCount(1);
        newMatch.setSubStadiumCount(availableSubStadium);
        matchRepository.save(newMatch);
        return newMatch;
    }

    private void addMatchMember(Member loggedInMember, Match newMatch) {
        MatchMember matchMember = new MatchMember();
        matchMember.setMember(loggedInMember);
        matchMember.setMatch(newMatch);
        matchMember.setVoteCount(0);
        matchMemberRepository.save(matchMember);
    }
}
