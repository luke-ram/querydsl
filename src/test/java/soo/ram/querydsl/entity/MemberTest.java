package soo.ram.querydsl.entity;

import com.querydsl.core.QueryFactory;
import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static soo.ram.querydsl.entity.QMember.*;

@SpringBootTest
@Transactional
class MemberTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;


    @BeforeEach
    public void testEntity() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        em.flush();
        em.clear();
    }

    @Test
    public void startJPQL() {

        String query = "select m from Member m where m.userName = :username";
        Member singleResult = em.createQuery(query, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(singleResult.getUserName()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {
        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);
        QMember m = new QMember("m");

        Member findMember = jpaQueryFactory
                .select(m)
                .from(m)
                .where(m.userName.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUserName()).isEqualTo("member1");
    }

    @Test
    public void startQuerydslCreateInstance() {
        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(em);

        Member findMember = jpaQueryFactory
                .select(member)
                .from(member)
                .where(member.userName.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUserName()).isEqualTo("member1");
    }

    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.userName.eq("member1").and(member.age.eq(10)))
                .fetchOne();

        Member findMember2 = queryFactory
                .selectFrom(member)
                .where(member.userName.eq("member1"),
                        member.age.eq(10))
                .fetchOne();

        assertThat(findMember.getUserName()).isEqualTo("member1");


    }

    @Test
    public void resultFetch() {
        queryFactory
                .selectFrom(member)
                .fetch();

        queryFactory
                .selectFrom(member)
                .where(member.userName.eq("member1"))
                .fetchOne();
        queryFactory
                .selectFrom(member)
                .fetchFirst();
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        results.getTotal(); // 쿼리 두번 나감 토탈숫자 쿼리 때문에
        List<Member> contents = results.getResults();


        long total = queryFactory
                .selectFrom(member)
                .fetchCount(); // 토탈 쿼리만 나감

    }

    /**
     * 회원 정렬 순서
     * 1. 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));


        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.userName.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        System.out.println("member5 = " + member5);
        Member member6 = result.get(1);
        System.out.println("member6 = " + member6);
        Member memberNull = result.get(2);
        System.out.println("memberNull = " + memberNull);


    }

}