package com.example.estote.service;

import com.example.estote.entity.*;
import com.example.estote.entityDetail.CartDetail;
import com.example.estote.repositories.CartRepository;
import com.example.estote.repositories.GoodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final GoodRepository goodRepository;
    private final UserService userService;

    @Autowired
    public CartService(CartRepository cartRepository, GoodRepository goodRepository, UserService userService) {
        this.cartRepository = cartRepository;
        this.goodRepository = goodRepository;
        this.userService = userService;
    }

    /**
     * createCart() creating cart the first time a good is added to the cart
     */
    @Transactional
    public Cart createCart(User user, List<Long> goodIds) {
        Cart cart = new Cart();
        user.setCart(cart);
        List<Good> goodList = getCollectRefGoodById(goodIds);
        cart.setGoods(goodList);
        return cartRepository.save(cart);
    }

    /**
     * getCollectRefGoodById() get references on goods in cart
     */
    private List<Good> getCollectRefGoodById(List<Long> goodIds) {
        return goodIds
                .stream()
                .map(goodRepository::getOne)
                .collect(Collectors.toList());}

    @Transactional
    public void addGood(Cart cart, List<Long> goodIds) {
        List<Good> goods = cart.getGoods();
        List<Good> newGoodList = goods == null ? new ArrayList<>() : new ArrayList<>(goods);
        newGoodList.addAll(getCollectRefGoodById(goodIds));
        cart.setGoods(newGoodList);
        cartRepository.save(cart);
    }

    @Transactional
    public void deleteGood(Cart cart, Good good) {
        List<Good> goods = cart.getGoods();
        goods.remove(good);
        List<Good> newGoods = goods;
        cart.setGoods(newGoods);
        cartRepository.save(cart);
    }
    /**
     * getCartByUser() aggregate info for cart view
     */
    public Cart getCartByUser(String name) {
        User user = userService.findByName(name);
        if (user == null || user.getCart() == null) {
            return new Cart();}

        Cart cart = new Cart();
        Map<Long, CartDetail> mapByGoodId = new HashMap<>();
        List<Good> goods = user.getCart().getGoods();
        for (Good good : goods) {
            CartDetail detail = mapByGoodId.get(good.getId());
            if (detail == null) {
                mapByGoodId.put(good.getId(), new CartDetail(good));
            } else {
                detail.setAmount(detail.getAmount().add(new BigDecimal(1.0)));
                Integer price = good.getCostBeforeSale() * good.getSale() / 100;
                detail.setSum(detail.getSum() + Double.valueOf(price.toString()));
            }
        }
        cart.setCartDetails(new ArrayList<>(mapByGoodId.values()));
        cart.aggregate();
        return cart;
    }

    /**
     * clearCart() helps to clear cart after create order
     */
    @Transactional
    public void clearCart (Cart cart) {
        cart.getGoods().clear();
        cartRepository.save(cart);
    }
}


