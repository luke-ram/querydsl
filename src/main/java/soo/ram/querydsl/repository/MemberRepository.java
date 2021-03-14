package soo.ram.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import soo.ram.querydsl.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member,Long> {
    List<Member> findByUserName(String username);
}
