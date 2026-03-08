package com.example.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


//Model -- Entity which has attributes / getter / setter / constructors /  toString
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity 
@Table(name="Users")
public class User {

	@Id @GeneratedValue
	private Long id;
	private String username;
	private String password;
	private String role;
	private String email;
	
	
}
