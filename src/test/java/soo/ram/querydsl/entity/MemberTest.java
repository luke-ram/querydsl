package soo.ram.querydsl.entity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryFactory;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.h2.engine.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static soo.ram.querydsl.entity.QMember.*;
import static soo.ram.querydsl.entity.QTeam.team;

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

        results.getTotal(); // ?????? ?????? ?????? ???????????? ?????? ?????????
        List<Member> contents = results.getResults();


        long total = queryFactory
                .selectFrom(member)
                .fetchCount(); // ?????? ????????? ??????

    }

    /**
     * ?????? ?????? ??????
     * 1. ?????? ????????????(desc)
     * 2. ?????? ?????? ????????????(asc)
     * ??? 2?????? ?????? ????????? ????????? ???????????? ??????(nulls last)
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

    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.userName.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2() {
        QueryResults<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.userName.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getLimit()).isEqualTo(2);
        assertThat(result.getOffset()).isEqualTo(1);
        assertThat(result.getResults().size()).isEqualTo(2);
    }

    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);

    }


    /**
     * ?????? ????????? ??? ?????? ?????? ????????? ?????????
     *
     * @throws Exception
     */
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);


    }

    /**
     * teamA??? ????????? ?????? ????????? ?????????
     *
     * @throws Exception
     */
    @Test
    public void join() throws Exception {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("userName")
                .containsExactly("member1", "member2");

    }

    @Test
    public void leftJoin() throws Exception {
        List<Member> result = queryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("userName")
                .containsExactly("member1", "member2");

    }

    /**
     * ?????? ?????? ????????? ?????? ?????? ?????? ??????
     * ????????? ????????? ??? ????????? ?????? ?????? ??????
     */
    @Test
    public void thetaJoin() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.userName.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("userName")
                .containsExactly("teamA", "teamB");

    }

    /**
     * ????????? ?????? ???????????????, ??? ????????? teamA??? ?????? ??????, ????????? ?????? ??????
     * select m , t from Member m left join m.team t on t.name = 'teamA'
     */

    @Test
    public void join_on_filltering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }

    }

    /**
     * ?????? ?????? ????????? ?????? ?????? ?????? ??????
     * ????????? ????????? ??? ????????? ?????? ?????? ??????
     */
    @Test
    public void join_on_no_relaction() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.userName.eq(team.name))
                // ?????? ????????? member.team,team ??????????????? ???????????????, ????????? ?????? id??? ????????? ????????????, ????????? ???????????? ?????? ??????(team.name) ????????????????????? team??? ??????
                .where(member.userName.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println(tuple);
        }


    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() {
        em.flush();
        em.clear();

        Member member1 = queryFactory
                .selectFrom(member)
                .where(member.userName.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member1.getTeam()); // ?????? ????????? ????????? ????????????(???????????????) ???????????? ?????????
        assertThat(loaded).as("???????????? ?????????").isFalse();

    }

    @Test
    public void fetchJoinUse() {
        em.flush();
        em.clear();

        Member member1 = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.userName.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member1.getTeam()); // ?????? ????????? ????????? ????????????(???????????????) ???????????? ?????????
        assertThat(loaded).as("???????????? ??????").isTrue();

    }

    @Test
    public void subQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }


    @Test
    public void subQueryGoe() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }


    @Test
    public void subQueryIn() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                        )
                )
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    public void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.userName,
                        select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    @Test
    public void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("??????")
                        .when(20).then("?????????")
                        .otherwise("??????"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println(s);
        }

    }

    /**
     * ?????? ????????? ?????? CaseBuilder?????? ?????? ???????????? ??????(DB??? ?????????)
     */
    @Test
    public void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20???")
                        .when(member.age.between(21, 30)).then("21~30???")
                        .otherwise("??????"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }

    }

    @Test
    public void concat() {
        //{username}_{age}
        List<String> result = queryFactory
                .select(member.userName.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.userName.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }


    }

    @Test
    public void simpleProjection() {
        List<String> result = queryFactory
                .select(member.userName)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }

    }

    @Test
    public void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.userName, member.age)
                .from(member)
                .fetch();

        //tuple??? service?????? ????????? ??????
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
            String username = tuple.get(member.userName);
            System.out.println("username = " + username);
            Integer age = tuple.get(member.age);
            System.out.println("age = " + age);
        }

    }

    @Test
    public void findDtoByJPQL() {
        List<MemberDto> resultList = em.createQuery("select new soo.ram.querydsl.entity.MemberDto(m.userName, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    /**
     * setter??? ????????? ?????? ?????????
     */
    @Test
    public void findDtoBySetter() {
        List<MemberDto> list = queryFactory
                .select(Projections.bean(MemberDto.class, member.userName, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : list) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    /**
     * ????????? ??????????????? ?????????
     */
    @Test
    public void findDtoByFields() {
        List<MemberDto> list = queryFactory
                .select(Projections.fields(MemberDto.class, member.userName, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : list) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    /**
     * DTO ???????????? ?????????
     * ????????? ????????? userName????????? name?????? ???????????? ??????
     * ????????? ????????? ?????? select?????? ????????? age????????? ???????????? ???????????? ??????
     */
    @Test
    public void findUserDtoByFields() {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> list = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.userName.as("name"),
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : list) {
            System.out.println("userDto = " + userDto);
        }

    }

    @Test
    public void findDtoByConstructor() {
        List<MemberDto> list = queryFactory
                .select(Projections.constructor(MemberDto.class, member.userName, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : list) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    @Test
    public void findUserDtoByConstructor() {
        List<UserDto> list = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.userName,
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : list) {
            System.out.println("userDto = " + userDto);
        }

    }


    /**
     * dto??? querydsl??? ???????????? ????????? ????????? ??????.
     */
    @Test
    public void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.userName, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }


    }

    /**
     * ?????? ?????? booleanBuilder ??????
     */
    @Test
    public void dynamicQuery_booleanBuilder() {
        //String usernameParam = "member1";
        String usernameParam = null;

        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameParam, Integer ageParam) {

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameParam != null) {
            builder.and(member.userName.eq(usernameParam));
        }

        if (ageParam != null) {
            builder.and(member.age.eq(ageParam));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameParam, Integer ageParam) {
        return queryFactory
                .selectFrom(member)
                //.where(usernameEq(usernameParam), ageEq(ageParam))
                .where(allEq(usernameParam, ageParam))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameParam) {
        return usernameParam == null ? null : member.userName.eq(usernameParam);
    }

    private BooleanExpression ageEq(Integer ageParam) {
        return ageParam == null ? null : member.age.eq(ageParam);
    }

    /**
     * usernameEq??? ageEq??? ???????????? ????????? ??? ??????.
     *
     * @param username
     * @param age
     * @return
     */
    private BooleanExpression allEq(String username, Integer age) {
        return usernameEq(username).and(ageEq(age));
    }

    @Test
    @Commit
    public void bulkUpdate(){
        //member1 = 10 -> ?????????
        //member2 = 20 -> ?????????
        //member3 = 30 -> ??????
        //member4 = 40 -> ??????

        queryFactory
                .update(member)
                .set(member.userName, "?????????")
                .where(member.age.lt(28))
                .execute();

        //????????????????????? ???????????? ????????????
    }

    @Test
    public void bulkAdd(){
        queryFactory
                .update(member)
                //.set(member.age, member.age.add(8))
                .set(member.age, member.age.multiply(2))
                .execute();
    }

    @Test
    public void bulkDelete(){
        queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    @Test
    public void sqlFunction(){
        List<String> result = queryFactory
                .select(Expressions.stringTemplate("function('replace' , {0},{1},{2})",
                        member.userName, "member", "M"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }


    }

    @Test
    public void sqlFunction2(){
        List<String> result = queryFactory.select(member.userName)
                .from(member)
//                .where(member.userName.eq(
//                        Expressions.stringTemplate("function('lower', {0})", member.userName)))
                .where(member.userName.eq(member.userName.lower()))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }

    }


}