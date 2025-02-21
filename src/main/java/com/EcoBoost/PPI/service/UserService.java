package com.EcoBoost.PPI.service;

import com.EcoBoost.PPI.entity.Product;
import com.EcoBoost.PPI.entity.User;
import com.EcoBoost.PPI.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductService productService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> listAll() {

        return userRepository.findAll();
    }

    public void save(User user) {
        User userExists = userRepository.findByDocumento(user.getDocumento());
        if (userExists != null) {  // Usuario ya existe
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                user.setPassword(userExists.getPassword()); // Mantiene la contraseña anterior
            } else {
                user.setPassword(passwordEncoder.encode(user.getPassword())); // Encripta la nueva
            }
        } else {
            // Usuario nuevo, asegurarse de que la contraseña no sea null antes de encriptar
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                throw new IllegalArgumentException("La contraseña no puede estar vacía.");
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        userRepository.save(user);
    }

    public User get(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con el id: " + id));
    }

    public User authUser(String document, String password) {
        User user = userRepository.findByDocumento(document);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {

            return user;

        }else {
            return null;
        }
    }
    public Boolean recoveryPassword(String document , String email, String password){
        User user=userRepository.findByDocumento(document);
        if(user!=null&&user.getCorreo().equals(email)){
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
            return true;
        }
        return false;
    }

    @Transactional
    public void delete(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));


        for (Product product : user.getProductos()) {
            productService.delete(product.getId());
        }
        userRepository.delete(user);
    }
}