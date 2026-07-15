package com.fishing.brazil.config.login;

import com.fishing.brazil.entity.login.Role;
import com.fishing.brazil.enums.login.RoleName;
import com.fishing.brazil.repository.login.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RoleDataLoader implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public RoleDataLoader(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (roleRepository.findByName(RoleName.ROLE_ADMIN).isEmpty()) {
            roleRepository.save(new Role(null, RoleName.ROLE_ADMIN));
        }
        if (roleRepository.findByName(RoleName.ROLE_PESCADOR).isEmpty()) {
            roleRepository.save(new Role(null, RoleName.ROLE_PESCADOR));
        }
    }
}