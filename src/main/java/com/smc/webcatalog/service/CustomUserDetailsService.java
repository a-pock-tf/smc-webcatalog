package com.smc.webcatalog.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.smc.webcatalog.dao.UserRepository;
import com.smc.webcatalog.model.User;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	@Autowired
	private UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		 Optional<User> user = userRepository.findByLoginId(username);
		    if(user.isPresent()) {
		    	List<GrantedAuthority> list = new ArrayList<GrantedAuthority>();
		    	User u = user.get();
		    	UserDetails detail = new org.springframework.security.core.userdetails.User(u.getLoginId(), u.getPassword(), list);

		        return detail;
		    } else {
		        throw new UsernameNotFoundException("username not found");
		    }
	}

}
