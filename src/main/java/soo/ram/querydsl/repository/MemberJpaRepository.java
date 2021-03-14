package soo.ram.querydsl.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import soo.ram.querydsl.entity.Member;
import soo.ram.querydsl.entity.QMember;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static soo.ram.querydsl.entity.QMember.*;

@Repository
@RequiredArgsConstructor
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public void save(Member member){
        em.persist(member);
    }

    public Optional<Member> findById(Long id){
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);

    }

    public List<Member> findAll(){
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findAll_Querydsl(){
        return queryFactory
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findByUsername_Querydsl(String username){
        return queryFactory
                .selectFrom(member)
                .where(member.userName.eq(username))
                .fetch();
    }


    public List<Member> findByUserName(String userName){
        return em.createQuery("select m from Member m where m.userName = :username", Member.class)
                .setParameter("username", userName)
                .getResultList();
    }

}
