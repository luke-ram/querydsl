package soo.ram.querydsl.repository;

import org.springframework.stereotype.Repository;
import soo.ram.querydsl.dto.MemberSearchCondition;
import soo.ram.querydsl.dto.MemberTeamDto;

import java.util.List;

@Repository
public interface MemberRepositoryCustom {
    List<MemberTeamDto> search(MemberSearchCondition condition);
}
