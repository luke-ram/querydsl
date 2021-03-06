package soo.ram.querydsl.entity;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data
public class MemberDto {

    private String username;
    private int age;

    //querydsl 기본생성자 만들기
    public MemberDto() {
    }

    @QueryProjection
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
