package com.neuedu.movieapi.service;
import com.neuedu.movieapi.common.Result;
import com.neuedu.movieapi.entity.PageResult;
import com.neuedu.movieapi.entity.Person;
import com.neuedu.movieapi.mapper.PersonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PersonService {
    @Autowired
    private PersonMapper personMapper;

    public PageResult<Person> findAll(Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Person> data = personMapper.findAll(pageSize, offset);
        Long totalCount = personMapper.count();
        return new PageResult<>(data, pageNum, pageSize, totalCount);
    }

    public Person findById(String personId) {
        return personMapper.findById(personId);
    }

    public PageResult<Person> search(String keyword, Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Person> data = personMapper.searchByName(keyword, pageSize, offset);
        Long totalCount = personMapper.countByName(keyword);
        return new PageResult<>(data, pageNum, pageSize, totalCount);
    }

    public Result<String> save(Person person) {
        // 验证必填字段
        if (person.getPersonId() == null || person.getPersonId().isEmpty()) {
            return Result.error("人员ID不能为空");
        }
        if (person.getName() == null || person.getName().isEmpty()) {
            return Result.error("人员姓名不能为空");
        }
        
        // 检查是否已存在
        Person existing = personMapper.findById(person.getPersonId());
        if (existing != null) {
            return Result.error("人员ID已存在");
        }
        
        int result = personMapper.insert(person);
        return result > 0 ? Result.success("添加成功") : Result.error("添加失败");
    }

    public Result<String> update(Person person) {
        // 验证必填字段
        if (person.getPersonId() == null || person.getPersonId().isEmpty()) {
            return Result.error("人员ID不能为空");
        }
        if (person.getName() == null || person.getName().isEmpty()) {
            return Result.error("人员姓名不能为空");
        }
        
        // 检查是否存在
        Person existing = personMapper.findById(person.getPersonId());
        if (existing == null) {
            return Result.error("人员不存在");
        }
        
        int result = personMapper.update(person);
        return result > 0 ? Result.success("更新成功") : Result.error("更新失败");
    }

    public Result<String> delete(String personId) {
        // 检查是否存在
        Person person = personMapper.findById(personId);
        if (person == null) {
            return Result.error("人员不存在");
        }
        
        int result = personMapper.deleteById(personId);
        return result > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }
}
