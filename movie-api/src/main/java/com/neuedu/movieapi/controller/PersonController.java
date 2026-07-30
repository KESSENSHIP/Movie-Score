package com.neuedu.movieapi.controller;
import com.neuedu.movieapi.common.Result;
import com.neuedu.movieapi.entity.PageResult;
import com.neuedu.movieapi.entity.Person;
import com.neuedu.movieapi.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/persons")
@CrossOrigin(origins = "*")
public class PersonController {
    @Autowired
    private PersonService personService;

    @GetMapping
    public PageResult<Person> list(@RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize) {
        return personService.findAll(pageNum, pageSize);
    }

    @GetMapping("/{id}")
    public Person getById(@PathVariable String id) {
        return personService.findById(id);
    }

    @GetMapping("/search")
    public PageResult<Person> search(@RequestParam String keyword, @RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize) {
        return personService.search(keyword, pageNum, pageSize);
    }

    @PostMapping
    public Result<String> add(@RequestBody Person person) {
        return personService.save(person);
    }

    @PutMapping
    public Result<String> update(@RequestBody Person person) {
        return personService.update(person);
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable String id) {
        return personService.delete(id);
    }
}
