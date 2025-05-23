package com.EcoBoost.PPI.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.EcoBoost.PPI.entity.Cart;
import com.EcoBoost.PPI.entity.Product;
import com.EcoBoost.PPI.entity.Sales;
import com.EcoBoost.PPI.entity.User;
import com.EcoBoost.PPI.repository.CartRepository;
import com.EcoBoost.PPI.repository.ProductRepository;
import com.EcoBoost.PPI.repository.SalesRepository;
import com.EcoBoost.PPI.repository.UserRepository;
import com.EcoBoost.PPI.service.Email.EmailServiceImpl;

import jakarta.mail.MessagingException;

@Service
public class SalesService {
    @Autowired
    private SalesRepository salesRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private EmailServiceImpl emailService;
    @Autowired
    private NotificationService notiservice;

        @Transactional
        public Sales realizarVenta(Long userID) {


            User user = userRepository.findById(userID)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));


            List<Cart> carritos = cartRepository.findAllByCompradorId(userID);


            if (carritos.isEmpty()) {
                throw new IllegalArgumentException("El carrito está vacío, no se puede realizar la compra");
            }
                try {
                    
                   emailService.enviarCorreoComprador(user);
                } catch (MessagingException e) {
                   
                    e.printStackTrace();
                    System.out.println("Error al enviar el email: "+e.getMessage());
                }

            Sales sales = new Sales();
            sales.setUsuario(user);
            sales.setFecha(LocalDate.now());

            int totalEcoPoints = 0;
            double total = 0;
            double totalUnitario=0;
            for (Cart cart : carritos) {
                total += cart.getCantidadProducto() * cart.getProducto().getValor();
                cart.setSales(sales);


                Product producto = cart.getProducto();
                int cantidadProducto = cart.getCantidadProducto();


                if (producto.getCantidadStock() < cantidadProducto) {
                    throw new IllegalArgumentException("No hay suficiente stock para el producto: " + producto.getNombre_producto());
                }
               //Generar Notificacion Vendedor
                User vendedor=producto.getUsuario();
                totalUnitario=cantidadProducto*producto.getValor();
                notiservice.createNotification(producto, vendedor, cantidadProducto, totalUnitario);
                
                //Actualizar datos producto
                totalEcoPoints += cantidadProducto;
                producto.setCantidadStock(producto.getCantidadStock() - cantidadProducto);
                productRepository.save(producto);
                cart.setActivo(false);
            }
            sales.setTotal(total);
            sales.setCarrito(carritos);
            salesRepository.save(sales);
            user.setEcoPoints(user.getEcoPoints() + totalEcoPoints);
            userRepository.save(user);


            



            return sales;
        }

        public List<Sales>historialCompras(Long id){

            List<Sales>historial=salesRepository.findHistorialByUsuarioId(id);
            
            return historial;
        }

    }


