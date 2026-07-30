package com.neuedu.movieapi.mapper;

import com.neuedu.movieapi.entity.Person;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface PersonMapper {

    @Select("SELECT person_id as personId, name, sex, name_en as nameEn, name_zh as nameZh, birth, birthplace, profession, biography FROM person LIMIT #{pageSize} OFFSET #{offset}")
    List<Person> findAll(@Param("pageSize") Integer pageSize, @Param("offset") Integer offset);

    @Select("SELECT COUNT(*) FROM person")
    Long count();

    @Select("SELECT person_id as personId, name, sex, name_en as nameEn, name_zh as nameZh, birth, birthplace, profession, biography FROM person WHERE person_id = #{personId}")
    Person findById(@Param("personId") String personId);

    @Select("SELECT person_id as personId, name, sex, name_en as nameEn, name_zh as nameZh, birth, birthplace, profession, biography FROM person WHERE name LIKE CONCAT('%', #{keyword}, '%') OR name_zh LIKE CONCAT('%', #{keyword}, '%') LIMIT #{pageSize} OFFSET #{offset}")
    List<Person> searchByName(@Param("keyword") String keyword, @Param("pageSize") Integer pageSize, @Param("offset") Integer offset);

    @Select("SELECT COUNT(*) FROM person WHERE name LIKE CONCAT('%', #{keyword}, '%') OR name_zh LIKE CONCAT('%', #{keyword}, '%')")
    Long countByName(@Param("keyword") String keyword);

    @Insert("INSERT INTO person(person_id, name, sex, name_en, name_zh, birth, birthplace, profession, biography) " +
            "VALUES (#{personId}, #{name}, #{sex}, #{nameEn}, #{nameZh}, #{birth}, #{birthplace}, #{profession}, #{biography})")
    int insert(Person person);

    @Update("UPDATE person SET name=#{name}, sex=#{sex}, name_en=#{nameEn}, name_zh=#{nameZh}, " +
            "birth=#{birth}, birthplace=#{birthplace}, profession=#{profession}, biography=#{biography} WHERE person_id=#{personId}")
    int update(Person person);

    @Delete("DELETE FROM person WHERE person_id=#{personId}")
    int deleteById(@Param("personId") String personId);
}
