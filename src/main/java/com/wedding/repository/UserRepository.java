package com.wedding.repository;

import com.wedding.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 冷库:用户表的存取。
// 继承 JpaRepository 即自带 findById / findAll / save / delete,无需手写 SQL。
public interface UserRepository extends JpaRepository<User, Long> {

    // 按用户名查(登录时定位账号)
    Optional<User> findByUsername(String username);

    // 用户名是否已存在(注册查重)
    boolean existsByUsername(String username);

}
