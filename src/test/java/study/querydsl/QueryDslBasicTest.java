package study.querydsl;


import com.querydsl.core.QueryFactory;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QueryDslBasicTest {


    @Autowired
    private EntityManager em;
    private JPAQueryFactory queryFactory;


    @BeforeEach
    void before(){
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
    }

    @Test
    void startJPQL(){

        String jpql = "select m from Member m where m.username = :username";

        Member findMember = em.createQuery(jpql, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void startQueryDsl(){


        Member findMember = queryFactory.selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void search(){

        Member findMember = queryFactory.selectFrom(member)
                .where(member.username.eq("member1"),
                       member.age.eq(10)
                 )
                .fetchOne();

        assertThat(findMember.getAge()).isEqualTo(10);
    }

    @Test
    void resultFetch(){
//
//        List<Member> memberList = queryFactory.selectFrom(member)
//                                    .fetch();
//        Member findMember = queryFactory.selectFrom(member).fetchOne();
        Member findMember2 = queryFactory.selectFrom(member).fetchFirst();
    }

    @Test
    void sort(){
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));
        em.persist(new Member(null, 100));

        List<Member> members = queryFactory.selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(),
                         member.username.asc().nullsLast())
                .fetch();

        assertThat(members.get(0).getUsername()).isEqualTo("member5");
        assertThat(members.get(1).getUsername()).isEqualTo("member6");
        assertThat(members.get(2).getUsername()).isNull();
    }

    @Test
    void paging(){
        List<Member> members = queryFactory.selectFrom(member)
                .orderBy(member.age.desc())
                .offset(1)
                .limit(2)
                .fetch();

    }

    @Test
    void aggregation(){
        List<Tuple> result = queryFactory.select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                ).from(member)
                .fetch();
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
    }

    @Test
    void group(){
        List<Tuple> result = queryFactory.select(team.name,
                        member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple tuple1 = result.get(0);
        Tuple tuple2 = result.get(1);

        assertThat(tuple1.get(team.name)).isEqualTo("teamA");
        assertThat(tuple2.get(team.name)).isEqualTo("teamB");
        assertThat(tuple1.get(member.age.avg())).isEqualTo(15);
        assertThat(tuple2.get(member.age.avg())).isEqualTo(35);
    }

    @Test
    void join(){
        List<Member> result = queryFactory.select(member)
                .from(member)
                .where(team.name.eq("teamA"))
                .join(member.team, team)
                .fetch();

        assertThat(result).extracting("username").containsExactly("member1","member2");
    }

    @Test
    void theta_join(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory.select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result).extracting("username").containsExactly("teamA","teamB");
    }



}
